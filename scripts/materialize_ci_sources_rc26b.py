#!/usr/bin/env python3
"""RC26b: seal the post-sign documentation closure without changing RC26 runtime bytes."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc26a.py"
PATCH = ROOT / ".source-parts/RC26bSignedDocumentationClosure.patch"
PATCH_BLOB = "ffc100d1fce54af108ed75c6a14fd6145849b80b"
TARGETS = {
    "docs/CURRENT_STATE.md": ("6f6b4dca2cf5b0cb05f8307d5a69752ed21239d9", "830552afdcedbe32eadcd1e46562828dc6efce5f"),
    "README.md": ("80682db1015e5ad1e2a9e2b0af5dc17730f47f89", "586627bbd21207d9fd028c38e5cccc5d613013bf"),
    "docs/history/RC26_MIXER_LEVELS_UI_2026-10-01.md": ("c5de787ddec06680316f5db0dc083b86d73914fc", "3fe5fa0c602ac10dad1f9f05afac858fda88c4c6"),
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
    history = (ROOT / "docs/history/RC26_MIXER_LEVELS_UI_2026-10-01.md").read_text()
    readme = (ROOT / "README.md").read_text()
    if "SIGNED DIGITAL PASS" not in current or "Exact signed RC26 identity" not in current:
        raise SystemExit("RC26b current-state closure guard failed")
    if "Final digital authority" not in history:
        raise SystemExit("RC26b history closure guard failed")
    if "RC26 SIGNED DIGITAL" not in readme:
        raise SystemExit("RC26b README closure guard failed")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC26b patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC26b signed-documentation closure")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC26b baseline blob mismatch after RC26a")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC26b terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC26b signed-documentation closure")
