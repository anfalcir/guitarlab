#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1d.sh"
PATCH_B64="$ROOT/.source-parts/C1eHomeExportMenuScroll.patch.gz.b64"
PATCH_B64_SHA256="22cc76c903f2510cce83c1913616ded5f43423a669ecae0d9af13c7bed13aceb"
PATCH_GZ_SHA256="d7fa8fd77e56ab7617f908cb7750d127b50e84d211a9c88794a48069c1270ef9"
PATCH_SHA256="6d021b978a5af89480cad6c70aed11684f956968a1675cc0326e0a08b9896427"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="3baaad0af31abeafe41b67c6845ea0da85821447"
POST_HASH="f9c7753b745f7060c8316465afca4150e069c1c4"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1e"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1d materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1e patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1e refuses to patch: lifecycle test is not the exact C1d blob" >&2
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
  echo "C1e final lifecycle test blob mismatch" >&2
  exit 1
}
grep -q 'onNodeWithTag("project-export-${project.id}").performScrollTo().assertIsDisplayed().performClick()' "$TARGET" || {
  echo "C1e viewport-safe Home export assertion missing" >&2
  exit 1
}

echo "Source patch chain materialized through C1e with exact blob verification"
