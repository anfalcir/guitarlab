#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bp.py"
PATCH = ROOT / ".source-parts/U12bqApi36PosixRunner.patch"
PATCH_BLOB = "57d60e5542be38cc45600110b9c75d306cc2457c"
TARGET = ROOT / ".github/workflows/android-ci.yml"
BEFORE_BLOB = "9f4d6144da5b36b40a5d4dfa5a6a859007c4d7c4"
AFTER_BLOB = "d403499821d1653b6934fddd22141a980a9684d8"

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bq patch blob mismatch")

if TARGET.is_file() and blob(TARGET) == AFTER_BLOB:
    text = TARGET.read_text(encoding="utf-8")
    for token in ("set -eu", 'while [ "$attempt" -le 30 ]', '[ "$state" = "device" ]'):
        if token not in text:
            raise SystemExit(f"U12bq semantic guard failed: {token}")
    if "[[" in text or "{1..30}" in text:
        raise SystemExit("U12bq stale bash-only syntax remains")
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bq")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bp materializer")
run("python3", str(PREVIOUS))
if not TARGET.is_file() or blob(TARGET) != BEFORE_BLOB:
    raise SystemExit("U12bq baseline blob mismatch")

run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if blob(TARGET) != AFTER_BLOB:
    raise SystemExit("U12bq terminal blob mismatch")
run("git", "diff", "--check")
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bq")
