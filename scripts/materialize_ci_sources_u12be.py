#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bd.py"
PATCH = ROOT / ".source-parts/U12beRiskScopedCriticalGate.patch"
PATCH_BLOB = "c806d068766ba36f415a1c3ded0574c479e10cec"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": (
        "6a09b304930cbe7175d4da3463d7a8b22e692142",
        "4987ff605e4d612dcca5cf74602dcc93caa281f1",
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
    for token in (
        'v.get("VulnerabilityID") == "CVE-2023-45853"',
        'v.get("PkgName") == "zlib1g"',
        'v.get("Status") == "will_not_fix"',
        'and not v.get("FixedVersion")',
        '"blockingCritical"',
        "if blocking_critical:",
    ):
        if token not in workflow:
            raise SystemExit(f"U12be risk-scoped vulnerability guard failed: {token}")


if not PATCH.is_file():
    raise SystemExit("Missing U12be risk-scoped vulnerability patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12be patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12be risk-scoped vulnerability gate")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bd materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12be baseline blob mismatch after U12bd")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12be terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12be risk-scoped vulnerability gate")
