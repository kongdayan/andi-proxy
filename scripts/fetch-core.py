#!/usr/bin/env python3
"""Download pinned official sing-box and hev-socks5-tunnel binaries.

Every archive is checked against the official release SHA-256 before it is
unpacked. Only known members are extracted, and the script never executes
downloaded code. By default all Android ABIs and the macOS arm64 host binary
(used by scripts/core_smoke.py) are fetched.
"""
from __future__ import annotations

import argparse
import hashlib
import pathlib
import shutil
import sys
import tarfile
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
DOWNLOADS = ROOT / ".tools/downloads"
JNI = ROOT / "app/src/main/jniLibs"

SING_BOX_VERSION = "1.14.2"
# platform -> (Android ABI or host, official archive SHA-256)
SING_BOX = {
    "android-arm64": ("arm64-v8a", "57eeb81867a9a5e406bcbb5f7fde352acdc354e8c084173a28bdd9417c5e0cba"),
    "android-arm": ("armeabi-v7a", "10352dab32c87e2eefe3fd61e21bd259900ef4096ced311ec49050769613e861"),
    "android-amd64": ("x86_64", "5a16af1bcb373d5e8824b7cca415813d3556dd10e7d41e5dd4dfb3cb9e9240ff"),
    "android-386": ("x86", "855ad664d406435f48c65488baf07cef0bd2c11e833c2c5a80b63f48ce6340db"),
    "darwin-arm64": ("host", "925c5382eca8492b0150f868a6db20b18290a38700e621724b3703fd453e032d"),
}
HEV_VERSION = "2.18.0"
HEV_DIGEST = "15ec8ed121663b562c99caa5bb602d1009f24e5b09e733438b81988f12feaaab"
HEV_ABIS = {"arm64-v8a", "armeabi-v7a", "x86_64", "x86"}


def download(url: str, target: pathlib.Path) -> None:
    if target.exists():
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".part")
    print(f"Downloading {url}")
    request = urllib.request.Request(url, headers={"User-Agent": "AndiProxy-build"})
    with urllib.request.urlopen(request, timeout=180) as response, temporary.open("wb") as out:
        shutil.copyfileobj(response, out, length=1024 * 1024)
    temporary.replace(target)


def verify(path: pathlib.Path, digest: str) -> None:
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual != digest:
        path.unlink(missing_ok=True)
        raise SystemExit(f"SHA-256 mismatch for {path.name}: expected {digest}, got {actual}")


def extract_sing_box(platform: str, abi: str) -> None:
    filename = f"sing-box-{SING_BOX_VERSION}-{platform}.tar.gz"
    archive = DOWNLOADS / filename
    digest = SING_BOX[platform][1]
    download(f"https://github.com/SagerNet/sing-box/releases/download/v{SING_BOX_VERSION}/{filename}", archive)
    verify(archive, digest)
    output = ROOT / ".tools/core" / f"sing-box-{SING_BOX_VERSION}-{platform}"
    output.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive) as tar:
        member = tar.getmember(f"sing-box-{SING_BOX_VERSION}-{platform}/sing-box")
        if not member.isfile():
            raise SystemExit(f"Unexpected archive member in {filename}")
        binary = tar.extractfile(member)
        if binary is None:
            raise SystemExit(f"Missing executable in {filename}")
        host = output / "sing-box"
        host.write_bytes(binary.read())
        host.chmod(0o755)
    if abi != "host":
        target = JNI / abi / "libsingbox.so"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(host.read_bytes())
        target.chmod(0o755)
        print(f"Verified {filename} -> {target.relative_to(ROOT)}")
    else:
        print(f"Verified {filename} -> {host.relative_to(ROOT)}")


def extract_hev(abis: set[str]) -> None:
    archive = DOWNLOADS / "hev-socks5-tunnel.aar"
    download(f"https://github.com/heiher/hev-socks5-tunnel/releases/download/{HEV_VERSION}/hev-socks5-tunnel.aar", archive)
    verify(archive, HEV_DIGEST)
    with zipfile.ZipFile(archive) as bundle:
        names = set(bundle.namelist())
        for abi in sorted(abis & HEV_ABIS):
            member = f"jni/{abi}/libhev-socks5-tunnel.so"
            if member not in names:
                raise SystemExit(f"HEV AAR is missing {member}")
            target = JNI / abi / "libhev-socks5-tunnel.so"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(bundle.read(member))
            target.chmod(0o755)
            print(f"Verified hev-socks5-tunnel {HEV_VERSION} -> {target.relative_to(ROOT)}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--abi", action="append", choices=sorted(HEV_ABIS), help="Android ABI to fetch (repeatable)")
    parser.add_argument("--no-host", action="store_true", help="Skip the macOS arm64 binary used by core_smoke.py")
    parser.add_argument("--clean", action="store_true", help="Remove existing libsingbox.so / HEV libraries first")
    args = parser.parse_args()

    abis = set(args.abi) if args.abi else set(HEV_ABIS)
    if args.clean:
        for abi in abis:
            for name in ("libsingbox.so", "libhev-socks5-tunnel.so"):
                (JNI / abi / name).unlink(missing_ok=True)

    for platform, (abi, _) in SING_BOX.items():
        if abi == "host" and args.no_host:
            continue
        if abi != "host" and abi not in abis:
            continue
        extract_sing_box(platform, abi)
    extract_hev(abis)
    print("All pinned binaries verified.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
