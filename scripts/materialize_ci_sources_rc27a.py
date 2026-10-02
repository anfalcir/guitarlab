#!/usr/bin/env python3
"""RC27a: make target-tablet visual evidence wait for the actual loaded Studio frame."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc27.py"
PATCH = ROOT / ".source-parts/RC27aVisualEvidenceSync.patch"
PATCH_BLOB = "c1cb98dc4de2d93a6fda0c4e4e80aa2a890037d8"
TARGETS = {
    "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt": ("ac266d5e34c89941c7dc5f3356e6bec745061569", "c48e0cad6d756475734aaf496e415631474042ae"),
    "docs/CURRENT_STATE.md": ("3c1e83b0775646bbce5361071289314f88396294", "087886912096b1019f82aa3142f8aba91f185c6f"),
    "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md": ("4b003cc2fd22d128fe2ce2d99968666633332388", "3bf18775d8d8cb625a7bbfcc97a4d6b2c7d26c39"),
    "docs/CI_PIPELINE.md": ("5682df0b0c0fd570aeae77e44cf553f40668c7e1", "2e095be65346e4c7442ab1f7d3b3e155f811dd30"),
    "README.md": ("dfa97f63b95c49c58a5ef827beba4b8df2809e09", "f1334adcc2bde915c23955ea04377300bca88120"),
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
    test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    history = (ROOT / "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md").read_text()
    ci = (ROOT / "docs/CI_PIPELINE.md").read_text()
    readme = (ROOT / "README.md").read_text()
    for token in [
        "!studio.state.value.loading",
        'onNodeWithTag("mixer-master-strip").assertIsDisplayed()',
        'onNodeWithTag("track-waveform-area-${project.tracks[2].id}").assertIsDisplayed()',
        "InstrumentationRegistry.getInstrumentation().waitForIdleSync()",
        'captureCohesionScreenshot("rc27-studio-five-audio-channels-complete")',
    ]:
        if token not in test:
            raise SystemExit(f"RC27a evidence-sync guard failed: {token}")
    if "CI #952 / run `37034736534`" not in current or "RC27a next action" not in current:
        raise SystemExit("RC27a current-state evidence guard failed")
    if "Qualification attempt #952" not in history or "loading spinner" not in history:
        raise SystemExit("RC27a history evidence guard failed")
    if "current candidate tail: RC27a visual-evidence synchronization" not in ci:
        raise SystemExit("RC27a CI-tail guard failed")
    if "RC27aVisualEvidenceSync" not in readme:
        raise SystemExit("RC27a README-tail guard failed")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC27a patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC27a visual-evidence synchronization")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC27a baseline blob mismatch after RC27")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC27a terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC27a visual-evidence synchronization")
