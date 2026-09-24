#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12aq.py"
PATCH = ROOT / ".source-parts/U12arCloudBuildDiagnostics.patch"
PATCH_BLOB = "79265c6a9c7a02037ac67646a56e63e34001dcca"
TARGETS = {
    "cloud/remote-separation/scripts/deploy-worker.sh": ("4ec9975d7cc2727e30677491150aac73a1540fff", "c57a86dbad99c0a424019052b178c7a3fbab8fb2"),
    ".github/workflows/u7-cloud-backend.yml": ("7e1384e2f3c17f32576645e720b830aa208751cf", "cd3232486a216f757b8d9804e86880b0ef033ec0"),
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
    deploy = (ROOT / "cloud/remote-separation/scripts/deploy-worker.sh").read_text(encoding="utf-8")
    workflow = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(encoding="utf-8")
    if 'gcloud builds log "$BUILD_ID"' not in deploy:
        raise SystemExit("U12ar Cloud Build log guard failed")
    if "steps.worker.outcome == 'success'" not in workflow:
        raise SystemExit("U12ar W4 artifact masking guard failed")
    run("bash", "-n", "cloud/remote-separation/scripts/deploy-worker.sh")


if not PATCH.is_file():
    raise SystemExit("Missing U12ar Cloud Build diagnostics patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12ar patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12ar Cloud Build diagnostics")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12aq materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12ar baseline blob mismatch after U12aq")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12ar terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12ar Cloud Build diagnostics")
