#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3b.sh"
PATCH_B64="$ROOT/.source-parts/C3cComposeAbsenceAssertions.patch.gz.b64"
PATCH_B64_SHA256="d273be25d44c7b3a086217a159cefd5f0bc60c925f21e0dad28b9cb1364df92a"
PATCH_GZ_SHA256="12599ec0e47a8166a023dc865bf74ffe986e53ccd46c5d5341e87b99f6f9d819"
PATCH_SHA256="4b689fb74a5d9af8af914c3efa8617149bfcc7be223b23e6cd3e91b606d0f602"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
PRE_HASH="f4cd3c7e6868e92b39b622e74bfc97d00ed26433"
POST_HASH="e2c441718871448b70576bb4126399b8ca6f7b53"
hash_target() { git -C "$ROOT" hash-object "$TARGET"; }
if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C3c"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C3b materializer" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C3c patch payload" >&2; exit 1; }
bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C3c refuses to patch: shell test blob mismatch" >&2; exit 1; }
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
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C3c final shell test blob mismatch" >&2; exit 1; }
! grep -q 'assertDoesNotExist' "$TARGET" || { echo "C3c unsupported Compose assertion remains" >&2; exit 1; }
grep -q 'onAllNodesWithText("Projeto vazio").assertCountEquals(0)' "$TARGET"
grep -q 'onAllNodesWithText("Importar áudio").assertCountEquals(0)' "$TARGET"
echo "Source patch chain materialized through C3c with exact blob verification"
