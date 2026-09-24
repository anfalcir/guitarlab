#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ar.py"
PATCH = ROOT / ".source-parts/U12asCloudLoggingDiagnostics.patch"
PATCH_BLOB = "90b3dc5352fd3578c952ae65f714a6ce78824b45"
TARGETS = {
    "cloud/remote-separation/scripts/deploy-worker.sh": ("c57a86dbad99c0a424019052b178c7a3fbab8fb2", "e968f381bf671d415b944ea48605f391d2f063b9"),
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
    if 'gcloud beta builds log "$BUILD_ID"' not in deploy:
        raise SystemExit("U12as Cloud Logging diagnostic guard failed")
    run("bash", "-n", "cloud/remote-separation/scripts/deploy-worker.sh")

if not PATCH.is_file():
    raise SystemExit("Missing U12as Cloud Logging diagnostics patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12as patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12as Cloud Logging diagnostics")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12ar materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12as baseline blob mismatch after U12ar")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12as terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12as Cloud Logging diagnostics")
