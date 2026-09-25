#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bl.py"
PATCH = ROOT / ".source-parts/U12bmExactDigestPromotion.patch"
PATCH_BLOB = "a6aaef7153e5735eefd8d03dc22c7d91231d3b68"
TARGETS = {
    "cloud/remote-separation/scripts/deploy-worker.sh": (
        "92e0978bfdf2e6901df3d48a40580923fea4ae61",
        "bdcb93908758c3e805470cd2a1cf1f280fcb1a8d",
    ),
    ".github/workflows/u7-cloud-backend.yml": (
        "0c7337422142517f55756d83247aa6fea5e64b32",
        "63539cbc0b783864b2eb397f214c774f1d5ba2c8",
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
    deploy = (ROOT / "cloud/remote-separation/scripts/deploy-worker.sh").read_text(encoding="utf-8")
    workflow = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(encoding="utf-8")
    digest = "sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698"
    for token in (
        'GBW_PREQUALIFIED_IMAGE="${GBW_PREQUALIFIED_IMAGE:-}"',
        "Promoting already-qualified immutable worker digest without rebuild",
        "^sha256:[0-9a-f]{64}$",
    ):
        if token not in deploy:
            raise SystemExit(f"U12bm exact-promotion guard failed: {token}")
    if digest not in workflow:
        raise SystemExit("U12bm workflow does not pin the U7 #164 qualified digest")
    if "steps.deploy_mode.outputs.mode == 'production'" not in workflow:
        raise SystemExit("U12bm production-only prequalified digest guard missing")
    run("bash", "-n", str(ROOT / "cloud/remote-separation/scripts/deploy-worker.sh"))


if not PATCH.is_file():
    raise SystemExit("Missing U12bm exact-digest promotion patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bm patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bm exact digest promotion")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bl materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bm baseline blob mismatch after U12bl")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bm terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bm exact digest promotion")
