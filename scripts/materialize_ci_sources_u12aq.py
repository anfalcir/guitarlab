#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ap.py"
PATCH = ROOT / ".source-parts/U12aqW4ShadowBenchmark.patch"
PATCH_BLOB = "b93bbcd514f4d06e3f4ed57c4a644b96b45db8b6"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": ("e0789d3e80003b0ee45a79e2e4de8ad5c6d0e2b2", "7e1384e2f3c17f32576645e720b830aa208751cf"),
    ".gitignore": ("9ec07203e40d5b5e8cfc704446029fa266bb077e", "1568917b5c09401d6f6acb878346ea22b1f24c75"),
    "cloud/remote-separation/worker/benchmark.py": ("b4dd364c61e292fdb55f70e8d7918dcbb34800bb", "f67e079cabd19fef59e9f708252ec781271fef56"),
    "cloud/remote-separation/worker/tests/test_benchmark.py": ("f249f6dc812bd83fd63a98b47b2df7b52d61b593", "8698df5747a22b56ce2276ce4f4501eb1ef13669"),
    "scripts/u12_generate_benchmark_fixture.py": (None, "bc0db290de84654e5eb8e53f1276ab5245f90e89"),
    "scripts/u12_rc20_shadow_benchmark.sh": (None, "0465111975a18b5430156ba49f82d4e527d5a5d7"),
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
    benchmark = (ROOT / "cloud/remote-separation/worker/benchmark.py").read_text(encoding="utf-8")
    workflow = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(encoding="utf-8")
    runner = (ROOT / "scripts/u12_rc20_shadow_benchmark.sh").read_text(encoding="utf-8")
    fixture = (ROOT / "scripts/u12_generate_benchmark_fixture.py").read_text(encoding="utf-8")
    if 'BUNDLE_PREFIX_ROOT = "diagnostics/u12-rc20/w4/"' not in benchmark:
        raise SystemExit("U12aq W4 bundle namespace guard failed")
    if "upload_listening_bundle" not in benchmark or '"bundleFiles"' not in benchmark:
        raise SystemExit("U12aq W4 bundle contract guard failed")
    if "Run W4 representative shadow benchmark" not in workflow:
        raise SystemExit("U12aq W4 workflow guard failed")
    if "owner_listening=REQUIRED_NOT_YET_PASSED" not in runner:
        raise SystemExit("U12aq human-listening gate guard failed")
    if "fixture_duration_seconds=180" not in runner:
        raise SystemExit("U12aq representative-duration guard failed")
    if "royalty-free technical W4 benchmark fixture" not in fixture:
        raise SystemExit("U12aq legal fixture guard failed")


if not PATCH.is_file():
    raise SystemExit("Missing U12aq W4 shadow benchmark patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12aq patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12aq W4 shadow benchmark")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12ap materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12aq baseline blob mismatch after U12ap")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12aq terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12aq W4 shadow benchmark")
