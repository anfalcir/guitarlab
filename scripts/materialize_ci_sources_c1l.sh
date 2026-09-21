#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1k.sh"
PATCH_B64="$ROOT/.source-parts/C1lHomeProjectViewportReset.patch.gz.b64"
PATCH_B64_SHA256="2edf493a4e6aba3f14048b6eb12c97d21d8fe86186deb27a6e6bf8dc52a296e5"
PATCH_GZ_SHA256="80622d361acea5527a067ccb3928ae7facacff12ba58eb031999d90ce4c89912"
PATCH_SHA256="a0266210fe006457b59fca3a53fa136726bcb59b5a9a9d6b2ddceeaa544bd3ab"

HOME_TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
TEST_TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"

HOME_PRE_HASH="61036fafba2ea1ad0054e0f31fde3296a8c0f0c5"
HOME_POST_HASH="9f35766ce34670388fe03b3800a6fb8bddea5a72"
TEST_PRE_HASH="14dae58c71330959e13189f1a2aec13306c12af2"
TEST_POST_HASH="6e80bdce709ba15009f6bd033c86b90bd1955caa"

hash_file() { git -C "$ROOT" hash-object "$1"; }

if [[ -f "$HOME_TARGET" && -f "$TEST_TARGET" ]] &&
   [[ "$(hash_file "$HOME_TARGET")" == "$HOME_POST_HASH" ]] &&
   [[ "$(hash_file "$TEST_TARGET")" == "$TEST_POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1l"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1k materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1l patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"

[[ "$(hash_file "$HOME_TARGET")" == "$HOME_PRE_HASH" ]] || {
  echo "C1l refuses to patch: HomeScreen is not the exact C1k blob" >&2
  exit 1
}
[[ "$(hash_file "$TEST_TARGET")" == "$TEST_PRE_HASH" ]] || {
  echo "C1l refuses to patch: lifecycle test is not the exact C1k blob" >&2
  exit 1
}

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

[[ "$(hash_file "$HOME_TARGET")" == "$HOME_POST_HASH" ]] || { echo "C1l final HomeScreen blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$TEST_TARGET")" == "$TEST_POST_HASH" ]] || { echo "C1l final lifecycle test blob mismatch" >&2; exit 1; }

grep -q 'rememberLazyListState' "$HOME_TARGET" || { echo "C1l Home list state missing" >&2; exit 1; }
grep -q 'projectListState.scrollToItem(0)' "$HOME_TARGET" || { echo "C1l query viewport reset missing" >&2; exit 1; }
grep -q 'state = projectListState' "$HOME_TARGET" || { echo "C1l LazyColumn state binding missing" >&2; exit 1; }
if ! sed -n '/onNodeWithContentDescription(menuDescription)/,+3p' "$TEST_TARGET" | grep -q 'assertIsDisplayed'; then
  echo "C1l visible Home routing action assertion missing" >&2
  exit 1
fi

echo "Source patch chain materialized through C1l with exact blob verification"
