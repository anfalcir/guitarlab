#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3a.sh"
PATCH_B64="$ROOT/.source-parts/C3bDuplicateFixture.patch.gz.b64"
PATCH_B64_SHA256="0f01f42949a783de18e885c4910b28e2b276261dee8630e1e476bb8a74d263fd"
PATCH_GZ_SHA256="75a32b8d9351878c3f4fd355f75866c97b8b36ac0d188088d7f15772c5a38fa3"
PATCH_SHA256="7b68726c55cc7adff52fe7891ec59f20bba1e805de3bbc5f0ffba71956ec9847"
TARGET="$ROOT/core/project/src/test/kotlin/studio/guitarlab/core/project/FileProjectRepositoryTest.kt"
PRE_HASH="0c1caabd3497ebc7689bbf6c4c368966655c8204"
POST_HASH="0e228adc8d6d57cd91e48bc98e141eab5e31b9da"
hash_target() { git -C "$ROOT" hash-object "$TARGET"; }
if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C3b"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C3a materializer" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C3b patch payload" >&2; exit 1; }
bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C3b refuses to patch: repository test blob mismatch" >&2; exit 1; }
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
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C3b final repository test blob mismatch" >&2; exit 1; }
grep -q 'create("Original", ProjectTemplate.BLANK)' "$TARGET" || { echo "C3b duplicate fixture normalization missing" >&2; exit 1; }
echo "Source patch chain materialized through C3b with exact blob verification"
