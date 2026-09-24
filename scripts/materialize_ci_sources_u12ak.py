#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12aj.py"
PATCH = ROOT / ".source-parts/U12akRemoteRecoveryTest.patch"
PATCH_BLOB = "01fee5e1027b4670d81ac1e652f0cc23b4a04516"
TARGET = ROOT / "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt"
TARGET_AFTER = "07d2e2a4391340b0cafc1e245db8d65bbb80ccde"


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
    text = TARGET.read_text(encoding="utf-8")
    if "if (identity.jobId == requested.jobId) return null" not in text:
        raise SystemExit("U12ak recovery-test semantic guard failed")


if not PATCH.is_file():
    raise SystemExit("Missing U12ak recovery test patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12ak patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12ak recovery test corrective")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12aj materializer")
run("python3", str(PREVIOUS))
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
run("git", "diff", "--check")
if not ready():
    raise SystemExit("U12ak terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12ak recovery test corrective")
