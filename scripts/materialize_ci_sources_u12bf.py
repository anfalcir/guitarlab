#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12be.py"
PATCH = ROOT / ".source-parts/U12bfTrixieRuntimeBase.patch"
PATCH_BLOB = "49055f844d7e983ac8d755197c9e05831e1619d2"
TARGETS = {
    "cloud/remote-separation/worker/Dockerfile": (
        "a10e90ac830f7f0b624a1e88a7b4e688c1fdb6e6",
        "6794cc425f4ff38e23d96e03a49215bb18b4cca9",
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
    dockerfile = (ROOT / "cloud/remote-separation/worker/Dockerfile").read_text(encoding="utf-8")
    expected = (
        "python:3.12.14-slim-trixie@sha256:"
        "2f17fc044b579bab302c2e8054d3a686e2cb9a83de48e70534b94cd8ebbe06a9"
    )
    if expected not in dockerfile or "slim-bookworm" in dockerfile:
        raise SystemExit("U12bf pinned Trixie runtime-base guard failed")


if not PATCH.is_file():
    raise SystemExit("Missing U12bf Trixie runtime-base patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bf patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bf Trixie runtime base")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12be materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bf baseline blob mismatch after U12be")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bf terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bf Trixie runtime base")
