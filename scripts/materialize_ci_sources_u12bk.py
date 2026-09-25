#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bj.py"
PATCH = ROOT / ".source-parts/U12bkWorkflowConsolidation.patch"
PATCH_BLOB = "89535cb7635e40510d9c40ea4b8e5a7e638db56c"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": (
        "051097bd37b643745895bae7e2ef66a58cec68b6",
        "b1674e09b690173dda49d412872cb2c0b1bf2528",
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
    workflow = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(encoding="utf-8")
    required = (
        "Run consolidated W3/W4 shadow qualification",
        "id: w3_w4_baseline",
        "GBW_W4_MODEL_PROBE: '0'",
        "Exercise full U4 prepared-reference v2 contract after production cutover",
        "Set up Node.js for production Functions and U4 smoke",
        "u12-rc20-w3-w4-shadow-",
    )
    for token in required:
        if token not in workflow:
            raise SystemExit(f"U12bk workflow consolidation guard failed: {token}")
    forbidden = (
        "Run W3 local/container versus Cloud Run parity",
        "w5_listening:",
        "[run u7 w5]",
        "GBW_W4_MODEL_PROBE: '1'",
    )
    for token in forbidden:
        if token in workflow:
            raise SystemExit(f"U12bk stale workflow path remains: {token}")
    if workflow.count("bash scripts/u4_cloud_integration_smoke.sh") != 1:
        raise SystemExit("U12bk U4 smoke must have exactly one workflow invocation")
    if workflow.count("bash scripts/u12_rc20_shadow_benchmark.sh") != 2:
        raise SystemExit("U12bk expected baseline plus optional CPU4 benchmark invocations")


if not PATCH.is_file():
    raise SystemExit("Missing U12bk workflow-consolidation patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bk patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bk workflow consolidation")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bj materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bk baseline blob mismatch after U12bj")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bk terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bk workflow consolidation")
