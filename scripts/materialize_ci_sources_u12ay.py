#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ax.py"
PATCH = ROOT / ".source-parts/U12aySupplyChainEvidence.patch"
PATCH_BLOB = "e57d7fa5f6301d3ad43708129d61f35614d9f3f8"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": ("cbbaa9ddf4da2ec35f09c478be3075b90e8cdaf9", "12193d34846906b16aa4657196f11c7130017edf"),
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
        "Generate RC20 CycloneDX SBOM",
        "Scan RC20 high and critical vulnerabilities",
        "Enforce RC20 critical-vulnerability gate",
        "Upload RC20 supply-chain evidence",
        "aquasecurity/trivy-action@ed142fd0673e97e23eac54620cfb913e5ce36c25",
        "/opt/rc20/python-resolved.txt",
    ):
        if token not in workflow:
            raise SystemExit(f"U12ay supply-chain guard failed: {token}")
    if workflow.count("if: steps.deploy_mode.outputs.mode == 'shadow'") < 5:
        raise SystemExit("U12ay supply-chain steps must remain shadow-only before W6")
    
if not PATCH.is_file():
    raise SystemExit("Missing U12ay supply-chain evidence patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12ay patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12ay supply-chain evidence")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12ax materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12ay baseline blob mismatch after U12ax")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12ay terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12ay supply-chain evidence")
