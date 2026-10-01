#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc21.py"
PATCH = ROOT / ".source-parts/RC22DualOutputCueRouting.patch"
PATCH_BLOB = "361ddd394149494c82b82bc2e1501bd827bdf973"
TARGETS = {
    "app/build.gradle.kts": ("076c5e76779c30a2a78df6edbbf5f849950134f8", "5e3343d00d2259fca4f57dadcbb9652355398fba"),
    "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": ("bf81aa9414a376679634f8ddf3f0b9bbf58fde5d", "1231ebcf48ba39bb336b4d1e087c55bbb87ca014"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt": ("f13441dec13ca4a09271fe0fd4df0a75e3a4e54d", "74c17436f3f7b7fc168abcf27679cdffebd334e9"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt": ("a0bb842418119c781c7f8832219d037d94e68490", "52379c7fe5518d619f58ca7326fa2899194ff1c3"),
    "app/src/main/java/studio/guitarlab/app/ui/AudioProbeViewModel.kt": ("2d4fba816d70ee023b513d46a517c248c2a6e540", "8f91a6bf3a5339eb30ae5e97afe4c2baac40e39a"),
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ("f55a09065866f8588e1e9c709595c3060a28e1f5", "7cf7410bb0c40a7d9eae31462c046928cbc5d239"),
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt": ("e4c982b4039d00e412901eb0ed75a58b94b298fa", "4f53241bff34e1e5e68c570753ec5e90eb78c60a"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt": ("c22242d284fe0891ccba526b30d04ee93a31692a", "862dc29cd00b872b34fb9b6240f12eba8d03448b"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ("6cd72c76501aef0b3cabc5b5c9f6f3c6dfb1aa3e", "40ec9b20efcf86325ff09e1c5cc2b8871e1a8833"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt": ("351de1cef1549091f3a08da2ab2d8a4d252721ae", "f36fb2a951aa8a7140b4616bce0b99f022342d13"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt": ("1c9e472debdf654de1b4b377ce3611c152812886", "8fac1d60715fa0de35b546b75c302608f2c73d7f"),
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/CueRouteSafetyPolicy.kt": (None, "e6a3e44aac493a268811eb72589a673d10353dc8"),
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/CueRouteSafetyPolicyTest.kt": (None, "9dbebd1905a8afb69223467bad1499395ba30b60"),
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt": ("6a0ccb1e2f9b41694fd815486dab3e0251c2a1e0", "86fea2a477c6e930cbbccb1152009a20ee8181b1"),
    "core/model/src/main/kotlin/studio/guitarlab/core/model/TrackOutputRoutingPolicy.kt": (None, "6c2aaa8f0fdbbb9a95e6253c9cddcf236abe7525"),
    "core/model/src/test/kotlin/studio/guitarlab/core/model/TrackOutputRoutingPolicyTest.kt": (None, "83efbeeab48b0d4ce739326f3311dbeed4c64a2c"),
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectCodecCompatibilityTest.kt": ("dcc828a0c57ad6a1f5f379170f817e1ea2146332", "5b06cf31025e7b70471302fb00f068838fbe1879"),
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt": ("b8a3a427d051965fafaef4904a5fcfa9e56253c1", "21001f07ea03fe22abacb6ae782b3cf8a9fced9a"),
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioRecordingEngine.kt": ("50dd319428389cc5128b72263c56e5c0f60a3897", "4f8d5947c598da2d6bd324af8d7f073a0dfc39e0"),
    "platform/audio-android/src/test/kotlin/studio/guitarlab/platform/audio/android/RecordingInputRoutePolicyTest.kt": ("71649fa5210d518df8fe7e8545f7b0cbe668bbc4", "16fb515b50857c334e953f59028960dd796c9657"),
}


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))


def ready() -> bool:
    return all(
        (ROOT / rel).is_file() and blob(ROOT / rel) == after
        for rel, (_, after) in TARGETS.items()
    )


def baseline_ready() -> bool:
    return all(
        (not (ROOT / rel).exists())
        if before is None
        else ((ROOT / rel).is_file() and blob(ROOT / rel) == before)
        for rel, (before, _) in TARGETS.items()
    )


def require(text: str, token: str, label: str) -> None:
    if token not in text:
        raise SystemExit(f"RC22 semantic guard failed ({label}): {token}")


def verify() -> None:
    run("git", "diff", "--check")

    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    model = (ROOT / "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt").read_text(encoding="utf-8")
    track_policy = (ROOT / "core/model/src/main/kotlin/studio/guitarlab/core/model/TrackOutputRoutingPolicy.kt").read_text(encoding="utf-8")
    cue_policy = (ROOT / "core/audio/src/main/kotlin/studio/guitarlab/core/audio/CueRouteSafetyPolicy.kt").read_text(encoding="utf-8")
    playback = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt").read_text(encoding="utf-8")
    recording = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioRecordingEngine.kt").read_text(encoding="utf-8")
    routing = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt").read_text(encoding="utf-8")
    settings = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt").read_text(encoding="utf-8")
    mixer = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt").read_text(encoding="utf-8")
    studio = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt").read_text(encoding="utf-8")
    guide = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt").read_text(encoding="utf-8")
    codec_test = (ROOT / "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectCodecCompatibilityTest.kt").read_text(encoding="utf-8")
    cue_test = (ROOT / "core/audio/src/test/kotlin/studio/guitarlab/core/audio/CueRouteSafetyPolicyTest.kt").read_text(encoding="utf-8")
    mixer_test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt").read_text(encoding="utf-8")

    guards = (
        (build, 'versionName = "0.5.0-rc22"', "version name"),
        (build, "versionCode = 42", "version code"),
        (model, "enum class TrackOutputRoute { MAIN, CUE, MAIN_AND_CUE }", "route model"),
        (model, "val outputRoute: TrackOutputRoute = TrackOutputRoute.MAIN", "backward-compatible route default"),
        (track_policy, "fun toggleExclusiveCue", "track routing policy"),
        (cue_policy, "object CueRouteSafetyPolicy", "cue safety policy"),
        (cue_policy, "CueRouteBlockReason.SAME_ENDPOINT", "same-endpoint guard"),
        (cue_policy, "fun secondaryWriteComplete", "secondary CUE write isolation"),
        (playback, "val preferredCueOutputDevice: AudioDeviceInfo? = null", "cue request"),
        (playback, "primeAndVerifyDualRoutes", "silent dual-route verification"),
        (playback, "cueRouteStillSafe", "runtime route verification"),
        (playback, "CueRouteSafetyPolicy.driftExceeded", "runtime drift guard"),
        (playback, "AudioTrack.WRITE_NON_BLOCKING", "non-blocking CUE writer"),
        (recording, "val preferredOutputRequired: Boolean = false", "required live-monitor route"),
        (recording, "RecordingMonitorRoutePolicy.accepts", "live-monitor fail closed"),
        (routing, "fun selectedCueOutputSignature()", "persisted cue route"),
        (routing, "fun resolveSelectedCueOutputDevice()", "cue endpoint resolver"),
        (settings, '"Saída secundária / CUE"', "settings cue selector"),
        (settings, '"Seleção e disponibilidade"', "selection availability copy"),
        (mixer, '"Saída CUE da pista', "mixer cue accessibility"),
        (mixer, "Icons.Default.Headphones", "headphone icon"),
        (mixer_test, "assertIsNotEnabled()", "transport-locked CUE test"),
        (studio, "fun toggleTrackCue(trackId: String)", "studio cue command"),
        (studio, "if (!structuralEditingAllowed(state)) return", "transport-stable cue command"),
        (studio, "preferredCueOutputRequested =", "studio cue playback integration"),
        (guide, "fone/CUE", "in-app guide"),
        (guide, "Durante Play/REC essa rota fica travada", "stopped-only guide"),
        (codec_test, "legacyTrackWithoutOutputRouteDefaultsToMain", "project compatibility test"),
        (cue_test, "cueAdmissionRequiresTwoExplicitDistinctResolvedEndpoints", "cue safety tests"),
        (mixer_test, '"Saída CUE da pista Teste"', "mixer instrumented test"),
    )
    for text_value, token, label in guards:
        require(text_value, token, label)

    if "KEY_CUE_OUTPUT_SIGNATURE" not in routing:
        raise SystemExit("RC22 semantic guard failed: CUE signature key missing")
    if "selectedOutputSignature() ?: return null" not in routing:
        raise SystemExit("RC22 semantic guard failed: explicit MAIN requirement missing")
    if "outputRoute = it.outputRoute" not in studio:
        raise SystemExit("RC22 semantic guard failed: recording/playback route propagation missing")


if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC22 patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC22 dual-output/CUE")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing RC21 materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC22 baseline blob mismatch after RC21")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC22 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC22 dual-output/CUE")
