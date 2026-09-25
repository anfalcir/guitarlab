#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bs.py"
PATCH = ROOT / ".source-parts/U12btPrepareSuggestionAssertion.patch"
PATCH_BLOB = "36879c313adce3360216348031dcf6fab09e5516"
TARGET = ROOT / "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
BEFORE_BLOB = "9ddeef7af6ac6fda79e884ae69091aa554d2bc5b"
AFTER_BLOB = "a6a13fe9d70029c50c82042eb81f6d7e7b7106fd"

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bt patch blob mismatch")

if TARGET.is_file() and blob(TARGET) == AFTER_BLOB:
    text = TARGET.read_text(encoding="utf-8")
    token = 'assertTextEquals("Você quis dizer “Memphis May Fire”?")'
    if token not in text:
        raise SystemExit("U12bt semantic guard failed")
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bt")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bs materializer")
run("python3", str(PREVIOUS))
if not TARGET.is_file() or blob(TARGET) != BEFORE_BLOB:
    raise SystemExit("U12bt baseline blob mismatch")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if blob(TARGET) != AFTER_BLOB:
    raise SystemExit("U12bt terminal blob mismatch")
run("git", "diff", "--check")
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bt")
