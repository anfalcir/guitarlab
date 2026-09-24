#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12as.py"
PATCH = ROOT / ".source-parts/U12atExplicitCloudBuildDocker.patch"
PATCH_BLOB = "21bad1d504f28f567e5b0dcaf7971003e6f736e7"
TARGETS = {
    "cloud/remote-separation/scripts/deploy-worker.sh": ("e968f381bf671d415b944ea48605f391d2f063b9", "30cb2a9bb8ac4e56ccd181d70aa208eff745717c"),
    "cloud/remote-separation/worker/cloudbuild.yaml": (None, "4d0299fffe5829f4459a8f29cacc2649e2a2ee0d"),
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
    for rel, (before, _) in TARGETS.items():
        target = ROOT / rel
        if before is None:
            if target.exists():
                return False
        elif not target.is_file() or blob(target) != before:
            return False
    return True

def verify() -> None:
    run("git", "diff", "--check")
    deploy = (ROOT / "cloud/remote-separation/scripts/deploy-worker.sh").read_text(encoding="utf-8")
    cloudbuild = (ROOT / "cloud/remote-separation/worker/cloudbuild.yaml").read_text(encoding="utf-8")
    if 'CLOUD_BUILD_CONFIG="cloud/remote-separation/worker/cloudbuild.yaml"' not in deploy:
        raise SystemExit("U12at explicit Cloud Build config guard failed")
    if '--config "$CLOUD_BUILD_CONFIG"' not in deploy:
        raise SystemExit("U12at Cloud Build config invocation guard failed")
    if "gcr.io/cloud-builders/docker" not in cloudbuild or "--no-cache" not in cloudbuild:
        raise SystemExit("U12at Docker builder guard failed")
    run("bash", "-n", "cloud/remote-separation/scripts/deploy-worker.sh")

if not PATCH.is_file():
    raise SystemExit("Missing U12at explicit Cloud Build Docker patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12at patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12at explicit Cloud Build Docker")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12as materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12at baseline blob mismatch after U12as")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12at terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12at explicit Cloud Build Docker")
