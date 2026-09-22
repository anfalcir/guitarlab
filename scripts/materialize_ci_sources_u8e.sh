#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8d.sh"
PATCH="$ROOT/.source-parts/U8eTransactionalDriveRestore.patch"
PATCH_SHA256="67a3ef5834e5c20f5d0964e471f3b5e0e12c6c7b39ee29b34bf2f675e598360a"
RESTORE="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveRestoreCoordinator.kt"
TEST="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveRestoreCoordinatorTest.kt"
RESTORE_HASH="1a3da5a7c5440811778d8abeb17ebfa14da35d13"
TEST_HASH="146ca5da6fd0483c566bc9d621d9ca11d79aa643"

ready() {
  [[ -f "$ROOT/$RESTORE" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$RESTORE")" == "$RESTORE_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8e"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U8d materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8e patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$RESTORE" && ! -e "$ROOT/$TEST" ]] || { echo "U8e refuses to overwrite unexpected source files" >&2; exit 1; }
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8e final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8e with exact blob verification"
