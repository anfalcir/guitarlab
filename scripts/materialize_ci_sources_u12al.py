#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ak.py"
PATCH = ROOT / ".source-parts/U12alBackendRecoveryCallableRebuild.patch"
PATCH_BLOB = "95f69d720461a18c46f1bdea078afae5034bf56e"
TARGET = ROOT / "cloud/remote-separation/functions/src/index.ts"
TARGET_AFTER = "b0c77616fa8b6d5beaf0056e62b270af89091c88"


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
    required = (
        "findRecoverableRemoteSeparation",
        "EXISTING_SAME_GENERATION",
        "cleanupUnregisteredUpload",
        "ACTIVE_JOB_CONFLICT",
        "MONTHLY_QUOTA_REACHED",
    )
    for needle in required:
        if needle not in text:
            raise SystemExit(f"U12al backend semantic guard failed: {needle}")
    if text.count('import {ExecutionsClient, JobsClient}') != 1:
        raise SystemExit("U12al backend source duplication detected")


if not PATCH.is_file():
    raise SystemExit("Missing U12al backend callable rebuild patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12al patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12al backend callable rebuild")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12ak materializer")
run("python3", str(PREVIOUS))
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
run("git", "diff", "--check")
if not ready():
    raise SystemExit("U12al terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12al backend callable rebuild")
