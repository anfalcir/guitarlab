#!/usr/bin/env python3
"""Hash-locked RC25 output/CUE correction; RC24 and prior payloads remain immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc24.py"
PATCH = ROOT / ".source-parts/RC25OutputCueCorrection.patch"
PATCH_BLOB = 'd9768bd6fef51ac6a1eabb056985927200bd671b'
TARGETS = {'app/build.gradle.kts': ('d32bed61a82e88438e339bbebdd57897baca62c1', '95d00255dbda77d9c72d36a50e71a38108400023'), 'app/src/androidTest/java/studio/guitarlab/app/PracticeWorkflowInstrumentedTest.kt': ('867caad652cc2dc4f9d8472313a9a847ab07d455', '0830eaab52a2c1555d782fbc0b8c447246f46685'), 'app/src/main/java/studio/guitarlab/app/ui/CueSelectionGate.kt': (None, '819ef509e4412ddc19068bc7c7abd4ff6b41dad5'), 'app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt': ('b0b2d2b14e90e25c4a5ecec17c93ebb46eba922d', '40a1109ae78f0f49eb5070ef3437ae7cd298cdf3'), 'app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutePolicy.kt': ('4d5668411da8029a1a9a93fe6a474fbb8b702d9c', 'f346389aa71eb648ed1d53727f93a9956c062313'), 'app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt': ('6608f5a828ad2c098bd713556e447cee5898eb1a', '85adc30e98acc9834ca1f0e60be33ba4a8535288'), 'app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt': ('6ba0066d7a120c97446d30bae34baa82e784619e', '604a5201761a57e99eadd49b759c386e91445cec'), 'app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt': ('e7594f890144197d8dceeb300183ecaa37e958d2', 'a51bf87546078fac670aff32078286d27968fdbe'), 'app/src/test/java/studio/guitarlab/app/ui/CueSelectionGateTest.kt': (None, '03fe3929b68ad19bf3e8816382e34824b20fe211'), 'app/src/test/java/studio/guitarlab/app/ui/StudioAudioRoutePolicyTest.kt': ('e3d90e7ce0a162f36df6e3fde514dc0df40f97fa', '6f614c381b83918527947f1c089a75e237a0134d'), 'core/audio/src/main/kotlin/studio/guitarlab/core/audio/CueStartupProbe.kt': (None, 'b62cf9ddff383bb1775de53c77723503e808c4a7'), 'core/audio/src/test/kotlin/studio/guitarlab/core/audio/CueStartupProbeTest.kt': (None, '0fb8b718dde2b19ea6a1be653f746dd17fc3ca9a'), 'platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt': (None, 'c07015dca2ed98369a1dafb5c849f948e964901b'), 'platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt': ('661356ad9e7d8c6dd76db0bf314c6734136d8e1a', 'e431b2db9bf6021080b0baf9cce102770ce33566')}

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
        "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt": ["CueStartupProbe.verify(", "AudioTrack.WRITE_NON_BLOCKING"],
        "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutePolicy.kt": ["BLUETOOTH_SCO_TYPE", "singleOrNull()?.signature"],
        "app/src/androidTest/java/studio/guitarlab/app/PracticeWorkflowInstrumentedTest.kt": ["unavailableCueLeavesTrackAndHistoryOnMain"],
        "app/build.gradle.kts": ['versionName = "0.5.0-rc25"', "versionCode = 45"],
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
    print("Source chain already materialized through RC25 output/CUE correction")
    raise SystemExit(0)
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC25 baseline blob mismatch after RC24")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC25 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC25 output/CUE correction")
