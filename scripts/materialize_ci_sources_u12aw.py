#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12av.py"
PATCH = ROOT / ".source-parts/U12awW3LocalCloudParity.patch"
PATCH_BLOB = "0e20748c5d0a3ef9c8155f956c2567af591bf7ab"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": ("cd3232486a216f757b8d9804e86880b0ef033ec0", "cbbaa9ddf4da2ec35f09c478be3075b90e8cdaf9"),
    "scripts/u12_rc20_local_parity.py": (None, "e3d9a5365a52778765c31f44326b7c05552b126d"),
    "scripts/u12_rc20_parity.sh": (None, "80649650422f3be64972ebd9dc1189fc7ddeb06f"),
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
    workflow = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(encoding="utf-8")
    runner = (ROOT / "scripts/u12_rc20_parity.sh").read_text(encoding="utf-8")
    driver = (ROOT / "scripts/u12_rc20_local_parity.py").read_text(encoding="utf-8")
    for token in (
        "Run W3 local/container versus Cloud Run parity",
        "Upload W3 parity evidence",
        "scripts/u12_rc20_parity.sh",
    ):
        if token not in workflow:
            raise SystemExit(f"U12aw workflow guard failed: {token}")
    for token in (
        'docker run --rm',
        'gcloud run jobs deploy "$PARITY_JOB"',
        'metricRelativeTolerance',
        'stemHashEquality',
        'preparedHashEquality',
    ):
        if token not in runner:
            raise SystemExit(f"U12aw parity runner guard failed: {token}")
    if 'sys.path.insert(0, "/app")' not in driver or "DemucsPyTorchRunner" not in driver:
        raise SystemExit("U12aw local parity driver guard failed")
    run("bash", "-n", "scripts/u12_rc20_parity.sh")
    run("python3", "-m", "py_compile", "scripts/u12_rc20_local_parity.py")

if not PATCH.is_file():
    raise SystemExit("Missing U12aw W3 parity patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12aw patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12aw W3 parity")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12av materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12aw baseline blob mismatch after U12av")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12aw terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12aw W3 parity")
