#!/usr/bin/env python3
"""Validate the runtime translation tables against the strings used in Java.

The Chinese source text is the lookup key. Every table must contain exactly the
same keys, keep printf placeholders intact, and avoid untranslated Han text.
"""
from __future__ import annotations

import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java"
I18N = ROOT / "app/src/main/assets/i18n"
LANGS = ("en", "ja", "ko", "ru")
HAN = re.compile(r"[\u3400-\u9fff]")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfxXeEgGc]")


def java_strings(text: str) -> list[str]:
    """Return every string literal appearing inside an L.t(...) / L.f(...) call."""
    found: list[str] = []
    for match in re.finditer(r"L\.[tf]\(", text):
        index = match.end()
        depth = 1
        while index < len(text) and depth:
            char = text[index]
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
            elif char == '"':
                end = index + 1
                while end < len(text):
                    if text[end] == "\\":
                        end += 2
                        continue
                    if text[end] == '"':
                        break
                    end += 1
                literal = text[index + 1 : end]
                if HAN.search(literal):
                    try:
                        found.append(json.loads('"' + literal + '"'))
                    except json.JSONDecodeError:
                        pass
                index = end
            index += 1
    return found


def keys_in_source() -> set[str]:
    keys: set[str] = set()
    for path in JAVA.rglob("*.java"):
        keys.update(java_strings(path.read_text(encoding="utf-8")))
    return keys


def load(language: str) -> dict[str, str]:
    path = I18N / f"{language}.json"
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        raise SystemExit(f"missing translation table: {path}")
    except json.JSONDecodeError as error:
        raise SystemExit(f"{path}: invalid JSON: {error}")
    if not isinstance(data, dict):
        raise SystemExit(f"{path}: expected a JSON object")
    return data


def main() -> int:
    expected = keys_in_source()
    failed = False
    if not expected:
        raise SystemExit("no translatable strings found")

    for language in LANGS:
        table = load(language)
        missing = sorted(expected - set(table))
        extra = sorted(set(table) - expected)
        if missing:
            failed = True
            print(f"{language}: missing {len(missing)} keys")
            for key in missing[:20]:
                print("  missing:", json.dumps(key, ensure_ascii=False))
        if extra:
            failed = True
            print(f"{language}: {len(extra)} stale keys")
            for key in extra[:20]:
                print("  stale:", json.dumps(key, ensure_ascii=False))

        for key, value in table.items():
            if not isinstance(value, str) or not value.strip():
                failed = True
                print(f"{language}: empty translation for {json.dumps(key, ensure_ascii=False)}")
                continue
            if sorted(PLACEHOLDER.findall(key)) != sorted(PLACEHOLDER.findall(value)):
                failed = True
                print(f"{language}: placeholder mismatch for {json.dumps(key, ensure_ascii=False)}")
                print("  value:", json.dumps(value, ensure_ascii=False))
            if language in ("en", "ko", "ru") and HAN.search(value):
                failed = True
                print(f"{language}: untranslated Han text for {json.dumps(key, ensure_ascii=False)}")
                print("  value:", json.dumps(value, ensure_ascii=False))
        print(f"{language}: {len(table)} keys")

    resource_failed = check_resource_strings()
    if failed or resource_failed:
        return 1
    print(f"OK: {len(expected)} keys across {len(LANGS)} languages and matching XML labels")
    return 0


def check_resource_strings() -> bool:
    """The manifest/widget XML labels use Android resources, not the JSON tables."""
    base_dir = ROOT / "app/src/main/res/values"
    dirs = [base_dir] + [ROOT / f"app/src/main/res/values-{code}" for code in ("zh-rCN", "ja", "ko", "ru")]
    tables: dict[str, dict[str, str]] = {}
    for directory in dirs:
        path = directory / "strings.xml"
        if not path.exists():
            print(f"missing resource file: {path}")
            return True
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as error:
            print(f"{path}: invalid XML: {error}")
            return True
        tables[directory.name] = {entry.get("name"): (entry.text or "") for entry in root.findall("string")}
    base = tables["values"]
    failed = False
    for name, table in tables.items():
        missing = sorted(set(base) - set(table))
        extra = sorted(set(table) - set(base))
        if missing or extra:
            failed = True
            print(f"{name}: resource keys missing={missing} extra={extra}")
        for key, value in table.items():
            if not value.strip():
                failed = True
                print(f"{name}: empty resource {key}")
    return failed


if __name__ == "__main__":
    sys.exit(main())
