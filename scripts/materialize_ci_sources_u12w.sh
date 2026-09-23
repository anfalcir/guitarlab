#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12v.sh"
PATCH_B64="$ROOT/.source-parts/U12wCloudAuthUiInstrumentation.patch.b64"
PATCH_B64_BLOB="330f1bb3c6beb41f169c1aaa64b33c359ec7e3a7"
PATCH_BLOB="91e021cb88de815811dbd404ebad01ce0377b115"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
)

declare -a HASHES=(
  "ffa58aaa8b898d849c91de0b1c56f8244f8eb219"
  "df12a5bc6a7c4dc1718bceae439d68dcdbd0e363"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'settings-cloud-auth-submit' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  grep -q 'onNodeWithTag("settings-cloud-auth-submit").assertIsDisplayed()' "$ROOT/app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
  ! grep -q 'onNodeWithText("Entrar").assertIsDisplayed()' "$ROOT/app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
  grep -q 'versionName = "0.5.0-rc11"' "$ROOT/app/build.gradle.kts"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12w cloud-auth UI instrumentation payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12w base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12w decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12w cloud-auth UI instrumentation fix"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12v materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12w final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12w cloud-auth UI instrumentation fix"
