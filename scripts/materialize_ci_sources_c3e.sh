#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3d.sh"
PATCH_B64="$ROOT/.source-parts/C3eLifecycleCreateCta.patch.gz.b64"
PATCH_B64_SHA256="d8f0e3b2836c7735bb1c36a684681c341c02e7de76f0bd8c99376a5a81893f67"
PATCH_GZ_SHA256="63584700b34a1af3856afc3dbb3e3081568ce18989d0f1405e77af4323a7fe85"
PATCH_SHA256="c71e9e49d7b33afcc297e1b76a5d75531a06dd680fb17e582ea36fb5f39e30c2"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="d9650eb89c12fbe49a7ad233cd223d955cf7456a"
POST_HASH="f6c33e05d64beec5cb1db3121c31f994d44a90b8"
hash_target() { git -C "$ROOT" hash-object "$TARGET"; }
if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C3e"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C3d materializer" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C3e patch payload" >&2; exit 1; }
bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C3e refuses to patch: lifecycle test blob mismatch" >&2; exit 1; }
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
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C3e final lifecycle test blob mismatch" >&2; exit 1; }
grep -q 'onNodeWithTag("new-project-create").assertIsEnabled().performClick()' "$TARGET" || { echo "C3e canonical create CTA assertion missing" >&2; exit 1; }
! grep -q 'onNodeWithText("Criar projeto")' "$TARGET" || { echo "C3e legacy create CTA assertion remains" >&2; exit 1; }
echo "Source patch chain materialized through C3e with exact blob verification"
