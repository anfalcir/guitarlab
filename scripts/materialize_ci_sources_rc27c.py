#!/usr/bin/env python3
"""RC27c: edge-anchor the compact clipping warning face inside its unchanged 48dp action target."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc27b.py"
PATCH = ROOT / ".source-parts/RC27cClipBadgeEdgeAnchor.patch"
PATCH_BLOB = "e0ecb83208526c32d3aa9b71178c6284aa6725b5"
TARGETS = {
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ("da134f013f7fced214081f31f32b402a1b2076f2", "dfd704f8d99551fb3ee7a566919fcb240ef6d09e"),
    "docs/UI_VISUAL_SYSTEM.md": ("07335d18a3600bf5788fcedf3f96b574517405a2", "eb4e87f3fa9daf4d33d6cac3e4c4127b958d80c4"),
    "docs/STUDIO_OPTIONS_AND_MIXER.md": ("984b1197451883d0b35431c8ad067cf00633c3c3", "f1296d9c7d2dcf0b07d7fd85746e082dcc20b496"),
    "docs/DECISIONS.md": ("7dbd7a43456b48e264f914dd319719a1cf5e0b5c", "53720dc1bd6b46ffdb520d2ae5f12bab10a190a2"),
    "docs/TEST_AND_HOMOLOGATION_POLICY.md": ("81ec78f4127964e5748cdcc670632c44ddc40c25", "198fb4238dd0aba579cf007a0e27dbd56c23ddf9"),
    "docs/CURRENT_STATE.md": ("65720a944e0b88e0facc38f56d2028df4f342150", "fb3de221070d085ddd8c0a777d348fe3b324633a"),
    "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md": ("7be552a8e568acde649034dd690a1b1c2938b4a3", "58c48473ff6769802ecb00edd0d7b3e82e18236d"),
    "docs/CI_PIPELINE.md": ("b14353b64694c84054336c8c57e2384f1beab8a2", "71d0f5ac6a8355e6f050b8389ac9d197f1f76aea"),
    "README.md": ("409b246c40864a96ba6d84aaa55dbc5109d442c2", "6abaf009ebd563c0ad859d3470255bc4945decc0"),
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
    mixer = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    decisions = (ROOT / "docs/DECISIONS.md").read_text()
    if "contentAlignment = Alignment.CenterEnd" not in mixer:
        raise SystemExit("RC27c edge-anchor runtime guard failed")
    if 'Modifier.size(width = 28.dp, height = 24.dp).testTag(indicatorTag)' not in mixer:
        raise SystemExit("RC27c compact-face size guard failed")
    if "D-105 — Visible clipping warning is edge-anchored" not in decisions:
        raise SystemExit("RC27c decision guard failed")
    if "scripts/materialize_ci_sources_rc27c.py" not in current:
        raise SystemExit("RC27c current-state tail guard failed")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC27c patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC27c CLIP badge edge anchoring")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC27c baseline blob mismatch after RC27b")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC27c terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC27c CLIP badge edge anchoring")
