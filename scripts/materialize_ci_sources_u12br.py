#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bq.py"
PATCH = ROOT / ".source-parts/U12brApi36SingleCommand.patch"
PATCH_BLOB = "c6e6303ad343a685e903d6c2048aae9d485cc1f2"
TARGETS = {
    ".github/workflows/android-ci.yml": (
        "d403499821d1653b6934fddd22141a980a9684d8",
        "fad97d82feb666dcb29fae83a685bcfa4e870b89",
    ),
    "scripts/ci_run_api36_regression_groups.sh": (
        "3572294655313543420f5f91ecb9a9a95d1570f4",
        "55bd9a9367fb4f2b02903755d0428231259c728b",
    ),
}

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

def ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == after for rel, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == before for rel, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check")
    workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
    script = (ROOT / "scripts/ci_run_api36_regression_groups.sh").read_text(encoding="utf-8")
    required = (
        (workflow, "script: bash scripts/ci_run_api36_regression_groups.sh"),
        (script, "API36 device readiness"),
        (script, "DiagnosticSupportInstrumentedTest"),
        (script, 'test "$(adb get-state)" = "device"'),
    )
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"U12br semantic guard failed: {token}")
    if "script: |" in workflow and "ci_run_api36_regression_groups.sh" in workflow:
        raise SystemExit("U12br multiline runner script unexpectedly remains")
    run("bash", "-n", str(ROOT / "scripts/ci_run_api36_regression_groups.sh"))

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12br patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12br")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bq materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12br baseline blob mismatch")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12br terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12br")
