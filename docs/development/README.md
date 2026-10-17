# Development notes

These are historical design and verification notes, kept in Chinese and sanitised for publication. They document how the no-root hotspot proxy approach was chosen and what was exercised on real hardware.

- [Investigation](INVESTIGATION.zh-CN.md) — why an explicit application-layer proxy is used, the Android hotspot/VPN APIs considered, and the core/protocol choices.
- [Completion checklist](COMPLETION.zh-CN.md) — the 0.2/0.3 feature acceptance matrix.

Current behaviour, setup and limits are documented in:

- [Using Andi Proxy](../USAGE.md)
- [Architecture](../ARCHITECTURE.md)
- [Verification](../VERIFICATION.md)

Device identifiers, UIDs, local addresses and public exit IPs have been redacted. Some notes refer to the 0.2/0.3 UI and version numbers; the source code and current documentation take precedence.
