#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bi.py"
PATCH = ROOT / ".source-parts/U12bjW5BundleNamespace.patch"
PATCH_BLOB = "0a6333316658318b23b00b9de59235e7aea0efcf"
TARGETS = {
    "cloud/remote-separation/worker/benchmark.py": (
        "1ed2c871e705a764d4168653b7b22a112c928c94",
        "ad7d20e20899dfeaab340b42a5ac9545be90d96c",
    ),
    "cloud/remote-separation/worker/tests/test_benchmark.py": (
        "dd76c816381d15916a393f5d2d2690461dd01f56",
        "736da413eedd5ca9f72ef3022b77ad4a39418925",
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
    benchmark = (ROOT / "cloud/remote-separation/worker/benchmark.py").read_text(encoding="utf-8")
    tests = (ROOT / "cloud/remote-separation/worker/tests/test_benchmark.py").read_text(encoding="utf-8")
    for token in (
        'BUNDLE_PREFIX_ROOTS = (',
        '"diagnostics/u12-rc20/w4/",',
        '"diagnostics/u12-rc20/w5/",',
        'normalized != prefix',
        'any(part in {"", ".", ".."} for part in parts)',
        'bundle prefix must stay inside an authorized RC20 diagnostic namespace',
    ):
        if token not in benchmark:
            raise SystemExit(f"U12bj W5 bundle-namespace guard failed: {token}")
    for token in (
        'benchmark.validate_bundle_prefix("diagnostics/u12-rc20/w4/run-1")',
        'benchmark.validate_bundle_prefix("diagnostics/u12-rc20/w5/run-1/bundle")',
        '"diagnostics/u12-rc20/w5/../escape"',
        '"diagnostics/u12-rc20//w5/run-1"',
        '"remote/v1/users/u/jobs/job"',
    ):
        if token not in tests:
            raise SystemExit(f"U12bj W5 test guard failed: {token}")
    run(
        "python3",
        "-m",
        "py_compile",
        str(ROOT / "cloud/remote-separation/worker/benchmark.py"),
        str(ROOT / "cloud/remote-separation/worker/tests/test_benchmark.py"),
    )


if not PATCH.is_file():
    raise SystemExit("Missing U12bj W5 bundle-namespace patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bj patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bj W5 bundle namespace")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bi materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bj baseline blob mismatch after U12bi")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bj terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bj W5 bundle namespace")
