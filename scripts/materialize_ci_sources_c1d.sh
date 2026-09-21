#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1c.sh"
PATCH_B64="$ROOT/.source-parts/C1dStableHomeExportTag.patch.gz.b64"
PATCH_B64_SHA256="50bd4c174564dace8896724d2dbb6b72805c2a8736c2e9a7270b6b815d878b8e"
PATCH_GZ_SHA256="6e510792e931e54b6f71087d66349605646939664884baeaa85238893f34d87f"
PATCH_SHA256="e1ab1b7827f8300f9e6daa614b1f2358dd41e60f459c910c169e5b201d8addf3"

HOME="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
HOME_PRE="fa571c6b4ef86035c4affe1f98756d2ac4ba9b29"
HOME_POST="61036fafba2ea1ad0054e0f31fde3296a8c0f0c5"
TEST="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
TEST_PRE="4eacdfb56270bdf44d55a81bea6958aa13527a2b"
TEST_POST="3baaad0af31abeafe41b67c6845ea0da85821447"

hash_file() { git -C "$ROOT" hash-object "$1"; }
ready() {
  [[ -f "$HOME" && -f "$TEST" ]] &&
  [[ "$(hash_file "$HOME")" == "$HOME_POST" ]] &&
  [[ "$(hash_file "$TEST")" == "$TEST_POST" ]]
}

if ready; then
  echo "Source patch chain already materialized through C1d"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1c materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1d patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_file "$HOME")" == "$HOME_PRE" ]] || { echo "C1d refuses to patch: HomeScreen blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$TEST")" == "$TEST_PRE" ]] || { echo "C1d refuses to patch: lifecycle test blob mismatch" >&2; exit 1; }

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
git -C "$ROOT" diff --check

ready || { echo "C1d final blobs do not match locked outputs" >&2; exit 1; }
grep -q 'Modifier.testTag("project-export-${project.id}")' "$HOME" || { echo "C1d stable Home export tag missing" >&2; exit 1; }
grep -q 'onNodeWithTag("project-export-${project.id}").assertIsDisplayed().performClick()' "$TEST" || { echo "C1d lifecycle test does not use stable Home export tag" >&2; exit 1; }

echo "Source patch chain materialized through C1d with exact blob verification"
