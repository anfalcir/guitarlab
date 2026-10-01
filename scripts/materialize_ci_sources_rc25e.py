#!/usr/bin/env python3
"""Hash-locked RC25 viewport-aware navbar regression; earlier payloads immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc25d.py"
PATCH = ROOT / ".source-parts/RC25NavbarViewportContract.patch"
PATCH_BLOB = 'fe56e3afa066ffefd3ea0d75fea89e9853064787'
TARGETS = {'app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt': ('ffff23e159cc65dded6021366017f9dcfec314c6', 'a0dba4ccf2837899582fe21f52c974a003e44926'), 'app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt': ('e574d6fc7956108a17cafe8a7fdb087fd4df9e49', '6b6e6d7c145b3ab18eefb159da46a782df6536d8'), 'app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt': ('c2e5822f0ac58a1ccf3a5f0e60d5f1638ee507e8', '29308b7e6cb511acee73db937d7a0028e54d5362')}

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
    print("Source chain already materialized through RC25 navbar viewport correction")
    raise SystemExit(0)
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC25 baseline blob mismatch after RC25 slider interaction")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC25 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC25 navbar viewport correction")
