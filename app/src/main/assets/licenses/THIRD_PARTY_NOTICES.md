# Third party components

The Android APKs package the unmodified official sing-box 1.14.2 Android executables as `libsingbox.so`. The build produces one APK per ABI and a universal APK.

- Upstream: https://github.com/SagerNet/sing-box
- Exact source: https://github.com/SagerNet/sing-box/tree/v1.14.2
- Commit: `af6e64c3b69e6132ebaee0e1a3d24e93903f6709`
- License: GPL-3.0-or-later; see `LICENSE.sing-box` and `LICENSE`.
- Additional upstream naming condition: derivative works must not use the upstream application's name or imply association without prior consent. This application is named Andi Proxy and is independent of SagerNet.

| Android ABI | Official archive | Archive SHA-256 |
| --- | --- | --- |
| `arm64-v8a` | `sing-box-1.14.2-android-arm64.tar.gz` | `57eeb81867a9a5e406bcbb5f7fde352acdc354e8c084173a28bdd9417c5e0cba` |
| `armeabi-v7a` | `sing-box-1.14.2-android-arm.tar.gz` | `10352dab32c87e2eefe3fd61e21bd259900ef4096ced311ec49050769613e861` |
| `x86_64` | `sing-box-1.14.2-android-amd64.tar.gz` | `5a16af1bcb373d5e8824b7cca415813d3556dd10e7d41e5dd4dfb3cb9e9240ff` |
| `x86` | `sing-box-1.14.2-android-386.tar.gz` | `855ad664d406435f48c65488baf07cef0bd2c11e833c2c5a80b63f48ce6340db` |

The executable is not loaded with `System.loadLibrary`; Android extracts it into the application's native library directory, from which the app starts it as a child process. Packaging it as a native library avoids executing downloaded code from a writable application directory on Android 10 and later.

This project is licensed under GPL-3.0-or-later. No server credentials are included in application resources, APKs, test fixtures, or published documentation. Temporary test profiles are kept in the ignored `.tools/runtime` directory and the encrypted Android application profile store.

Before distributing this application, provide the complete corresponding source for the included GPL components, their build instructions and dependencies in accordance with GPLv3. The links above identify the current pinned upstream source; they do not replace that distribution requirement.

The APK also includes the unmodified JNI libraries and Java binding from **hev-socks5-tunnel 2.18.0**, used to transport Android VpnService packets to the same local rule engine. The official AAR contains `arm64-v8a`, `armeabi-v7a`, `x86_64` and `x86`.

- Source: https://github.com/heiher/hev-socks5-tunnel/tree/2.18.0
- License: MIT, see `LICENSE.hev` and `assets/licenses/hev.txt`.
- Official release AAR SHA-256: `15ec8ed121663b562c99caa5bb602d1009f24e5b09e733438b81988f12feaaab`.
- Native source and dependency notices: https://github.com/heiher/hev-socks5-tunnel/blob/2.18.0/LICENSE

GeoIP rule data is downloaded only when an enabled subscription needs it, from https://github.com/SagerNet/sing-geoip/tree/rule-set. Database provenance and license are described by that upstream project (GPL-3.0). User-provided subscriptions retain their own licenses; they are fetched into private runtime cache and are not bundled into this APK. The suggested example source is https://github.com/johnshall/Shadowrocket-ADBlock-Rules-Forever.

The blue shield and Wi-Fi icon is an original vector asset; no Samsung icon, logo or proprietary font is included.
