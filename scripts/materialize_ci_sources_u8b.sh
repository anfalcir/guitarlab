#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8a.sh"
PATCH="$ROOT/.source-parts/U8bUnifiedDriveTransaction.patch"
PATCH_SHA256="f1e6430a08963f2d660e2cca2dd3cfae96da32bbf3f18a1694c63d983348f59d"
COORDINATOR="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveBackupCoordinator.kt"
TEST="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveBackupCoordinatorTest.kt"
COORDINATOR_HASH="41140d2f18a4e2c5ffbc4f1f37b85188e5787974"
TEST_HASH="2f758428496cda210fb9d12ffda30306e3f571bf"

ready() {
  [[ -f "$ROOT/$COORDINATOR" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$COORDINATOR")" == "$COORDINATOR_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8b"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8a materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8b patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$COORDINATOR" && ! -e "$ROOT/$TEST" ]] || {
  echo "U8b refuses to overwrite unexpected source files" >&2
  exit 1
}
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8b final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8b with exact blob verification"
