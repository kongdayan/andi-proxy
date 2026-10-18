# Changelog

All notable changes to Andi Proxy are documented here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows [Semantic Versioning](https://semver.org/).

## 1.0.1 - 2026-10-18

Privacy cleanup; no proxy behaviour changes.

### Changed

- Removed the in-app author introduction, personal links and footer. The About page now focuses on the project, source, licences and privacy.
- Removed personal author metadata from the repository history; commits now use a project identity.
- Version bumped to 1.0.1 (`versionCode` 10001).

## 1.0.0 - 2026-10-18

The first public, reproducible release.

### Added

- Five-language UI: English (default for non-Chinese locales), Simplified Chinese, Japanese, Korean and Russian.
- Language picker in Settings and Android 13+ per-app language integration.
- Bundled About/author page with project, source, issue, release, licence and privacy links, plus third-party notices.
- Per-ABI APKs for `arm64-v8a`, `armeabi-v7a`, `x86_64` and `x86`, plus a universal APK.
- GitHub Actions CI and a signed release pipeline with `SHA256SUMS`, signature verification and APK inspection.
- English README and documentation, with translated READMEs.
- Issue templates, pull-request template, contribution guide, security policy and code of conduct.

### Changed

- Version bumped to 1.0.0 (`versionCode` 10000).
- Public 1.0.0 builds use a new release signing key; uninstall pre-1.0.0 debug builds before upgrading.
- The core fetch script now downloads and verifies all supported Android ABIs and the macOS host core.
- App labels, notification text, widget text, tile text and status strings are localised at runtime.
- `ProxyService` now exposes an explicit failure flag instead of matching localised status text.
- `LocalVpnService` distinguishes an established tunnel from a failed startup when shutting down.

### Verified

- Unit tests, Android lint, debug and release APK builds, per-ABI native-library contents, locale configuration and `apksigner` output.
- The networking and sharing matrix from 0.3 remains the reference for the shared core/service code path; see [Verification](docs/VERIFICATION.md).

## 0.3.0 - 2026-10-01

### Added

- Independent phone VPN and hotspot sharing controls.
- Searchable per-app direct list, including system apps, with UID-aware exclusions.
- Shadowrocket rule subscription with manual rules, country rule sets and atomic cache retention.
- Resizable home-screen widget, Quick Settings tile and long-press shortcuts.
- Large-title card UI, light/dark/system themes, compact and small-window layouts.
- Five-step connection guide with real SSID, password and proxy address.

### Verified

- Real HY2 and authenticated SOCKS5 exits, HTTP CONNECT and SOCKS5 from a physical hotspot client.
- 392-rule subscription parsing, cache retention and failed-update behaviour.
- Phone VPN with per-app exclusions, split-screen and window resize.

## 0.2.0 - 2026-10-01

- Initial working core: HY2 upstream, local mixed proxy, foreground service, hotspot and LAN sharing.
