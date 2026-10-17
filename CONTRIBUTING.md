# Contributing to Andi Proxy

Thanks for taking the time to improve Andi Proxy. Issues, pull requests, device reports and translation fixes are all welcome.

## Before you start

- Read [Using Andi Proxy](docs/USAGE.md) and [Architecture](docs/ARCHITECTURE.md).
- Search existing issues before opening a new one.
- For security problems, do **not** open a public issue. Follow [SECURITY.md](SECURITY.md).
- Never include server credentials, subscription tokens, private IPs or personal data in an issue, log or screenshot.

## Ways to contribute

### Report a bug

Use the bug-report template and include:

- Android version, device model and ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`)
- App version (Settings → About)
- Whether the phone VPN, hotspot sharing or both were active
- Exact steps, expected result and actual result
- The error text shown in the app, with credentials redacted

### Improve a translation

The UI uses the Chinese source string as a lookup key. Translations live in:

```text
app/src/main/assets/i18n/en.json
app/src/main/assets/i18n/ja.json
app/src/main/assets/i18n/ko.json
app/src/main/assets/i18n/ru.json
```

Edit values only, keep keys unchanged, preserve printf placeholders (`%s`, `%1$d`, …), and run:

```sh
python3 scripts/check-i18n.py
```

If a source key itself needs to change, update the Java call site and every table in the same pull request.

### Add a device report

If you can test on a device or ABI that is not in [Verification](docs/VERIFICATION.md), open an issue with the “device report” details. Positive and negative results are both useful. Note whether the device uses 4 KiB or 16 KiB memory pages if you know.

### Send code

1. Fork the repository and create a topic branch.
2. Follow the build steps in [Building](docs/BUILDING.md).
3. Keep changes focused. A pull request should do one thing.
4. Add or update tests when behaviour changes.
5. Run the checks below.
6. Open a pull request with a short explanation and any device/emulator evidence.

```sh
python3 scripts/fetch-core.py
python3 scripts/check-i18n.py
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Code guidelines

- Java 17, no AndroidX (the project deliberately uses the platform UI toolkit).
- Keep the programmatic UI style and the existing colour/spacing helpers.
- Put every user-facing string through `L.t("中文")` or `L.f("模板 %s", value)`. Do not leave hard-coded Chinese or English in activities or services.
- Do not log credentials. Use `CoreRunner`'s redaction when handling core output.
- Keep runtime files private (`0600`) and delete them when a session stops.
- Do not add analytics, ads, telemetry or a remote-control endpoint.
- Do not add a dependency or binary without updating [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the licence files.
- Prefer a clear error over silently ignoring an unsupported rule or profile field.

## Commit and pull-request style

- Write commit subjects in the imperative mood: `Add Korean translations`, `Fix hotspot stop race`.
- Explain *why* in the body, not just *what*.
- Reference the issue number when one exists.
- Keep generated files (`build/`, `.tools/`, `.so` libraries) out of commits.

## Licensing

By contributing, you agree that your contribution is licensed under **GPL-3.0-or-later**, the same licence as the project. You confirm that you have the right to submit the code or documentation. Do not copy code from a project with an incompatible licence.

## Review

Maintainers review for correctness, security, privacy and fit with the project's small scope. A pull request may be declined if it adds a large dependency, a cloud service, tracking or a feature that only works with root. Discussion is welcome before a large change.
