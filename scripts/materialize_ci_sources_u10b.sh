#!/usr/bin/env bash
# U10b/C7b source seal: shared product section geometry for Settings and Backup.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10a.sh"
PATCH="$ROOT/.source-parts/U10bSharedProductSections.patch"
PATCH_BLOB="87d98f67c30ccc749dfe6c1db94f076ca7cd40fa"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/ProductUiPrimitives.kt"
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
)

declare -a HASHES=(
  "b6808e126ff58a56e36777dd3005731b23226d28"
  "c354924b02bf0c1c5f9a86b865cb3c385f78e2a8"
  "a0c2ec9e10bebc3d2f53b678fd6c199d9a671ba2"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10b shared-section patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10b shared-section patch blob mismatch" >&2
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
  local settings="$ROOT/app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  local backup="$ROOT/app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"

  grep -q 'fun ProductSectionCard' "$primitive" || { echo "U10b shared ProductSectionCard missing" >&2; exit 1; }
  grep -q 'shape = MaterialTheme.shapes.large' "$primitive" || { echo "U10b major-panel shape is not theme-bound" >&2; exit 1; }
  grep -q 'ProductSectionCard(' "$settings" || { echo "U10b Settings did not adopt shared section card" >&2; exit 1; }
  grep -q 'ProductSectionCard(title = title, subtitle = subtitle)' "$backup" || { echo "U10b Backup did not adopt shared section card" >&2; exit 1; }
  if grep -q 'RoundedCornerShape(14.dp)' "$settings"; then
    echo "U10b stale 14dp Settings panel shape remains" >&2
    exit 1
  fi
  if grep -q 'RoundedCornerShape(12.dp)' "$backup"; then
    echo "U10b stale 12dp Backup section shape remains" >&2
    exit 1
  fi
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10b/C7b with blob/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10a materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10b final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10b/C7b with exact blob/reverse verification"
