#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1a.sh"
PATCH_B64="$ROOT/.source-parts/C1bViewportTests.patch.gz.b64"
PATCH_B64_SHA256="475e4825c921d1e1461a2f822704af7793e35f680e3c8be4830eea21d4a0b819"
PATCH_GZ_SHA256="ad72f0771e4455c852164946e3074102cc4f06fd462d6b9cce63866d72f469af"
PATCH_SHA256="6839c79f8a6a95e83b5107039dfb68ca1b367e39f56b31c0feedb227cd497eff"

TARGET1="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE1="0a40382e2ef0b9a3c889b2203c9dcf1215c2f4dd"
POST1="04b864b289d1302f1481420508b6dd9fb87f1c73"
TARGET2="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
PRE2="b6fa99e435233490fe9e9958b4917e1b021acfc4"
POST2="7559bb8909ee7290352bd3c6c2810e7e6983d4d5"

hash_file() { git -C "$ROOT" hash-object "$1"; }
ready() {
  [[ -f "$TARGET1" && -f "$TARGET2" ]] &&
  [[ "$(hash_file "$TARGET1")" == "$POST1" ]] &&
  [[ "$(hash_file "$TARGET2")" == "$POST2" ]]
}

if ready; then
  echo "Source patch chain already materialized through C1b"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1a materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1b patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_file "$TARGET1")" == "$PRE1" ]] || { echo "C1b refuses to patch: lifecycle test blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$TARGET2")" == "$PRE2" ]] || { echo "C1b refuses to patch: workspace test blob mismatch" >&2; exit 1; }

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

ready || { echo "C1b final test blobs do not match locked outputs" >&2; exit 1; }
grep -q 'onNodeWithText("Exportar").performScrollTo().assertIsDisplayed().performClick()' "$TARGET1" || { echo "C1b Home export viewport assertion missing" >&2; exit 1; }
grep -q 'onNodeWithTag("export-guitar-wav").performScrollTo().assertIsDisplayed().assertIsEnabled()' "$TARGET2" || { echo "C1b export workspace viewport assertion missing" >&2; exit 1; }

echo "Source patch chain materialized through C1b with exact blob verification"
