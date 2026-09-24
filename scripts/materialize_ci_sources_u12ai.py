#!/usr/bin/env python3
from __future__ import annotations

import base64
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ah.py"
PATCH_B64 = ROOT / ".source-parts/U12aiBackupRestoreTest.patch.b64"
PATCH_B64_BLOB = "d00befc5d57c2de70921aaf889f5b526e9cc1c13"
PATCH_BLOB = "7fc1ba9c69ec3f1a3f43e0fbf193828d330726ed"
TARGET = ROOT / "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
TARGET_BEFORE = "bc29a07f014aa8e2a4fe5a4dc9d98d2ce598b46c"
TARGET_AFTER = "67b200b6be911830628158f73d9475f150689951"


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ai backup restore test payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ai base64 payload blob mismatch")
try:
    patch = base64.b64decode(b"".join(PATCH_B64.read_bytes().split()), validate=True)
except Exception as exc:
    raise SystemExit(f"U12ai base64 payload is invalid: {exc}") from exc

with tempfile.NamedTemporaryFile(prefix="u12ai-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ai decoded patch blob mismatch")

    if out("git", "hash-object", str(TARGET)) == TARGET_AFTER:
        run("git", "diff", "--check")
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ai backup restore correction")
    else:
        if not PREVIOUS.is_file():
            raise SystemExit("Missing U12ah materializer")
        run("python3", str(PREVIOUS))
        if out("git", "hash-object", str(TARGET)) != TARGET_BEFORE:
            raise SystemExit("U12ai expected the pre-correction backup restore test blob")
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if out("git", "hash-object", str(TARGET)) != TARGET_AFTER:
            raise SystemExit("U12ai final backup restore test blob mismatch")
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ai backup restore correction")
finally:
    temp.unlink(missing_ok=True)