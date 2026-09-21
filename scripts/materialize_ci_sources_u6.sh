#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u5c.sh"
PATCH_B64="$ROOT/.source-parts/U6UnifiedExports.patch.gz.b64"
PATCH_B64_SHA256="fcec95a908b5375f59809d8ebbd9fc48f5adfcb205a67d7bba9659637c1deab0"
PATCH_GZ_SHA256="7d636f7d074d3094757feeb30da72d279093ad1cd309366a45e1145f351e4998"
PATCH_SHA256="54e09e59c3bec75891d0c8ddca1be5bf5c7e38772afe395c2aaaf6daee257aa1"

CHECKS=(
  "app/src/main/java/studio/guitarlab/app/ui/ProjectExportService.kt|abf839787e4db7aea74d474ba4fb5d31bb012f7c"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|385eaf35911d63b53c559b7f46c4a905a0c7ab1f"
  "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|67749191944adcfd411df28941dd638dd41d575a"
  "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/StudioMasterRenderer.kt|8d529a5b88daa8172e3d35f2a633250a39fb0625"
  "platform/codec-android/src/main/kotlin/studio/guitarlab/platform/codec/android/AndroidMasterAudioEncoder.kt|991df19043b8f0e93fbdc964714ef78603982ac7"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt|af6e59bbb5ab96fdce12336cb4e12a869abd47e6"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }

u6_ready() {
  local entry relative expected
  for entry in "${CHECKS[@]}"; do
    relative="${entry%%|*}"
    expected="${entry#*|}"
    [[ -f "$ROOT/$relative" ]] || return 1
    [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
  done
}

if u6_ready; then
  echo "Source patch chain already materialized through U6"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U5c materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing U6 source payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"

echo "$PATCH_B64_SHA256  $PATCH_B64" | sha256sum -c -
TMP_GZ="$(mktemp)"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PATCH_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -

git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"

u6_ready || {
  echo "U6 applied but final hashes do not match" >&2
  exit 1
}
echo "Source patch chain materialized through U6 with verified final hashes"
