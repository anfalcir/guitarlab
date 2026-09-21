#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1e.sh"
PATCH_B64="$ROOT/.source-parts/C1fFinalScrollImport.patch.gz.b64"
PATCH_B64_SHA256="14f728d68f0dd5d5483bcd2063f985f8643fb62b1cc67bc398b0d55e52960ea3"
PATCH_GZ_SHA256="ab17d10d7e679a8fe440245fa70481b5870c87a24a4d23bb5db865cf64338cd1"
PATCH_SHA256="870a6e55a0fe8e32149e2204675f2e3b760ee2701bf8a1177906cb5472eccfd0"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="f9c7753b745f7060c8316465afca4150e069c1c4"
POST_HASH="4763ef745f2814c461ee1b658e7a75e952a8a69d"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1f"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1e materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1f patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1f refuses to patch: lifecycle test is not the exact C1e blob" >&2
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
  echo "C1f final lifecycle test blob mismatch" >&2
  exit 1
}
grep -q '^import androidx.compose.ui.test.performScrollTo$' "$TARGET" || {
  echo "C1f performScrollTo import missing" >&2
  exit 1
}

echo "Source patch chain materialized through C1f with exact blob verification"
