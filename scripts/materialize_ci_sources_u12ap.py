#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ao.py"
PATCH = ROOT / ".source-parts/U12apRc20AndroidManifest.patch"
PATCH_BLOB = "82942b6e5d630fc173d51ebda79364eb811776af"
TARGETS = {
    "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt": ("c72c45865fed557e0f9e59446875905e0e7066ee", "7b026fa974024e658f00e8e4ee161ddb9095e829"),
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodec.kt": ("24a8e53570c6897cf45541535497758c548537dc", "9b22eed91ba91ce37523036a289503818ca252d0"),
    "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt": ("07d2e2a4391340b0cafc1e245db8d65bbb80ccde", "0a7294bd3553806509d72e3e247ed118e5e485e9"),
    "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodecTest.kt": ("28345a2678e83d79838a0c33932bdb8da65d229e", "4e388f320394a1251eb2f4ad684eb8fbb08b76dd"),
}


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))


def ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == after for rel, (_, after) in TARGETS.items())


def baseline_ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == before for rel, (before, _) in TARGETS.items())


def verify() -> None:
    run("git", "diff", "--check")
    core = (ROOT / "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt").read_text(encoding="utf-8")
    codec = (ROOT / "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodec.kt").read_text(encoding="utf-8")
    tests = (ROOT / "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodecTest.kt").read_text(encoding="utf-8")
    if 'OFFICIAL_ENGINE="demucs-pytorch"' not in core:
        raise SystemExit("U12ap Android engine guard failed")
    if 'OFFICIAL_MODEL_SHA256="34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd"' not in core:
        raise SystemExit("U12ap Android checkpoint guard failed")
    for field in ("device", "shifts", "overlap", "demucsVersion", "pytorchVersion", "modelBytes"):
        if field not in codec:
            raise SystemExit(f"U12ap codec field guard failed: {field}")
    if 'legacyEngine.validateFor' not in tests:
        raise SystemExit("U12ap legacy v2 rejection regression missing")


if not PATCH.is_file():
    raise SystemExit("Missing U12ap RC20 Android manifest patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12ap patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12ap RC20 Android manifest")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12ao materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12ap baseline blob mismatch after U12ao")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12ap terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12ap RC20 Android manifest")
