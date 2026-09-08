#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidAudioProbeEngine.kt"
mkdir -p "$(dirname "$TARGET")"
cat "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part00 "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part01 "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part02 "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part03 > "$TARGET"
