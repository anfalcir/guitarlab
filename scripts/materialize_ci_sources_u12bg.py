#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bf.py"
PATCH = ROOT / ".source-parts/U12bgVendorSeverityGate.patch"
PATCH_BLOB = "aebc508906114bbb1db440aa5df9ede5c70b4f08"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": (
        "4987ff605e4d612dcca5cf74602dcc93caa281f1",
        "d331a3b4c57ea2ca9f5d907330aa0d44395020aa",
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
        'vulnerability.get("VulnerabilityID") == "CVE-2026-6653"',
        'vulnerability.get("PkgName") == "libxml2"',
        'vulnerability.get("SeveritySource") == "nvd"',
        'vendor_severity.get("redhat") == 2',
        'vendor_severity.get("ubuntu") == 2',
        "accepted_critical_reason(v)",
        "if blocking_critical:",
    ):
        if token not in workflow:
            raise SystemExit(f"U12bg vendor-severity guard failed: {token}")


if not PATCH.is_file():
    raise SystemExit("Missing U12bg vendor-severity patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bg patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bg vendor-severity gate")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bf materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bg baseline blob mismatch after U12bf")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bg terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bg vendor-severity gate")
