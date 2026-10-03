#!/usr/bin/env python3
"""RC27d: seal the truthful post-sign RC27 documentation delta without rewriting RC27c."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc27c.py"
PATCH = ROOT / ".source-parts/RC27dSignedDocumentationClosure.patch"
PATCH_BLOB = "b72ec28c8daa6604286b4e45f04937d210870136"
TARGETS = {
    "README.md": ("6abaf009ebd563c0ad859d3470255bc4945decc0", "9793e5fe41cae27621fb57d9afbbeea6b5b8a4f6"),
    "docs/CI_PIPELINE.md": ("71d0f5ac6a8355e6f050b8389ac9d197f1f76aea", "75a0bec445a5f6cf8d8371b781d43454e3d8218c"),
    "docs/CURRENT_STATE.md": ("fb3de221070d085ddd8c0a777d348fe3b324633a", "68afa473e1f9855a834c2ba736b9af88714026eb"),
    "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md": ("58c48473ff6769802ecb00edd0d7b3e82e18236d", "b761e932c33bf6b34505d418f24ad96e836e4bd6"),
}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    if "RC27 MIXER CARD HIERARCHY — SIGNED DIGITAL PASS" not in current:
        raise SystemExit("RC27d signed-authority guard failed")
    if "37047225250" not in current:
        raise SystemExit("RC27d signed-run guard failed")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC27d patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC27d signed documentation closure")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC27d baseline blob mismatch after RC27c")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC27d terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC27d signed documentation closure")
