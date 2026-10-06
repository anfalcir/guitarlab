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
    ["CuePreflightEvidence", "CueRouteTraceBuffer", "candidateCueSampleRates", "negotiatedProfile", "CONVERGED_TO_MAIN"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/ui/CueRouteController.kt",
    ["AudioDeviceCallback", "sessionCache", "fun retry()", "routing.selectCueOutput(null)"],
)
require_tokens(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    ["verifyMainEffectiveRoute", "qualifyRuntimeRoutes", "StereoLinearResampler", "scaleFramesBetweenRates", "WRITE_NON_BLOCKING"],
)
require_tokens(
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt",
    ["lastCuePreflightStatus", "lastCuePreflightEvidence", "cuePreflightJson"],
)
require_tokens(
    "docs/CURRENT_STATE.md",
    ["CONVERGED_TO_MAIN", "TARGETED JVM/ANDROID COMPILE PASS", "API36/PHYSICAL QUALIFICATION PENDING"],
)

print("Typed CUE capability source checkpoint verified")
