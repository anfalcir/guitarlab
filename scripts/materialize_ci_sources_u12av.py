#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12au.py"
PATCH = ROOT / ".source-parts/U12avW4BundleHierarchy.patch"
PATCH_BLOB = "6c117947fdb47b444f334038e4e48a031b258c4c"
TARGETS = {
    "scripts/u12_rc20_shadow_benchmark.sh": ("0465111975a18b5430156ba49f82d4e527d5a5d7", "ac4f8ed08734b9f23c669fbaf0375686652bd3a7"),
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
    runner = (ROOT / "scripts/u12_rc20_shadow_benchmark.sh").read_text(encoding="utf-8")
    for token in (
        'mkdir -p "$ARTIFACT_DIR/stems" "$ARTIFACT_DIR/prepared"',
        '"$ARTIFACT_DIR/stems/"',
        '"$ARTIFACT_DIR/prepared/"',
        'METRICS="$ARTIFACT_DIR/benchmark.json"',
        'SUMS="$ARTIFACT_DIR/SHA256SUMS"',
    ):
        if token not in runner:
            raise SystemExit(f"U12av W4 bundle hierarchy guard failed: {token}")
    if "gcloud storage cp --recursive" in runner:
        raise SystemExit("U12av must not flatten the W4 bundle")
    run("bash", "-n", "scripts/u12_rc20_shadow_benchmark.sh")

if not PATCH.is_file():
    raise SystemExit("Missing U12av W4 bundle hierarchy patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12av patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12av W4 bundle hierarchy")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12au materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12av baseline blob mismatch after U12au")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12av terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12av W4 bundle hierarchy")
