# Security policy

## Supported versions

Security fixes are made on the latest release. Older releases may not receive patches.

| Version | Supported |
| --- | --- |
| 1.x | Yes |
| 0.x | No |

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

Use GitHub's private vulnerability reporting on the repository:

<https://github.com/kongdayan/andi-proxy/security/advisories/new>

Include:

- affected version and ABI
- a description of the issue and its impact
- reproduction steps or a minimal proof of concept
- whether the issue requires a malicious subscription, a malicious local app, a hostile network or physical access
- any suggested fix

Do not include real server credentials, subscription tokens or personal data. If a secret was exposed, rotate it first.

## What to expect

- Acknowledgement within a reasonable time.
- A private discussion to confirm the issue and plan a fix.
- A coordinated disclosure after a fix is available. Credit is given unless you prefer otherwise.

## Scope

In scope:

- the Android app, services, VPN and hotspot lifecycle
- profile encryption and credential handling
- subscription download, parsing, caching and redirect handling
- the generated sing-box config and process invocation
- release signing and build provenance
- bundled third-party binaries and their notices

Out of scope:

- vulnerabilities in sing-box or hev-socks5-tunnel that are not caused by how Andi Proxy packages or invokes them. Report those upstream:
  - <https://github.com/SagerNet/sing-box/security>
  - <https://github.com/heiher/hev-socks5-tunnel/issues>
- the content, availability or logging of a user-supplied proxy server
- the content or licensing of a user-supplied subscription
- devices that are already rooted or running a hostile ROM
- denial of service caused by intentionally configuring an invalid or unreachable server
- Android platform behaviour that the app cannot control (for example, one active VPN)

## Safe defaults

The project aims to keep:

- credentials encrypted at rest with Android Keystore
- backup and device transfer disabled
- cleartext traffic disabled
- subscription URLs HTTPS-only, without embedded credentials or fragments
- runtime configs private and deleted on stop
- no telemetry, remote control or analytics
- pinned third-party binaries verified by SHA-256 during the build
