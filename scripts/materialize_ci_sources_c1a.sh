#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1.sh"
PATCH_B64="$ROOT/.source-parts/C1aTestCompile.patch.gz.b64"
PATCH_B64_SHA256="7ae2a9d6a47388e5a86a2d88f636571d5ff3e63dacb462b465ef818d0a027c94"
PATCH_GZ_SHA256="9f798b80c31be558fc0da4087aa2d61553c58ea5b87868cf7eb8c9456006e7fd"
PATCH_SHA256="fa3bebb5f30108605bfbefb37979ab52cb2520ec89c553cb06a49181c58de389"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="ae39b6b4ec94d3b4543106d9e47ee2b55e8fc7e7"
POST_HASH="0a40382e2ef0b9a3c889b2203c9dcf1215c2f4dd"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1a"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1 materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1a patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1a refuses to patch: lifecycle test is not the exact C1 blob" >&2
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
  echo "C1a final lifecycle test blob mismatch" >&2
  exit 1
}
! grep -q '^import androidx.compose.ui.test.assertDoesNotExist$' "$TARGET" || {
  echo "C1a obsolete Compose test import remains" >&2
  exit 1
}

echo "Source patch chain materialized through C1a with exact blob verification"
