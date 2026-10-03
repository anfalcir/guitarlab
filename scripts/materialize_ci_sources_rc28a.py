#!/usr/bin/env python3
"""RC28a: qualify RC28 with the module-correct JUnit4 test annotation."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc28.py"
PATCH = ROOT / ".source-parts/RC28aUnitTestImportFix.patch"
PATCH_BLOB = "0ba1d9744df39a8fedde839a14c48ca2c420ee41"
TARGETS = {
    "platform/audio-android/src/test/kotlin/studio/guitarlab/platform/audio/android/AndroidOutputRouteIdentityTest.kt": ("1c17e57a57a8bb56bd7f5a96b5637fdad9b67607", "838a00c9c7c1bf797416e54c41cf0891874912bf"),
    "README.md": ("c483a5e8d158b4d8ba1470aae7b7a40eeb49b067", "6c5896ee3565ecd309cd679f44d7d37f76baaa97"),
    "docs/CURRENT_STATE.md": ("aaa3c351059a717e6774c57af20b95a18d9354c0", "7abad7cf2e71243386d37c6e2ecc03626d24453b"),
    "docs/CI_PIPELINE.md": ("3b11d0358a225ec0cc62e83f4a31febe52417e7a", "da6e59c1a0d17dc253c8c2c343c40e53a0069972"),
    "docs/history/RC28_CUE_PHYSICAL_ROUTE_IDENTITY_2026-10-03.md": ("bfd0145a3ecd18785961c00faaadb7cf4e7b40b1", "423c9ca1f0aa4c083620ea20b08cc3c1d88301a8"),
}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    test = (ROOT / "platform/audio-android/src/test/kotlin/studio/guitarlab/platform/audio/android/AndroidOutputRouteIdentityTest.kt").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    ci = (ROOT / "docs/CI_PIPELINE.md").read_text()
    history = (ROOT / "docs/history/RC28_CUE_PHYSICAL_ROUTE_IDENTITY_2026-10-03.md").read_text()
    if "import org.junit.Test" not in test or "import kotlin.test.Test" in test:
        raise SystemExit("RC28a JUnit4 import guard failed")
    if "CI #957 result" not in current or "37122989286" not in current:
        raise SystemExit("RC28a current-state failure evidence guard failed")
    if "RC28a qualification-only correction" not in ci:
        raise SystemExit("RC28a CI-pipeline guard failed")
    if "CI #957 — test annotation import failure" not in history:
        raise SystemExit("RC28a history guard failed")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC28a patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC28a unit-test import correction")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC28a baseline blob mismatch after RC28")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC28a terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC28a unit-test import correction")
