#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12at.py"
PATCH = ROOT / ".source-parts/U12auDirectArtifactRegistryPublish.patch"
PATCH_BLOB = "dba2b87b557526b592edd2e6e0127b195cd24b18"
TARGETS = {
    "cloud/remote-separation/scripts/deploy-worker.sh": ("30cb2a9bb8ac4e56ccd181d70aa208eff745717c", "92e0978bfdf2e6901df3d48a40580923fea4ae61"),
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
    for token in (
        'gcloud auth configure-docker "$REGISTRY_HOST"',
        'docker build --pull --no-cache',
        'docker push "$GBW_TAGGED_IMAGE"',
        'gcloud artifacts docker images describe',
    ):
        if token not in deploy:
            raise SystemExit(f"U12au direct publish guard failed: {token}")
    if "gcloud builds submit" in deploy:
        raise SystemExit("U12au must not use Cloud Build for RC20 deploy")
    run("bash", "-n", "cloud/remote-separation/scripts/deploy-worker.sh")

if not PATCH.is_file():
    raise SystemExit("Missing U12au direct Artifact Registry publish patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12au patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12au direct Artifact Registry publish")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12at materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12au baseline blob mismatch after U12at")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12au terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12au direct Artifact Registry publish")
