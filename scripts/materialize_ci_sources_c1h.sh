#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1g.sh"
PATCH_B64="$ROOT/.source-parts/C1hHomeMenuAnchorViewport.patch.gz.b64"
PATCH_B64_SHA256="bb054b1509e2f30be7f10368ff04a4c37a00fdf73128a84300ef6f479c722eb2"
PATCH_GZ_SHA256="31ec6528e59bc030fb06026e2553c40eb81ec374e16e99a40d587ced049cf699"
PATCH_SHA256="0e0f5767589c6ddf2edfff05d328e8dc880da834e90db85dcd424c2f62d7ad62"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="ad22866c2403526154198b35817cf5844384fde6"
POST_HASH="0d3b4d26f84eb9dceac22e33297d43aa145fc6fe"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1h"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1g materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1h patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1h refuses to patch: lifecycle test is not the exact C1g blob" >&2
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
  echo "C1h final lifecycle test blob mismatch" >&2
  exit 1
}
grep -q 'onNodeWithContentDescription("Mais ações de ${project.name}")' "$TARGET" || {
  echo "C1h project menu anchor missing" >&2
  exit 1
}
grep -q '^                \.performScrollTo()$' "$TARGET" || {
  echo "C1h project menu anchor is not viewport-safe" >&2
  exit 1
}

echo "Source patch chain materialized through C1h with exact blob verification"
