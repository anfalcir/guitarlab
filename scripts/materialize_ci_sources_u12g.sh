#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12f.sh"
PATCH_B64="$ROOT/.source-parts/U12gSourceDiscoveryRuntimeCorrective.patch.b64"
PATCH_B64_BLOB="53a849a3714d4b2b44fdb7b231f4a6609d4b94b2"
PATCH_BLOB="f69ffa8ba03ba1f7d5c9897ecccc291b6710ae4d"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  "app/build.gradle.kts"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "platform/source-android/build.gradle.kts"
  "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceDiscovery.kt"
  "platform/source-android/src/test/kotlin/studio/guitarlab/platform/source/android/SourceDiscoveryTest.kt"
)

declare -a HASHES=(
  "a057ee46384c75718ad2aec8b65bc7d2ef0c266f"
  "b19146be67ec25cd224e803cf5a58e8aec2b5bcb"
  "aed74d95956c4cb3de17c1d91d7866d167e547ad"
  "5e380015df5b767d24d9f507bcf4060bae2e840f"
  "097bca78bc9cbfc2bc08b8fc41d96ab258dddb92"
  "8d41e7381f9d7ece0b8e806aca64355659265f32"
  "995c908fd191e7ea881b4f38b21a6a3101828b58"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc7"' "$ROOT/app/build.gradle.kts"
  grep -q 'versionCode = 27' "$ROOT/app/build.gradle.kts"
  grep -q 'android:extractNativeLibs="true"' "$ROOT/app/src/main/AndroidManifest.xml"
  grep -q 'refreshRuntime(force = true)' "$ROOT/platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceDiscovery.kt"
  grep -q 'YouTube/SoundCloud indisponíveis durante a pesquisa' "$ROOT/platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceDiscovery.kt"
  grep -q 'Pesquisa de fontes indisponível' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  grep -q 'packagedSourceRuntimeExtractsNativeLibraries' "$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  grep -q 'ytDlpSearchRetriesAfterForcedRuntimeRefresh' "$ROOT/platform/source-android/src/test/kotlin/studio/guitarlab/platform/source/android/SourceDiscoveryTest.kt"
  grep -q 'ytDlpSearchSurfacesPersistentRuntimeFailure' "$ROOT/platform/source-android/src/test/kotlin/studio/guitarlab/platform/source/android/SourceDiscoveryTest.kt"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12g source-discovery runtime corrective payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12g base64 payload blob mismatch" >&2
  exit 1
}

base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12g decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12g source-discovery runtime corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12f materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12g final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12g source-discovery runtime corrective"
