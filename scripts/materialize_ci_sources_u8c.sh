#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8b.sh"
PATCH="$ROOT/.source-parts/U8cDriveV3Adapter.patch"
PATCH_SHA256="076006f4a9a07a453cd9bc00ed12e7eeba99fe6b720523b98f2a81dce221ffc8"
ADAPTER="app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt"
TEST="app/src/test/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStoreTest.kt"
ADAPTER_HASH="608c9371c666a260071028f7049cd2014808dadb"
TEST_HASH="745112e4c36b9707266b79a2e461b9c3ad342544"

ready() {
  [[ -f "$ROOT/$ADAPTER" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$ADAPTER")" == "$ADAPTER_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8c"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U8b materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8c patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$ADAPTER" && ! -e "$ROOT/$TEST" ]] || { echo "U8c refuses to overwrite unexpected source files" >&2; exit 1; }
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8c final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8c with exact blob verification"
