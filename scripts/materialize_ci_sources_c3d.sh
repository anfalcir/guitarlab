#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3c.sh"
PATCH_B64="$ROOT/.source-parts/C3dLifecycleNewProjectIntent.patch.gz.b64"
PATCH_B64_SHA256="aebda23db9870cc3f7082159412505780feef894b476d611dcedc1bda66525ba"
PATCH_GZ_SHA256="93cdea90c6188937361a3af1dcfe8c096c369478698fb2e0bf997cb209e2e698"
PATCH_SHA256="c90eb2b4bbe47642d9a0865ec04888e87f10afa6235f5f826f6aa75b67732589"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="eb5bd523062c3ee1ee4c316163838b58edd99153"
POST_HASH="d9650eb89c12fbe49a7ad233cd223d955cf7456a"
hash_target() { git -C "$ROOT" hash-object "$TARGET"; }
if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C3d"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C3c materializer" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C3d patch payload" >&2; exit 1; }
bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C3d refuses to patch: lifecycle test blob mismatch" >&2; exit 1; }
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
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C3d final lifecycle test blob mismatch" >&2; exit 1; }
grep -q 'onNodeWithTag("new-project-studio").performClick()' "$TARGET" || { echo "C3d explicit Studio intent lifecycle step missing" >&2; exit 1; }
echo "Source patch chain materialized through C3d with exact blob verification"
