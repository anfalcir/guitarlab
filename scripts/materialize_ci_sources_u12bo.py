#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bn.py"
PATCH = ROOT / ".source-parts/U12boAcceptedManifestTyping.patch"
PATCH_BLOB = "cbae654a43d3b019056ac3663fc6ec1f9b174c44"
TARGET = ROOT / "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/AcceptedRemoteManifestStore.kt"
BEFORE_BLOB = "52fe29477d55fbc69082ab8a833acb12bbb2d0c0"
AFTER_BLOB = "4cee4f52006baa2ca7a7bbf15a0ea4613b5b0d58"

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bo patch blob mismatch")

if TARGET.is_file() and blob(TARGET) == AFTER_BLOB:
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bo")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bn materializer")
run("python3", str(PREVIOUS))

if not TARGET.is_file() or blob(TARGET) != BEFORE_BLOB:
    raise SystemExit("U12bo baseline blob mismatch")

run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))

if blob(TARGET) != AFTER_BLOB:
    raise SystemExit("U12bo terminal blob mismatch")

run("git", "diff", "--check")
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bo")
