#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bu.py"
PATCH = ROOT / ".source-parts/U12bvSettingsTestAssertion.patch"
PATCH_BLOB = "25f34d99cf516487b8565e9b92a39300e6d6f6c6"
TARGET = ROOT / "app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
BEFORE_BLOB = "79ca916734328b65be5ec8adfc7f2174c10380a6"
AFTER_BLOB = "36543c47e4c8ee4da843b4d6a8a0d21c6bce651d"

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bv patch blob mismatch")

if TARGET.is_file() and blob(TARGET) == AFTER_BLOB:
    text = TARGET.read_text(encoding="utf-8")
    if "assertDoesNotExist" in text:
        raise SystemExit("U12bv unsupported assertion remains")
    for token in ('onAllNodesWithText("Projeto e Studio").assertCountEquals(0)',
                  'onAllNodesWithText("Importação").assertCountEquals(0)',
                  'onAllNodesWithText("Ver atividade").assertCountEquals(0)'):
        if token not in text:
            raise SystemExit(f"U12bv semantic guard failed: {token}")
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bv")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bu materializer")
run("python3", str(PREVIOUS))
if not TARGET.is_file() or blob(TARGET) != BEFORE_BLOB:
    raise SystemExit("U12bv baseline blob mismatch")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if blob(TARGET) != AFTER_BLOB:
    raise SystemExit("U12bv terminal blob mismatch")
run("git", "diff", "--check")
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bv")
