#!/usr/bin/env python3
"""Hash-locked RC23 stage; prior RC22 payload is immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc22.py"
PATCH = ROOT / ".source-parts/RC23StudioSpaceLayout.patch"
PATCH_BLOB = 'd16b0307e941d2cab52ceee61aaba29562451fbc'
TARGETS = {'app/build.gradle.kts': ('5e3343d00d2259fca4f57dadcbb9652355398fba', '5b8556b8f21d85bf749816bd843cf1948b680c4a'), 'app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt': ('0debce09c4377c7d4c39ca950dad332816161430', 'a52eb8ea3903dffb783c90756dd5674737eeb6b1'), 'app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt': ('6caa17e56c450dd2dcbd519709eb194e41a05db8', 'e00b6841a3a17c33b357e7b9642fa81df6a4f5c9'), 'app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt': ('40ec9b20efcf86325ff09e1c5cc2b8871e1a8833', '0584e90a91fa2eb91215e2395922f753903cc96c'), 'app/src/main/java/studio/guitarlab/app/ui/StudioUiPreferencesStore.kt': ('9938efbd0a67aafa406da243e72159361ceffe5e', 'a21f2002fe77b9f899a8a992862d3f10aa4ab389'), 'app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt': ('99381a8819068f887799e305d32a0b16298daf80', '3629b356a520d0a8b2661b6a2cf6cf60362e7e22'), 'app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt': (None, '8b18a644e0587da5bba8dc9d6c3697ef05ca1a5d'), 'app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt': ('142bc737e453802b7acd0b4d66ff5184d7cf4768', 'c4d06e62c07bdbeba0c69b593e3e21bc6f393eaf'), 'app/src/androidTest/java/studio/guitarlab/app/PracticeWorkflowInstrumentedTest.kt': ('829268a1a14ec696bfd75b628263172af831f859', '867caad652cc2dc4f9d8472313a9a847ab07d455'), 'app/src/androidTest/java/studio/guitarlab/app/StudioUiPreferencesStoreInstrumentedTest.kt': ('c8c5bdccafffa669ea5ba99d3965f8a82a96d4c7', '811d129fd360140a04e48a8c418c5bd33eb89b6e'), 'app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt': (None, 'fb67c7100bd50da406cfe27e5a93b432bd208f5b'), 'app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt': (None, '4bb1812d1646dce0dec5fc5579864402bf11badc'), 'scripts/ci_run_api36_regression_groups.sh': ('9fd83e5a8124b42fa2febe5376748c3bd09cc5ea', '90a8fd548ec179fcb3f7732cb119116cb31aaf5b')}

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
        "app/build.gradle.kts": ['versionName = "0.5.0-rc23"', "versionCode = 43"],
        "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ["StudioWorkspaceBar(", "showPracticeControls = false", "if (mixerPinned) renderMixer()", "uiPreferences.setMixerPinned(mixerPinned)", "onToggleCue = viewModel::toggleTrackCue"],
        "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": [".width(168.dp", ".size(48.dp)", "MeterPair(", "Icons.Default.Headphones", "onValueChangeFinished = { onPanCommit(panDraft) }"],
        "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt": ["DropdownMenu(", "horizontalScroll(rememberScrollState())", 'stateDescription = if (mixerPinned) "Fixado" else "Flutuante"'],
        "scripts/ci_run_api36_regression_groups.sh": ["StudioWorkspaceBarInstrumentedTest", "StudioMixerTabletInstrumentedTest"],
    }
    for rel, tokens in checks.items():
        content = (ROOT / rel).read_text(encoding="utf-8")
        for token in tokens:
            if token not in content:
                raise SystemExit(f"RC23 semantic guard failed: {rel}: {token}")
    if "headerContent =" in (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt").read_text():
        raise SystemExit("RC23 stale Mixer practice header")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC23 patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC23 Studio layout")
    raise SystemExit(0)
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC23 baseline blob mismatch after RC22")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC23 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC23 Studio layout")
