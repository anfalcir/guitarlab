#!/usr/bin/env bash
# U10e corrective seal: restore Backup controls; canonical C7a-C7d checkpoint.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10d.sh"
PATCH="$ROOT/.source-parts/U10eBackupHelpersCorrective.patch"
PATCH_BLOB="732fb33dd9951188e7eb42d2c6a5553d874f24ed"
TARGET="app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
TARGET_BLOB="5c839b88534a7c6fc56c58ae5fd6b1fc017a3dbd"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10e Backup corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10e Backup corrective patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q 'private fun ToggleRow(' "$target" || { echo "U10e ToggleRow missing" >&2; exit 1; }
  grep -q 'private fun <T> ChoiceRow(' "$target" || { echo "U10e ChoiceRow missing" >&2; exit 1; }
  grep -q 'ProductSectionCard(title = title, subtitle = subtitle)' "$target" || {
    echo "U10e shared Backup section regression" >&2
    exit 1
  }
  grep -q 'testTag("backup-catalog-empty")' "$target" || {
    echo "U10e U10d Backup empty-state regression" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10e corrective with exact blob/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10d materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10e final Backup blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10e corrective with exact blob/reverse verification"
