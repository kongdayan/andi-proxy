# Architecture

Andi Proxy is a small Android app with three runtime layers:

1. **UI and preferences** — activities, widgets, Quick Settings tile and the encrypted profile store.
2. **Android services** — a foreground proxy service, a `VpnService` and a process supervisor for the pinned sing-box core.
3. **Routing core** — sing-box 1.14.2, fed by a generated JSON config, plus hev-socks5-tunnel for TUN traffic.

There is no root component, no `iptables` rule, no background polling service and no remote-control endpoint.

## Component map

| Component | Responsibility |
| --- | --- |
| `MainActivity` | Home, Servers, Rules, Settings; runtime state polling; permission and VPN consent flows |
| `AppPickerActivity` | Searchable per-app direct list, including system apps |
| `AboutActivity` | Author, source, releases, licences, privacy notes bundled in the APK |
| `L` / `AndiApp` | Runtime localisation, language preference and Android per-app locale integration |
| `ProxyService` | Foreground service; hotspot/LAN lifecycle, core start/stop/apply, notifications, exit check |
| `LocalVpnService` | `VpnService`, TUN file descriptor, per-app exclusions, hev-socks5-tunnel lifecycle |
| `CoreRunner` | Writes the runtime config, runs `sing-box check`, starts/stops the core process, redacts logs |
| `Shell` | Small process runner with a timeout |
| `Profile` | In-memory server, routing and settings model; validation |
| `ProfileStore` | AES-GCM encryption with an Android Keystore key; backup disabled by manifest |
| `ConfigBuilder` | Converts `Profile` + optional subscription snapshot into a sing-box config |
| `RuleSubscription` | Parses Shadowrocket `[Rule]` entries; validates URLs and unsupported syntax |
| `SubscriptionStore` | HTTPS download, atomic snapshot cache, size limits, geo-IP database fetch |
| `ProxyProbe` | Local SOCKS5 + TLS request to an exit-IP service |
| `QuickControls`, `ProxyWidget`, `ProxyTile` | Widget and Quick Settings surfaces |
| `DeviceDiagnostics` | Interface/address diagnostics and a redacted support report |

## Startup flow

### Phone-only proxy

```text
MainActivity
  └─ VpnService.prepare() (system consent)
      └─ ProxyService START (deviceOnly=true)
          ├─ ProfileStore.load() + validate()
          ├─ SubscriptionStore.forStart() (optional)
          ├─ ConfigBuilder.build()
          ├─ sing-box check
          ├─ sing-box run (mixed inbound on 127.0.0.1:2080)
          └─ LocalVpnService
              ├─ VpnService.Builder with per-app exclusions
              ├─ hev-socks5-tunnel TUN → 127.0.0.1:2081
              └─ sing-box VPN inbound on 127.0.0.1:2081
```

### Hotspot sharing

```text
MainActivity
  └─ ProxyService START (createHotspot=true)
      ├─ WifiManager.startLocalOnlyHotspot()
      ├─ wait for the new wlan/ap interface and its private IPv4 address
      ├─ sing-box mixed inbound on that address:2080
      └─ notification with address, SSID and password
```

The phone VPN can use the same core at the same time. The core has two inbounds (mixed on `2080` and VPN on `2081`) and one route table.

### Existing Wi-Fi / LAN

The user selects an interface from the address spinner. The app resolves its private IPv4 address and starts the mixed inbound there. No hotspot is created.

## Process and state model

`ProxyService` owns a generation counter. Every asynchronous start, apply or stop increments it; callbacks from an old generation are ignored. This prevents a late hotspot callback from reviving a stopped session.

- `active` — the core is running and its listening port accepted a connection.
- `busy` — a start, stop or apply transition is in progress.
- `failure` — the last status is an error; the UI shows `detail`.
- `address` — the mixed inbound address; `127.0.0.1` means phone-only.
- `isSharing()` — active with a non-loopback address.

Status strings are localised at assignment time. Error/success decisions use the `failure` flag, not string matching.

`LocalVpnService` tracks `nativeStarted` and `established` separately. `established` distinguishes "the tunnel was up" from "startup failed", so a failure message is not overwritten by a generic "stopped" message.

## Config generation

`ConfigBuilder.build(profile, listenAddress, snapshot)` emits:

- one outbound (`hysteria2` or `socks`) with the upstream credentials;
- a mixed inbound on the chosen address and port `2080`;
- a second SOCKS inbound on `127.0.0.1:2081` when the phone VPN is enabled;
- DNS servers and rules;
- route rules: manual domains/CIDRs, private networks, subscription rules, final route;
- `rule_set` entries for downloaded geo-IP files.

The config is written to `files/active-config.json` with mode `0600`. It is deleted on stop. The core is started as a child process; its stdout/stderr are read on a dedicated thread, truncated to a small tail and redacted for known secrets before reaching the UI.

`sing-box check` validates the config before `run`. If validation fails, the temporary config is deleted and the error is surfaced with secrets removed.

## Rules and DNS

`RuleSubscription` accepts:

- `DOMAIN`, `DOMAIN-SUFFIX`, `DOMAIN-KEYWORD`
- `IP-CIDR`, `IP-CIDR6` (with optional `no-resolve`)
- `GEOIP` (two-letter country code or `LAN`)
- `FINAL` / `MATCH`
- policies `DIRECT`, `PROXY`, `REJECT`, `REJECT-DROP`

Anything else — policy groups, `RULE-SET`, unsupported options, a rule after `FINAL` — is an error. The update is rejected and the previous cache is kept.

Routing order is described in the README. DNS is deliberately asymmetric:

- Proxied domains use remote Cloudflare DoH through the proxy.
- Direct domains use `223.5.5.5`.
- Server hostnames are resolved through the direct DNS path first, so the proxy does not need itself to connect.
- IP rules resolve explicitly only when required; `no-resolve` suppresses that lookup.

Country rules use the destination IP after resolution. They do not promise that every DNS query for a Chinese domain goes direct.

## Storage and atomicity

- Profile: encrypted JSON in `SharedPreferences("profile")`; key alias `andi-profile-v1` in Android Keystore; AES-GCM with a random IV.
- Runtime config: private file, `0600`, deleted on stop.
- Subscription cache: `files/subscriptions/<sha256(url)>/snapshot-<uuid>/`.
  - `current.json` is written with `AtomicFile`.
  - The snapshot contains `rules.conf`, optional `geoip-*.srs` files and a SHA-256 of the rule body.
  - A new snapshot is validated with `sing-box check` before `current.json` is switched.
  - The two newest snapshots are retained so a running core keeps its referenced files.
- VPN tunnel config: `files/vpn-tunnel.yml`, deleted on stop.

No profile, subscription body or core config is included in the APK.

## Network and permissions

- `INTERNET`, network/Wi-Fi state, Wi-Fi change.
- `NEARBY_WIFI_DEVICES` on Android 13+; location permissions only up to Android 12L.
- `POST_NOTIFICATIONS`, foreground service permissions.
- `QUERY_ALL_PACKAGES` is intentional: the VPN exclusion picker must list headless system apps.
- `BIND_VPN_SERVICE` on the VPN service; only the system can bind it.
- Cleartext traffic is disabled at the manifest level. Subscription URLs are HTTPS-only.

The app does not request root. It does not use `su` for normal operation.

## UI and layout

The UI is built programmatically to keep the APK small and to support large-title, card-based layouts without XML duplication. `MainActivity` renders one of four tabs into a `ScrollView` and reflows on window-size changes:

- narrow widths reduce padding and text sizes;
- short heights hide the brand and compress navigation;
- the settings, rules and picker pages remain scrollable so save actions stay reachable.

The widget uses `RemoteViews` with a small and an expanded layout. Android 12+ receives a size map; older versions use widget options. The Quick Settings tile opens the app for actions that may need VPN or permission consent.

## Localisation

`L` keeps the Chinese source string as the key and loads `assets/i18n/<language>.json` at runtime. English is the fallback for every non-Chinese locale. The language preference is stored in `SharedPreferences("andi_settings")`; Android 13+ also receives the choice through `LocaleManager`, so the app appears in the system per-app language picker.

This design was chosen because the UI is programmatic and status strings are produced in services and widgets, not only in activities. The keys are stable text, and a translator can edit one JSON file without touching code. `scripts/check-i18n.py` verifies key coverage, placeholders and untranslated text in CI.

## Testing

- JVM unit tests: config generation, rule parsing, validation, search ranking.
- Build checks: `lintDebug`, `assembleDebug`, per-ABI APK inspection.
- `scripts/core_smoke.py`: runs the pinned macOS host core against generated configs and local TCP/UDP fixtures.
- Optional device runners (`app/src/androidTest`) for real VPN, hotspot and subscription tests. They are not part of the main APK and do not contain credentials.
- CI: `.github/workflows/ci.yml`; release: `.github/workflows/release.yml`.

## Known constraints

- Android permits one active VPN. The phone VPN cannot coexist with another VPN service.
- Hotspot clients are not transparently proxied; they must set the HTTP/SOCKS5 proxy.
- `LocalOnlyHotspot` names/passwords are system-generated on Android 16.0.
- The 16.1 app-defined hotspot branch is compiled but not device-verified.
- Other ABIs and 16 KiB page-size devices are built but not device-verified.
- The core is a child process; OEM background restrictions can still stop the app if the user force-stops it or removes the foreground notification.
