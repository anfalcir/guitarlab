#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bg.py"
PATCH = ROOT / ".source-parts/U12bhStochasticParityEvidence.patch"
PATCH_BLOB = "562a94f65e2f341a759400d2a2586e76e0cfb6a6"
TARGETS = {
    "scripts/u12_rc20_parity.sh": (
        "437793c147e46708b1d52c565850ef9925e4cdf2",
        "a4039fde2eef4593b4b10f54ccbaf60a28c7a2c2",
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
    parity = (ROOT / "scripts/u12_rc20_parity.sh").read_text(encoding="utf-8")
    for token in (
        '"equivalencePolicy"',
        '"metricDeltas"',
        '"previousEnvelopeMisses"',
        '"failures"',
        "if failures:",
        "numeric deltas are diagnostic because Demucs shifts are random",
    ):
        if token not in parity:
            raise SystemExit(f"U12bh stochastic-parity guard failed: {token}")
    run("bash", "-n", str(ROOT / "scripts/u12_rc20_parity.sh"))


if not PATCH.is_file():
    raise SystemExit("Missing U12bh stochastic-parity patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bh patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bh stochastic parity evidence")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bg materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bh baseline blob mismatch after U12bg")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bh terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bh stochastic parity evidence")
