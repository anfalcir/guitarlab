#!/usr/bin/env python3
"""Hash-locked RC24 UI correction. RC23 and prior payloads remain immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc23.py"
PATCH = ROOT / ".source-parts/RC24StudioDensity.patch"
PATCH_BLOB = '056299faf4fe9a6263c409a4ad597af0ca4d853f'
TARGETS = {'app/build.gradle.kts': ('5b8556b8f21d85bf749816bd843cf1948b680c4a', 'd32bed61a82e88438e339bbebdd57897baca62c1'), 'app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt': ('c4d06e62c07bdbeba0c69b593e3e21bc6f393eaf', '19407b5b7624d6f4072efda131993af43c3e31aa'), 'app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt': ('207b9384836e953adc6c7fe407cbc0ce96288a5d', 'c2e5822f0ac58a1ccf3a5f0e60d5f1638ee507e8'), 'app/src/androidTest/java/studio/guitarlab/app/StudioUiPreferencesStoreInstrumentedTest.kt': ('811d129fd360140a04e48a8c418c5bd33eb89b6e', '025ddce3bfe836c34a958c2d09391366063ebb5f'), 'app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt': ('fb67c7100bd50da406cfe27e5a93b432bd208f5b', 'e574d6fc7956108a17cafe8a7fdb087fd4df9e49'), 'app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt': ('a52eb8ea3903dffb783c90756dd5674737eeb6b1', '1200cd67b885792aa71f54c8f6ac8706beb0e07c'), 'app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt': ('e00b6841a3a17c33b357e7b9642fa81df6a4f5c9', 'ee7d86c04f3d7f00d5b885b9c497d550b35b49ff'), 'app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt': ('0584e90a91fa2eb91215e2395922f753903cc96c', '37aa90d2bbeeadfefff94ea5ad67a6260da40aba'), 'app/src/main/java/studio/guitarlab/app/ui/StudioUiPreferencesStore.kt': ('a21f2002fe77b9f899a8a992862d3f10aa4ab389', '8007d7ce01d2bd8f109d86fa25767768faa5557b'), 'app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt': ('3629b356a520d0a8b2661b6a2cf6cf60362e7e22', '6ba0066d7a120c97446d30bae34baa82e784619e'), 'app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt': ('8b18a644e0587da5bba8dc9d6c3697ef05ca1a5d', 'ffff23e159cc65dded6021366017f9dcfec314c6')}

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
    checks = {
        "app/build.gradle.kts": ['versionName = "0.5.0-rc24"', "versionCode = 44"],
        "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ["showPracticeControls = false", "minimal = mixerMinimal", "uiPreferences.setMixerMinimal(mixerMinimal)", "onToggleCue = viewModel::toggleTrackCue"],
        "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ["if (minimal) 172.dp else 252.dp", ".width(200.dp", ".size(46.dp)", "mixer-track-details", "DenseMixerSlider", "onValueChangeFinished = { onPanCommit(panDraft) }"],
        "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt": ["Modifier.width(104.dp)", "studio-mixer-mode", "DropdownMenu("],
        "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt": ["Defina e ative um loop primeiro", "DropdownMenuItem("],
        "scripts/ci_run_api36_regression_groups.sh": ["StudioWorkspaceBarInstrumentedTest", "StudioMixerTabletInstrumentedTest"],
    }
    for rel, tokens in checks.items():
        content = (ROOT / rel).read_text()
        for token in tokens:
            if token not in content:
                raise SystemExit(f"RC24 semantic guard failed: {rel}: {token}")
    shell = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt").read_text()
    bar = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt").read_text()
    if "mixerPinned" in shell or "onToggleMixerPin" in bar or "ADJUSTMENTS" in bar:
        raise SystemExit("RC24 obsolete floating/pin/adjustments state")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC24 patch blob mismatch")
if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC24 density correction")
    raise SystemExit(0)
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC24 baseline blob mismatch after RC23")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC24 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC24 density correction")
