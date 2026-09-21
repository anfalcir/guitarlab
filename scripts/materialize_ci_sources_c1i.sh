#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1h.sh"
PATCH_B64="$ROOT/.source-parts/C1iHomeExportLazyListScroll.patch.gz.b64"
PATCH_B64_SHA256="8aa518518e88e190dd4aaa08a4a8e478955b518392fd584185fa85705ff492d7"
PATCH_GZ_SHA256="e9c4724d84f81993825aca75579bb9f5405aa2f98496fe2642c22df2d4e53717"
PATCH_SHA256="c3069da8753f4323ee763727335befb4f266cb111be318f7fed7128dc6da19f6"

TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="0d3b4d26f84eb9dceac22e33297d43aa145fc6fe"
POST_HASH="f7da808b7589f5015578511fb0da03f8ddbc0022"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1i"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1h materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1i patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1i refuses to patch: lifecycle test is not the exact C1h blob" >&2
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
  echo "C1i final lifecycle test blob mismatch" >&2
  exit 1
}

grep -q 'performScrollToNode(hasContentDescription(menuDescription))' "$TARGET" || {
  echo "C1i lazy-list scroll contract missing" >&2
  exit 1
}
grep -q 'runCatching { composeRule.onNodeWithTag(exportTag).assertIsDisplayed() }.isSuccess' "$TARGET" || {
  echo "C1i visible export menu wait missing" >&2
  exit 1
}
if grep -q 'onNodeWithContentDescription("Mais ações de .*performScrollTo' "$TARGET"; then
  echo "C1i found obsolete direct child performScrollTo" >&2
  exit 1
fi

echo "Source patch chain materialized through C1i with exact blob verification"
