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
    ["RC33 RUNTIME ALIGNMENT", "SIGNED CI PENDING", "PHYSICAL REQUALIFICATION PENDING"],
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

print("Typed CUE capability source checkpoint verified")

for relative in [
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt",
]:
    source = (ROOT / relative).read_text(encoding="utf-8")
    forbidden = "AUDIOFOCUS_GAIN_TRANSIENT_" + "MAY_DUCK"
    if forbidden in source:
        raise SystemExit(f"CUE zero-duck invariant violated in {relative}")
