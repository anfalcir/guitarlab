#!/usr/bin/env bash
# U10d/C7d source seal: shared empty/blocked product state across Home, Activity and Backup.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10c.sh"
PATCH="$ROOT/.source-parts/U10dUnifiedEmptyStates.patch"
PATCH_BLOB="46f1f54824179c21f0622e7c25aeb79adad4b2d8"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/ProductUiPrimitives.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
)

declare -a HASHES=(
  "a6fbcc4034c91a46ff70d24dc04cd75d26a541be"
  "f92d4b2f7a8314b5ca338398859f28b7b1e8d79a"
  "6ba8725b92002bb8d1b87b411a80e51f1f934d58"
  "9217c25e73808a1a74e8cf600e2d6b42857bbb59"
  "5ecfbb650cf247274700d7985f787675964372bd"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10d unified-empty-state patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10d unified-empty-state patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  local primitive="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProductUiPrimitives.kt"
  local home="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  local activity="$ROOT/app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  local backup="$ROOT/app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  local test="$ROOT/app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"

  grep -q 'fun ProductEmptyState' "$primitive" || { echo "U10d shared ProductEmptyState missing" >&2; exit 1; }
  grep -q 'ProductEmptyState(' "$home" || { echo "U10d Home did not adopt shared empty state" >&2; exit 1; }
  grep -q 'modifier = modifier.testTag("home-empty")' "$home" || { echo "U10d Home real-empty tag missing" >&2; exit 1; }
  grep -q 'ProductEmptyState(' "$activity" || { echo "U10d Activity did not adopt shared empty state" >&2; exit 1; }
  grep -q 'testTag("activity-empty")' "$activity" || { echo "U10d Activity empty tag missing" >&2; exit 1; }
  grep -q 'testTag("backup-catalog-disconnected")' "$backup" || { echo "U10d Backup disconnected state missing" >&2; exit 1; }
  grep -q 'testTag("backup-catalog-empty")' "$backup" || { echo "U10d Backup empty state missing" >&2; exit 1; }
  grep -q 'disconnectedCatalogUsesUnifiedBlockedState' "$test" || { echo "U10d disconnected-catalog test missing" >&2; exit 1; }
  grep -q 'emptyConnectedCatalogUsesUnifiedEmptyState' "$test" || { echo "U10d empty-catalog test missing" >&2; exit 1; }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10d/C7d with blob/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10c materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10d final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10d/C7d with exact blob/reverse verification"
