#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12al.py"
PATCH = ROOT / ".source-parts/U12amShadowRecoveryGate.patch"
PATCH_BLOB = "7728421cb7b29b153cc1c6b3e45c2a55598f847c"
TARGET = ROOT / "scripts/u4_cloud_integration_smoke.sh"
TARGET_BEFORE = "358785d3c2c97b94c9373ecd20bf987120d501ec"
TARGET_AFTER = "2b78584d287805598fccd8ac3a547eedb52f6acc"


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))


def ready() -> bool:
    return TARGET.is_file() and blob(TARGET) == TARGET_AFTER


def verify() -> None:
    run("git", "diff", "--check")
    run("bash", "-n", str(TARGET))
    text = TARGET.read_text(encoding="utf-8")
    required = (
        'GBW_RECOVERY_GATE_REQUIRED:-1',
        'findRecoverableRemoteSeparation',
        'EXISTING_SAME_GENERATION',
        'worker-only shadow stage',
    )
    for needle in required:
        if needle not in text:
            raise SystemExit(f"U12am semantic guard failed: {needle}")


if not PATCH.is_file():
    raise SystemExit("Missing U12am shadow recovery gate patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12am patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12am shadow recovery gate")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12al materializer")
run("python3", str(PREVIOUS))
if not TARGET.is_file() or blob(TARGET) != TARGET_BEFORE:
    raise SystemExit("U12am before-blob mismatch")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12am terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12am shadow recovery gate")
