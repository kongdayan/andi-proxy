# Privacy

Andi Proxy is designed to keep proxy credentials and routing data on the device. It has no account system, no analytics, no advertising and no telemetry.

## What the app stores

| Data | Where | Protection |
| --- | --- | --- |
| Server address, port, username, password, SNI, obfuscation password | App-private `SharedPreferences("profile")` | AES-GCM with a key in the Android Keystore (`andi-profile-v1`); Android backup and device transfer disabled |
| Rules, default route, subscription URL | Same encrypted profile | Same |
| Excluded app package names | Same encrypted profile | Same |
| Subscription bodies and geo-IP files | `files/subscriptions/<sha256(url)>/` | App-private storage; app-private files are not readable by other apps |
| Runtime sing-box config | `files/active-config.json` | Mode `0600`; deleted when the proxy stops |
| VPN tunnel config | `files/vpn-tunnel.yml` | App-private; deleted when the VPN stops |
| Language and theme | Local preferences | Not sensitive |

There is no login, cloud sync or remote configuration service.

## Network connections

The app makes network connections only for the functions you enable:

1. **Upstream proxy** — Hysteria2 or SOCKS5 to the server you configure.
2. **Subscription download** — HTTPS to the URL you paste. Redirects must remain HTTPS; embedded credentials and fragments are rejected. Downloads are size-limited (4 MiB for rules, 8 MiB per geo-IP file) and cached.
3. **Geo-IP data** — when a subscription contains `GEOIP`, the app downloads the matching `.srs` file from `raw.githubusercontent.com/SagerNet/sing-geoip`.
4. **DNS** — proxied domains are resolved through Cloudflare DoH via the proxy; direct domains use `223.5.5.5`; server hostnames use direct DNS bootstrap.
5. **Exit check** — optional, user-triggered. The app sends a TLS request to `api.ipify.org` through the local proxy to display the current exit IPv4 address.

No request is sent to the project author. The app does not collect crash reports or usage statistics. GitHub issues are handled on GitHub, not from inside the app.

## Android permissions

| Permission | Why |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | Proxy, subscription and hotspot functions |
| `NEARBY_WIFI_DEVICES` (Android 13+) | Create a local-only hotspot |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (max SDK 32) | Legacy hotspot creation on Android 12L and below |
| `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the proxy and VPN running with a visible notification |
| `QUERY_ALL_PACKAGES` | The VPN exclusion picker intentionally lists all installed apps, including headless system apps |

The app does not request root, contacts, storage, camera, microphone, SMS or location on Android 13+ (location is capped at Android 12L for legacy hotspot APIs).

## Logs and diagnostics

The core's stdout/stderr is kept in a small in-memory tail for error messages. Known secrets (server password, obfuscation password, local proxy password) are replaced with `[hidden]` before the text reaches the UI. The runtime config and VPN config are deleted on stop.

The optional **Device diagnostics** report includes the device manufacturer/model, Android version, ABI, network interface names/addresses and the core version. It is shown on screen and is not uploaded automatically.

## Third parties

- **sing-box 1.14.2** (GPL-3.0-or-later) is bundled as an executable named `libsingbox.so`.
- **hev-socks5-tunnel 2.18.0** (MIT) is bundled as a JNI library.
- **sing-geoip** rule sets are downloaded only when a subscription needs them.
- Subscriptions are supplied by the user and retain their own licences and privacy practices.

See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) for source links and SHA-256 digests.

## Children and sensitive data

The app is a networking tool, not a data-collection service. It is not directed at children. Do not use it to violate the laws or network policies that apply to you.

## Changes

This document describes the current release. Material changes will be noted in [CHANGELOG.md](../CHANGELOG.md). The source code is the authoritative description of behaviour.

## Contact

Open an issue at <https://github.com/kongdayan/andi-proxy/issues> for privacy questions. Do not include credentials, subscription tokens or personal data in an issue. Security reports should follow [SECURITY.md](../SECURITY.md).
