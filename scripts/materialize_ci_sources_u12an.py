#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12am.py"
PATCH = ROOT / ".source-parts/U12anSameGenerationPriority.patch"
PATCH_BLOB = "aec992ca4c2e9e32f270a4830b46122f91c31ec7"
TARGETS = {
    "cloud/remote-separation/functions/src/policy.ts": (
        "d1140ef30fbaba7f111e7bf5f69161a0c35d93d1",
        "04a7dc673c669e8fb23f716b77a8dbe233b38683",
    ),
    "cloud/remote-separation/functions/src/test/policy.test.ts": (
        "8b344471ea30051da63fb73039b775be459c4f03",
        "ea9f55ae164292a6bf1a11ef961a16a4ed98c428",
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


def verify() -> None:
    run("git", "diff", "--check")
    policy = (ROOT / "cloud/remote-separation/functions/src/policy.ts").read_text(encoding="utf-8")
    tests = (ROOT / "cloud/remote-separation/functions/src/test/policy.test.ts").read_text(encoding="utf-8")
    if "if (recoverable.length === 1)" not in policy:
        raise SystemExit("U12an policy semantic guard failed")
    if "same generation adoption wins safely even when unrelated active work is present" not in tests:
        raise SystemExit("U12an regression guard failed")


if not PATCH.is_file():
    raise SystemExit("Missing U12an same-generation priority patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12an patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12an same-generation priority")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12am materializer")
run("python3", str(PREVIOUS))
for rel, (before, _) in TARGETS.items():
    target = ROOT / rel
    if not target.is_file() or blob(target) != before:
        raise SystemExit(f"U12an before-blob mismatch: {rel}")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12an terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12an same-generation priority")
