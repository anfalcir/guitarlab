#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12g.sh"
PATCH_B64="$ROOT/.source-parts/U12iAgpNativePackagingCorrective.patch.b64"
PATCH_B64_BLOB="35d5e2613e082744d4692af800c764b93ddeda15"
PATCH_BLOB="78fd52618fbee8fe7a9ad3af4487dfc0bde8309a"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  "app/build.gradle.kts"
  "app/src/main/AndroidManifest.xml"
)

declare -a HASHES=(
  "c1337f08bcbd1f1be6daa55f9a3db3cc8c469036"
  "790f061d61ba77306e8c5f41bc18611b49959de8"
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
  grep -q 'useLegacyPackaging = true' "$ROOT/app/build.gradle.kts"
  ! grep -q 'android:extractNativeLibs=' "$ROOT/app/src/main/AndroidManifest.xml"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12i AGP native-packaging corrective payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12i base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12i decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12i AGP native packaging corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12g materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12i final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12i AGP native packaging corrective"
