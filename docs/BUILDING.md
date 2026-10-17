# Building and releasing

## Requirements

- JDK 17 (`JAVA_HOME` set; Gradle 8.11.1 is used through the wrapper)
- Android SDK Platform 36 and Build Tools 35.0.0
- Python 3.10+
- macOS arm64 for `scripts/core_smoke.py` (the host sing-box binary is macOS arm64)

The project does not need Android Studio. Set `ANDROID_HOME` to your SDK, or create a local `local.properties` with:

```properties
sdk.dir=/absolute/path/to/android-sdk
```

`local.properties` is ignored by Git.

## Fetch the pinned cores

```sh
python3 scripts/fetch-core.py
```

By default this downloads and verifies:

| Upstream | Version | Android ABI | Purpose |
| --- | --- | --- | --- |
| sing-box | 1.14.2 | arm64-v8a, armeabi-v7a, x86_64, x86 | `libsingbox.so` per APK |
| sing-box | 1.14.2 | macOS arm64 | local `core_smoke.py` host |
| hev-socks5-tunnel | 2.18.0 | arm64-v8a, armeabi-v7a, x86_64, x86 | `libhev-socks5-tunnel.so` per APK |

Every archive is checked against the official SHA-256 in the script before extraction. Only the known executable/library members are extracted.

Useful options:

```sh
python3 scripts/fetch-core.py --abi arm64-v8a   # one ABI
python3 scripts/fetch-core.py --no-host         # skip the macOS core
python3 scripts/fetch-core.py --clean           # remove existing .so files first
```

The downloaded binaries are ignored by Git. A clean checkout therefore needs this step before any Android build.

## Build

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Because ABI splits are enabled, `assembleDebug` produces:

```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk
app/build/outputs/apk/debug/app-x86_64-debug.apk
app/build/outputs/apk/debug/app-x86-debug.apk
app/build/outputs/apk/debug/app-universal-debug.apk
```

Install a specific APK with `adb install -r <path>`.

## Versioning

`app/build.gradle` reads optional Gradle properties:

```sh
./gradlew :app:assembleRelease -PversionName=1.2.3 -PversionCode=10203
```

Defaults are `1.0.0` / `10000`. The release workflow derives the version from the tag (`v1.2.3` → `1.2.3`) and computes `versionCode` as `major * 10000 + minor * 100 + patch`.

## Release signing

Release builds are signed with a keystore when these environment variables (or matching Gradle properties) are present:

| Environment variable | Gradle property | Meaning |
| --- | --- | --- |
| `ANDI_KEYSTORE` | `andi.keystore` | Absolute path to the `.jks` file |
| `ANDI_KEYSTORE_PASSWORD` | `andi.keystorePassword` | Keystore password |
| `ANDI_KEY_ALIAS` | `andi.keyAlias` | Key alias |
| `ANDI_KEY_PASSWORD` | `andi.keyPassword` | Key password |

Example:

```sh
export ANDI_KEYSTORE="$PWD/.signing/andi-release.jks"
export ANDI_KEYSTORE_PASSWORD='…'
export ANDI_KEY_ALIAS='andi'
export ANDI_KEY_PASSWORD='…'
./gradlew :app:assembleRelease -PversionName=1.0.0 -PversionCode=10000
```

If no keystore is configured, the release build falls back to the debug signing key so contributors can still build installable APKs. Such APKs are not suitable for a long-lived release channel: Android requires the same signing key for updates. The CI workflow writes `SIGNING.txt` into the release assets and emits a warning when it falls back to debug signing.

Never commit a keystore or its passwords. For GitHub Actions, add these repository secrets:

- `ANDROID_KEYSTORE_BASE64` — `base64 -w0 release.jks` or `base64 release.jks` (line breaks are accepted by `base64 --decode` on Ubuntu)
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Back up the keystore and passwords offline. Losing them means existing users cannot upgrade without uninstalling.

## Verify a release APK

```sh
export JAVA_HOME=/path/to/jdk17
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-arm64-v8a-release.apk
$ANDROID_HOME/build-tools/35.0.0/aapt dump badging app/build/outputs/apk/release/app-arm64-v8a-release.apk | head
unzip -l app/build/outputs/apk/release/app-arm64-v8a-release.apk | grep -E 'lib/|assets/i18n'
```

The release workflow performs the same checks and writes `SHA256SUMS`.

## Translations

The UI uses the Chinese source string as a key. Add or change a key in Java with `L.t("中文")` or `L.f("模板 %s", value)`, then run:

```sh
python3 scripts/check-i18n.py
```

The checker extracts keys from `L.t(...)` / `L.f(...)`, compares them with `app/src/main/assets/i18n/{en,ja,ko,ru}.json`, verifies printf placeholders and rejects untranslated Han text in non-English tables. Add keys to every table before opening a PR.

## CI and releases

- `.github/workflows/ci.yml` runs on pushes to `main` and pull requests. It fetches the cores, validates translations, runs unit tests, lint and `assembleDebug`, and uploads the debug APKs.
- `.github/workflows/release.yml` runs on tags matching `v*.*.*` and on manual dispatch. It builds signed release APKs (one per ABI plus universal), verifies them, writes `SHA256SUMS` and `SIGNING.txt`, uploads them as artifacts, and creates a GitHub Release for tag pushes.

To publish:

```sh
git tag -a v1.0.0 -m "Andi Proxy 1.0.0"
git push origin v1.0.0
```

The workflow attaches:

```text
AndiProxy-arm64-v8a.apk
AndiProxy-armeabi-v7a.apk
AndiProxy-x86_64.apk
AndiProxy-x86.apk
AndiProxy-universal.apk
SHA256SUMS
SIGNING.txt
```

A manual `workflow_dispatch` builds the same artifacts without creating a release, which is useful for testing the pipeline.

## Smoke tests

```sh
python3 scripts/check-i18n.py
python3 scripts/core_smoke.py
```

`core_smoke.py` expects the macOS host core downloaded by `fetch-core.py` and the unit-test config fixtures in `app/build/test-configs/`. It starts real local TCP/UDP and SOCKS5 fixtures against the generated configs. It does not need an Android device.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `SDK location not found` | Set `ANDROID_HOME` or create `local.properties`. |
| `libsingbox.so` missing from the APK | Run `python3 scripts/fetch-core.py` before Gradle. |
| `sing-box check` fails in CI | Check the config/rule output; run the unit tests locally. Never weaken the check. |
| Release is debug-signed | Add the four repository secrets. Check the `SIGNING.txt` asset. |
| `apksigner` cannot find Java | Export `JAVA_HOME` to JDK 17. |
| `core_smoke.py` fails on Linux | The host core is macOS arm64; use an Android device or skip the host smoke test on Linux. |
