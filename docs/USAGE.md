# Using Andi Proxy

Andi Proxy has two independent jobs:

1. Proxy **this phone** through a local VPN.
2. Share a proxy with **other devices** over the phone's hotspot or an existing Wi-Fi/LAN.

They can run together, and stopping one does not stop the other.

## 1. Add a server

Open **Servers** and either:

- tap **Import profile** and paste a HY2/SOCKS5 JSON profile, or
- choose **SOCKS5** or **HY2 / Hysteria2**, then fill in the address, port and credentials.

For SOCKS5, the username and password are required. For HY2, enter the authentication password and, if needed, the TLS SNI and Salamander obfuscation password. Certificate verification is on by default; only disable it if you understand the risk.

Tap **Save server**.

## 2. Proxy this phone

On **Home**, tap **Enable phone proxy** and allow Android's VPN request.

- Phone apps follow **Rules**.
- **Apps kept off the VPN** opens a searchable list of installed apps, including system apps. Selected apps bypass the VPN and connect directly. Andi always bypasses its own VPN to avoid a loop.
- The VPN uses the same routing core as sharing. You do not need to start a hotspot.

To stop only the phone VPN, tap **Stop phone proxy**. Sharing, if active, stays up.

## 3. Share with other devices

1. On **Home**, tap **Start Wi-Fi sharing**. Android may ask for Nearby devices or Location permission.
2. Tap **Connect other devices**. The guide shows the hotspot name and password, the proxy address and port `2080`.
3. On the client (laptop, tablet or another phone), join the shown Wi-Fi. Open the Wi-Fi details and set the proxy to **Manual**:
   - Server: the address shown in the app
   - Port: `2080`
4. Open a web page. If the client warns that the Wi-Fi has no internet, choose **Keep connection**.

Clients that support SOCKS5 can use the same address and port instead of HTTP. The app accepts HTTP CONNECT and SOCKS5 on the same port.

Sharing is explicit: the client must set the proxy. A normal Android app cannot transparently intercept the hotspot's kernel forwarding path without root. Games, UDP-only apps and whole-device proxying need a client-side SOCKS5/VPN/TUN tool.

### Using an existing Wi-Fi network

If the phone and the client are already on the same Wi-Fi, expand **Use the system hotspot or an existing Wi-Fi network** on Home, choose the phone's LAN address and tap **Share this network**. This is useful when the phone is connected to a Wi-Fi network and you do not want to create a hotspot.

### Using the system hotspot

Android 16.0 generates the name and password of an app-created `LocalOnlyHotspot`. If you need a fixed name or password:

1. Open **Settings → System hotspot settings** and set the name, password and band in Android.
2. Return to Andi Home, expand **Use the system hotspot or an existing Wi-Fi network**, select the hotspot interface and tap **Share this network**.

The system hotspot and the app hotspot are different networks. Changing the system hotspot does not change the app-created one. Android 16.1 adds APIs for app-defined hotspot names and passwords; the app includes that branch, but it has not been verified on a 16.1 device.

## 4. Rules and subscriptions

Open **Rules**.

- **Rule subscription**: paste a Shadowrocket `.conf` URL or rule-list URL. HTTPS is required. Tap **Update now** to download immediately. A failed update keeps the previous cache.
- **Update expired rules on startup** refreshes a cache older than 24 hours when the proxy starts.
- **Manual routing**: choose the default route (proxy or direct), then add proxied domains, direct domains and direct CIDRs, one per line.
- **Save rules** applies changes to a running proxy.

Only `[Rule]` sections are used. MITM, certificate settings, URL rewriting and General system settings in a subscription are ignored. Unsupported routing rules fail the update instead of being silently dropped.

Priority: manual proxy domains → manual direct domains → manual direct CIDRs → private networks → subscription order → final route. A subscription `FINAL` overrides the manual default route.

## 5. Appearance, language and shortcuts

- **Appearance**: follow the system, light or dark theme. Tap **Apply theme**.
- **Language**: system, English, 简体中文, 日本語, 한국어 or Русский. On Android 13+, the same choice is available in Android's per-app language settings.
- **Shortcuts**: add a resizable home-screen widget or a Quick Settings tile. Long-press the widget and drag its handles to resize it.
- **Advanced settings**: optional local proxy authentication for shared clients, device diagnostics, and the open-source information page.

## 6. Troubleshooting

| Symptom | What to check |
| --- | --- |
| Client says “no internet” | Choose **Keep connection**; the hotspot has no direct internet by design. Confirm the client proxy is set to the phone address and port `2080`. |
| Client cannot reach the proxy | Make sure sharing is active, the client is on the same hotspot/LAN, and the phone's firewall or “private DNS” is not blocking the client. |
| Phone VPN will not start | Android allows one active VPN. Stop any other VPN app and allow Andi's VPN request. |
| Subscription update fails | The URL must be HTTPS and reachable without a proxy if the proxy is not running. The previous cache remains in use. |
| Core says “validation failed” | Check the server fields and rules. The message includes the core's reason. No credentials are shown in logs. |
| Hotspot name/password cannot be edited in Andi | Android 16.0 generates them. Use the system hotspot and share its interface, or wait for Android 16.1 support. |
| Widget does not change | Open Andi once after installing, then re-add the widget. Some launchers cache widget layouts. |

For a bug report, include the Android version, device model, ABI, app version, whether the phone VPN or sharing was active, and the exact error text. Never include server credentials, subscription URLs with tokens, or personal data.

## 7. Uninstall and cleanup

Stopping the proxy deletes the runtime core config and closes the VPN/TUN. Uninstalling the app removes the encrypted profile, subscription caches and logs. The Android Keystore key is removed with the app.
