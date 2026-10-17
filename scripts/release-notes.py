#!/usr/bin/env python3
"""Build a GitHub Release body from CHANGELOG.md plus download links."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

APK_ABIS = (
    ("arm64-v8a", "Most modern phones (64-bit ARM)"),
    ("armeabi-v7a", "Older 32-bit ARM phones"),
    ("x86_64", "x86_64 emulators and tablets"),
    ("x86", "Legacy x86 emulators"),
    ("universal", "Universal (all ABIs, larger download)"),
)


def changelog_section(text: str, version: str) -> str | None:
    pattern = re.compile(rf"^##\s+{re.escape(version)}[^\n]*\n", re.MULTILINE)
    match = pattern.search(text)
    if not match:
        return None
    next_heading = re.search(r"^##\s+", text[match.end():], re.MULTILINE)
    end = match.end() + next_heading.start() if next_heading else len(text)
    return text[match.end():end].strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True, help="version without the v prefix, e.g. 1.0.0")
    parser.add_argument("--output", required=True, help="write the release body to this file")
    parser.add_argument("--changelog", default="CHANGELOG.md")
    parser.add_argument("--repo", default="kongdayan/andi-proxy")
    parser.add_argument("--tag", help="release tag (default: v<version>)")
    args = parser.parse_args()

    version = args.version
    tag = args.tag or f"v{version}"
    changelog = pathlib.Path(args.changelog)
    if not changelog.exists():
        print(f"missing changelog: {changelog}", file=sys.stderr)
        return 1
    section = changelog_section(changelog.read_text(encoding="utf-8"), version)
    if not section:
        print(f"no CHANGELOG.md section for {version}", file=sys.stderr)
        return 1

    base = f"https://github.com/{args.repo}"
    # Changelog links are relative to the repository; make them work on the release page too.
    section = re.sub(
        r"\]\((?!https?://|#|mailto:)([^)]+)\)",
        lambda match: f"]({base}/blob/{tag}/{match.group(1)})",
        section,
    )
    lines = [
        f"# Andi Proxy {version}",
        "",
        section,
        "",
        "## Downloads",
        "",
        "| Device | APK |",
        "| --- | --- |",
    ]
    for abi, description in APK_ABIS:
        lines.append(f"| {description} | [AndiProxy-{abi}.apk]({base}/releases/download/{tag}/AndiProxy-{abi}.apk) |")
    lines += [
        "",
        f"APKs are signed with the project release key. Verify the download with "
        f"[SHA256SUMS]({base}/releases/download/{tag}/SHA256SUMS) and read "
        f"[SIGNING.txt]({base}/releases/download/{tag}/SIGNING.txt) for the signing note.",
        "",
        "Requires Android 8.0 (API 26) or newer. Sideload the APK; the project is not on Google Play.",
        "",
        f"Full changelog: [CHANGELOG.md]({base}/blob/{tag}/CHANGELOG.md) · "
        f"[Source]({base}/tree/{tag}) · [Issues]({base}/issues)",
        "",
    ]
    pathlib.Path(args.output).write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {args.output} for {tag}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
