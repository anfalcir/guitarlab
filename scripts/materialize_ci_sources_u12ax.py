#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12aw.py"
PATCH = ROOT / ".source-parts/U12axWorkerRuntimeHardening.patch"
PATCH_BLOB = "779b31770c47c25060369f9351a8aeee71db0c5e"
TARGETS = {
    "cloud/remote-separation/worker/Dockerfile": ("eebea762fec7cc4c0d953b7856dcd50730279d0b", "72739f555ed4359d10b356af9b633b9ccb772f0c"),
    "cloud/remote-separation/worker/demucs_engine.py": ("512a281ca87911d73a0e72382233e17360532b39", "c9cb0d3d1a8e6b7e10232a5bb9f7086246543b38"),
    "cloud/remote-separation/worker/tests/test_demucs_engine.py": ("59ff03380c3819e8071f56a7adfd915757ee5407", "edef5dc4a93b6682004a6efe566e40a1c1b0a413"),
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
    dockerfile = (ROOT / "cloud/remote-separation/worker/Dockerfile").read_text(encoding="utf-8")
    engine = (ROOT / "cloud/remote-separation/worker/demucs_engine.py").read_text(encoding="utf-8")
    for token in (
        "AS builder",
        "AS runtime",
        "COPY --from=builder /opt/venv /opt/venv",
        "python-resolved.txt",
        'ENTRYPOINT ["/opt/venv/bin/python3", "/app/gbw_worker.py"]',
    ):
        if token not in dockerfile:
            raise SystemExit(f"U12ax runtime hardening guard failed: {token}")
    if "validate_runtime_packages" not in engine or 'NUMPY_VERSION = "1.26.4"' not in engine:
        raise SystemExit("U12ax runtime package guard failed")
    run("python3", "-m", "unittest", "cloud.remote-separation.worker.tests.test_demucs_engine")

if not PATCH.is_file():
    raise SystemExit("Missing U12ax worker runtime hardening patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12ax patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12ax runtime hardening")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12aw materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12ax baseline blob mismatch after U12aw")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12ax terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12ax runtime hardening")
