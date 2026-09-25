#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bh.py"
PATCH = ROOT / ".source-parts/U12biW4StrategyContract.patch"
PATCH_BLOB = "b5f66facaac5376cea1da156b5fb70c1f1acb5b9"
TARGETS = {
    "scripts/u12_rc20_shadow_benchmark.sh": (
        "cdffef35b3d691e406450a50a372ca67a15af1d7",
        "59d3b857fd9422cb921d4608d7daef7922ff0f85",
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
    script = (ROOT / "scripts/u12_rc20_shadow_benchmark.sh").read_text(encoding="utf-8")
    for token in (
        'EXPECTED_ENGINE_STRATEGY="pytorch-cpu-s1-o0.5-t8"',
        'EXPECTED_ENGINE_STRATEGY="pytorch-cpu-s1-o0.5-t4"',
        'assert data["strategy"]==expected_engine_strategy',
        'engine_strategy=${EXPECTED_ENGINE_STRATEGY}',
    ):
        if token not in script:
            raise SystemExit(f"U12bi W4 strategy-contract guard failed: {token}")
    run("bash", "-n", str(ROOT / "scripts/u12_rc20_shadow_benchmark.sh"))


if not PATCH.is_file():
    raise SystemExit("Missing U12bi W4 strategy-contract patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bi patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bi W4 strategy contract")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bh materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bi baseline blob mismatch after U12bh")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bi terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bi W4 strategy contract")
