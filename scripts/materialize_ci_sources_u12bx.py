#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bw.py"
PATCH = ROOT / ".source-parts/U12bxPlaybackPresentationClock.patch"
PATCH_BLOB = "e037541facec07f84aa21fb2c968e0d6cb87d381"
TARGETS = {
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/PlaybackClockPolicy.kt": ("6533a6ba8758a17fa5321bcdd7db24622e85e7c3", "71b8d557ba55141ff3f319059ad48ac00718ff20"),
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/PlaybackClockPolicyTest.kt": ("22ee73cec194a0ad918d577a4cc58238525f7e5f", "ef8a7febeb6815229b86786b5a12338f855b61a4"),
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt": ("9366d7fa6a5dd2c39be11b1ce122840f42826a3a", "b8a3a427d051965fafaef4904a5fcfa9e56253c1"),
}

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))

def ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == after for rel, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == before for rel, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check")

    engine = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt").read_text(encoding="utf-8")
    clock = (ROOT / "core/audio/src/main/kotlin/studio/guitarlab/core/audio/PlaybackClockPolicy.kt").read_text(encoding="utf-8")
    tests = (ROOT / "core/audio/src/test/kotlin/studio/guitarlab/core/audio/PlaybackClockPolicyTest.kt").read_text(encoding="utf-8")

    required = (
        (engine, "var playbackClockAnchor: AudioClockAnchor? = null"),
        (engine, "playbackClockAnchor = stablePlaybackClockAnchor(audioTrack, request.sampleRateHz)"),
        (engine, "presentationOriginMonotonicNs = anchor.streamOriginMonotonicNs"),
        (engine, "playbackClockAnchor = null"),
        (engine, "clockAnchorAttempted = false"),
        (clock, "fun presentedFramesAt("),
        (clock, "nowMonotonicNs <= presentationOriginMonotonicNs"),
        (clock, "return presented.coerceIn(0L, written)"),
        (tests, "fun presentationClockDoesNotAdvanceBeforeAudioOrigin()"),
        (tests, "fun presentationClockMapsElapsedTimeAndNeverOutrunsWrittenAudio()"),
        (tests, "244_800L"),
    )
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"U12bx semantic guard failed: {token}")

    forbidden = (
        (engine, "val playbackClockAnchor = stablePlaybackClockAnchor(audioTrack, request.sampleRateHz)"),
        (engine, "val presentedFrames = playbackHeadDelta(playbackHead(audioTrack), clockHeadBase)"),
    )
    for text_value, token in forbidden:
        if token in text_value:
            raise SystemExit(f"U12bx stale playback clock remains: {token}")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bx patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bx")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bw materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bx baseline blob mismatch after U12bw")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bx terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bx")
