#!/usr/bin/env python3
"""Hash-locked RC25 compact Slider interaction correction; earlier payloads immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc25c.py"
PATCH = ROOT / ".source-parts/RC25SliderInteractiveBounds.patch"
PATCH_BLOB = '2211864111cf424dd483834753090905b2024cd9'
TARGETS = {'app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt': ('1200cd67b885792aa71f54c8f6ac8706beb0e07c', '1ef6e8e5660fdfc95d6f73eebf5402050cba3f84'), 'app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt': ('de076f254e4a94b8c28d749be5c28390c98673bb', '97a3992482d1fa6b4cc410fa62f383d29d881635')}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all(not (ROOT / p).exists() if before is None else (ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    checks = {
        "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ["height(48.dp).padding(horizontal = 10.dp)", "size(width = 6.dp, height = 48.dp)"],
        "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": ["scrollMixerControlIntoView(control, action)", "repeat(12)", "scrolling made no progress", "unclippedBounds", "semantics.positionInRoot", "touchBoundsInRoot", "gainPreviews.get() > 0", "gainCommits.get() > 0", "MixerGeometryTest"],
        "scripts/ci_run_api36_regression_groups.sh": ['if [[ "$status" -eq 0 ]]; then status=1; fi'],
    }
    for rel, tokens in checks.items():
        content = (ROOT / rel).read_text()
        for token in tokens:
            if token not in content:
                raise SystemExit(f"RC25 semantic guard failed: {rel}: {token}")
    shell = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt").read_text()
    bar = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt").read_text()
    if "mixerPinned" in shell or "onToggleMixerPin" in bar or "ADJUSTMENTS" in bar:
        raise SystemExit("RC25 obsolete floating/pin/adjustments state")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC25 patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC25 slider interaction correction")
    raise SystemExit(0)
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC25 baseline blob mismatch after RC25 semantic bounds")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC25 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC25 slider interaction correction")
