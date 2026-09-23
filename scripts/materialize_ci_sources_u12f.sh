#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12d.sh"
PATCH="$ROOT/.source-parts/U12fBackupBusyFeedbackTestCorrective.patch"
PATCH_BLOB="de08a103262d5c7e48eb09796a0d1e4a89bf0a1b"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
)

declare -a HASHES=(
  "61b5159bce8e4cb364268839148b43f4dacab75d"
  "352d420b5dfb7da9af6a43be4d3ee7d09d8286a5"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'BackupBusyFeedback(state.busyLabel)' "$ROOT/${FILES[0]}"
  grep -q 'internal fun BackupBusyFeedback(label: String?)' "$ROOT/${FILES[0]}"
  grep -q 'BackupBusyFeedback("Backup total em andamento…")' "$ROOT/${FILES[1]}"
  grep -q 'backup-busy-feedback' "$ROOT/${FILES[1]}"
}

[[ -f "$PATCH" ]] || { echo "Missing U12f backup busy-feedback corrective patch" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12f patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --unidiff-zero --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U12f backup busy-feedback test corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12d materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --unidiff-zero --check "$PATCH"
git -C "$ROOT" apply --unidiff-zero "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12f final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --unidiff-zero --check --reverse "$PATCH"
echo "Source patch chain materialized through U12f backup busy-feedback test corrective"
