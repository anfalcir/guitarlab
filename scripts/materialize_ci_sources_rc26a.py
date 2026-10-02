#!/usr/bin/env python3
"""RC26a: remove CI-only lint/semantics blockers without changing approved Studio geometry."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc26.py"
PATCH = ROOT / ".source-parts/RC26aMixerLevelsCiFix.patch"
PATCH_BLOB = "ea50f61551a50ddbb4ad5ef3f652fcf6f1042623"
TARGETS = {
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ("96d714d5f4c62d96bb5b6d22b58e105cbf92d7a2", "17b78eef520025601073603c5493fefc1653bb58"),
    "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": ("72902c74310dbf1b5169a00ff6535d2b03b3a46b", "13e6310baff60217579b75f11c2fc70a481239c2"),
    "docs/CURRENT_STATE.md": ("65168515b58da57c0020255a3b5e2077b836d44a", "6f6b4dca2cf5b0cb05f8307d5a69752ed21239d9"),
    "docs/history/RC26_MIXER_LEVELS_UI_2026-10-01.md": ("bfd181d052231d992255fae889ebe4984560636f", "c5de787ddec06680316f5db0dc083b86d73914fc"),
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
    test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    history = (ROOT / "docs/history/RC26_MIXER_LEVELS_UI_2026-10-01.md").read_text()
    if "BoxWithConstraints(\n                    Modifier.fillMaxWidth().height(4.dp)" in mixer:
        raise SystemExit("RC26a lint guard failed: Master slider still uses unused BoxWithConstraints")
    if "track = {\n                Box(\n                    Modifier.fillMaxWidth().height(4.dp)" not in mixer:
        raise SystemExit("RC26a Master slider track guard failed")
    for token in [
        'onNodeWithTag("mixer-track-meters-${track.value.id}", useUnmergedTree = true)',
        'onNodeWithTag("mixer-master-meters", useUnmergedTree = true)',
        'onNodeWithTag("mixer-master-volume", useUnmergedTree = true)',
        'onNodeWithTag("mixer-master-volume-readout", useUnmergedTree = true)',
    ]:
        if token not in test:
            raise SystemExit(f"RC26a semantics guard failed: {token}")
    if "scripts/materialize_ci_sources_rc26a.py" not in current:
        raise SystemExit("RC26a current-state tail not recorded")
    if "CI #949" not in history or "useUnmergedTree = true" not in history:
        raise SystemExit("RC26a qualification evidence not recorded")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC26a patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC26a CI correction")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC26a baseline blob mismatch after RC26")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC26a terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC26a CI correction")
