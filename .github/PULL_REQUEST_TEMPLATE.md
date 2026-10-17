## Summary

<!-- What does this change, and why? One or two sentences. -->

## Related issue

<!-- Fixes #... or N/A -->

## Type

- [ ] Bug fix
- [ ] Feature
- [ ] Translation
- [ ] Documentation
- [ ] Build / release
- [ ] Refactor (no behaviour change)

## Testing

<!-- Commands and devices used. Remove credentials and personal data. -->

- [ ] `python3 scripts/check-i18n.py`
- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- [ ] Device/emulator test (describe model, Android version, ABI, mode)
- [ ] Not applicable

## Checklist

- [ ] User-facing strings go through `L.t(...)` / `L.f(...)`.
- [ ] New translation keys exist in every table.
- [ ] No credentials, tokens, private addresses or personal data are included.
- [ ] Third-party notices were updated if a dependency or binary changed.
- [ ] The change keeps the no-root, no-telemetry scope.
