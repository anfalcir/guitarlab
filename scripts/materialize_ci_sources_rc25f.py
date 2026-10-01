#!/usr/bin/env python3
"""Hash-locked RC25 separate scroll viewport/content layouts; prior payloads immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc25e.py"
PATCH = ROOT / ".source-parts/RC25NavbarScrollLayoutNodes.patch"
PATCH_BLOB = '6d25c4c3bb815b2febb99840364dc3acc2a1a49d'
TARGETS = {'app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt': ('a0dba4ccf2837899582fe21f52c974a003e44926', 'fabdef6dc0931cae81f7ccc73dad0a447ec0276c'), 'app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt': ('6b6e6d7c145b3ab18eefb159da46a782df6536d8', 'f51ea548bfeacade9b25fea73d3fdbeb515e8060')}

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
        "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt": ['testTag("studio-workspace-viewport")', 'Modifier.width(viewportWidth.coerceAtLeast(664.dp)).testTag("studio-workspace-row")'],
        "app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt": ["fun contentBounds", "centered in the fixed content row"],
        "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt": ["Tablet transport must be centered on the viewport"],
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
    print("Source chain already materialized through RC25 navbar scroll-node correction")
    raise SystemExit(0)
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC25 baseline blob mismatch after RC25 navbar viewport")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC25 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC25 navbar scroll-node correction")
