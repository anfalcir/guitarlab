#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1f.sh"
PATCH_B64="$ROOT/.source-parts/C1gWaitForHomeExportMenu.patch.gz.b64"
PATCH_B64_SHA256="2cdaebb664bb04cbb6c7da1b5896446154678cb60f3ea7070d2c1dfadc7e8cd0"
PATCH_GZ_SHA256="3b57020af5e515ab4ba6107eca26eb72ed1cfc914cf98f593e76f4a71995f98e"
PATCH_SHA256="bf964e17350652a68e5fbd3815c22a457d6cd25028a6cf934a079d2b29141e36"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="4763ef745f2814c461ee1b658e7a75e952a8a69d"
POST_HASH="ad22866c2403526154198b35817cf5844384fde6"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1g"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1f materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1g patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1g refuses to patch: lifecycle test is not the exact C1f blob" >&2
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
[[ "$(hash_target)" == "$POST_HASH" ]] || {
  echo "C1g final lifecycle test blob mismatch" >&2
  exit 1
}
grep -q 'runCatching { composeRule.onNodeWithTag(exportTag).fetchSemanticsNode() }.isSuccess' "$TARGET" || {
  echo "C1g async menu existence wait missing" >&2
  exit 1
}

echo "Source patch chain materialized through C1g with exact blob verification"
