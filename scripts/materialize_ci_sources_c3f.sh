#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3e.sh"
PATCH_B64="$ROOT/.source-parts/C3fNewProjectSaveableState.patch.gz.b64"
PATCH_B64_SHA256="53e33bd3b6de0120ab1d06f9a358ede700659dc110b7a93c7114bd4223b7c2c7"
PATCH_GZ_SHA256="bb3908b0a51ac0453845624fab9c84f0e0942d867354348d4178d0b7156c2338"
PATCH_SHA256="33192a028fc0c3aebfb5ff8fc1bb69d06db600cd0f4a182a11ad719c68dabf7a"
FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
)
PRE_HASHES=(
  "9f11a4a68848c95d346dc325aa2685ee824c4e89"
  "e2c441718871448b70576bb4126399b8ca6f7b53"
  "f6c33e05d64beec5cb1db3121c31f994d44a90b8"
)
POST_HASHES=(
  "253d16dc0109bcada9fc5709ddbdd17328d1ee5c"
  "33e303ef02cf594192e5d995e7dfd11fac75d9fc"
  "94d7bc997fc785e9fa21a13c83011809fa385083"
)
hash_file() { git -C "$ROOT" hash-object "$ROOT/$1"; }
all_match() {
  local -n expected="$1"
  local i
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(hash_file "${FILES[$i]}")" == "${expected[$i]}" ]] || return 1
  done
}
if all_match POST_HASHES; then
  echo "Source patch chain already materialized through C3f"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C3e materializer" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C3f patch payload" >&2; exit 1; }
bash "$PREVIOUS"
all_match PRE_HASHES || { echo "C3f refuses to patch: input blob mismatch" >&2; exit 1; }
echo "$PATCH_B64_SHA256  $PATCH_B64" | sha256sum -c -
TMP_GZ="$(mktemp)"; TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PATCH_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
all_match POST_HASHES || { echo "C3f final blob mismatch" >&2; exit 1; }
grep -q 'rememberSaveable' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt" || { echo "C3f saveable New Project state missing" >&2; exit 1; }
grep -q 'NewProjectSourceIntent.IMPORT' "$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt" || { echo "C3f import intent coverage missing" >&2; exit 1; }
grep -q 'fun newProjectIntentAndNameSurviveActivityRecreation' "$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt" || { echo "C3f recreation coverage missing" >&2; exit 1; }
echo "Source patch chain materialized through C3f with exact blob verification"
