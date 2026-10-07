#!/usr/bin/env python3
"""Verify the post-RC29 typed CUE capability source checkpoint."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def require_tokens(relative: str, tokens: list[str]) -> None:
    path = ROOT / relative
    if not path.is_file():
        raise SystemExit(f"CUE capability source missing: {relative}")
    source = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in source:
            raise SystemExit(f"CUE capability guard failed in {relative}: {token}")


subprocess.run(
    ["git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch"],
    cwd=ROOT,
    check=True,
)

require_tokens(
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/CueStartupProbe.kt",
    ["CONVERGED_TO_MAIN", "MISSING_EFFECTIVE_ROUTE", "WRONG_OR_MIRRORED_ROUTE", "routedPhysicalKeys"],
)
require_tokens(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt",
    ["CuePreflightEvidence", "CueCommunicationEvidence", "candidateCueSampleRates", "negotiatedProfile", "COMMUNICATION_SPLIT", "beginRuntimeCueDiagnostic", "clearRuntimeCueDiagnostic"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/ui/CueRouteController.kt",
    ["AudioDeviceCallback", "sessionCache", "fun retry()", "routing.selectCueOutput(null)"],
)
require_tokens(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    ["verifyMainEffectiveRoute", "qualifyRuntimeRoutes", "CueRelativeDriftMonitor", "RUNTIME_ROUTE_TARGET_OUTSTANDING_FRAMES", "initiallyQueuedCueFrames"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt",
    ["lastCuePreflightStatus", "lastCuePreflightEvidence", "lastCueRuntimeVersion", "lastCueRuntime", "initialOffsetNs", "duckingRequested"],
)
require_tokens(
    "docs/CURRENT_STATE.md",
    ["RC34 COMMUNICATION PHASE PROBE", "SIGNED CI PENDING", "PHYSICAL DIAGNOSTIC PENDING"],
)

require_tokens(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/CueRuntimeAlignment.kt",
    ["CueRuntimeWarmupPolicy", "CueRelativeDriftMonitor", "baselineDeltaFrames", "relativeDriftFrames"],
)

communication_source = (
    ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/CommunicationCueRouting.kt"
).read_text(encoding="utf-8")
if "CommunicationCuePolicy" in communication_source:
    raise SystemExit("Obsolete CommunicationCuePolicy must not return to the active routing path")

engine_source = (
    ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt"
).read_text(encoding="utf-8")
if "cueDriftUnsafeSinceNs" in engine_source:
    raise SystemExit("Communication runtime drift must use the relative-baseline monitor")

require_tokens(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/CommunicationSplitPhaseProbe.kt",
    ["A_MAIN_ONLY", "B_COMMUNICATION_DEVICE_SELECTED", "C_CUE_TRACK_SILENT", "D_CUE_TONE_ACTIVE", "STREAM_MUSIC", "STREAM_VOICE_CALL", "mainPreferredReassertedMidPhase", "cueToneOutcome", "mainAfterReassertOutcome"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/ui/CueRouteController.kt",
    ["runCommunicationProbe", "recordCommunicationProbeOutcome", "recordCommunicationProbeCueToneOutcome", "recordCommunicationProbeMainAfterReassertOutcome", "audio.cue_communication_probe"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt",
    ["settings-cue-communication-probe", "Em qual etapa o tom grave da MAIN deixou de ser ouvido?"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt",
    ["audio-communication-probe.json", "communicationSplitProbeJson"],
)

print("Typed CUE capability source checkpoint verified")

for relative in [
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt",
]:
    source = (ROOT / relative).read_text(encoding="utf-8")
    forbidden = "AUDIOFOCUS_GAIN_TRANSIENT_" + "MAY_DUCK"
    if forbidden in source:
        raise SystemExit(f"CUE zero-duck invariant violated in {relative}")
