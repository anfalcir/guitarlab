#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12br.py"
PATCH = ROOT / ".source-parts/U12bsPrepareSearchTestImports.patch"
PATCH_BLOB = "1a5d9a7c37562e12d8166f91c4c2c194feb3a119"
TARGET = ROOT / "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
BEFORE_BLOB = "f355326a2f8d71313f77d36afd5a5873f7d4ef75"
AFTER_BLOB = "9ddeef7af6ac6fda79e884ae69091aa554d2bc5b"

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bs patch blob mismatch")

if TARGET.is_file() and blob(TARGET) == AFTER_BLOB:
    text = TARGET.read_text(encoding="utf-8")
    for token in ("import studio.guitarlab.app.ui.SourceSearchOutcome", "import studio.guitarlab.app.ui.SourceSearchTerminalState"):
        if token not in text:
            raise SystemExit(f"U12bs semantic guard failed: {token}")
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bs")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12br materializer")
run("python3", str(PREVIOUS))
if not TARGET.is_file() or blob(TARGET) != BEFORE_BLOB:
    raise SystemExit("U12bs baseline blob mismatch")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if blob(TARGET) != AFTER_BLOB:
    raise SystemExit("U12bs terminal blob mismatch")
run("git", "diff", "--check")
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bs")
