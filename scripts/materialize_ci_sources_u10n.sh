#!/usr/bin/env bash
# U10n/C8a corrective seal: fail closed unless the required screenshot artifact matrix is collected; canonical checkpoint.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10m.sh"
PATCH="$ROOT/.source-parts/U10nC8ScreenshotMatrixGuard.patch"
PATCH_BLOB="3ff4e3549cf164e1f037a68702810f4e2add758a"
TARGET="scripts/ci_run_api36_regression_groups.sh"
TARGET_BLOB="7a0c9954471c0de39de7d02dfc8fe39738f8cd37"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10n screenshot-matrix guard patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10n screenshot-matrix guard patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q '^verify_visual_matrix()' "$target" || { echo "U10n visual matrix verifier missing" >&2; exit 1; }
  grep -q '^verify_visual_matrix$' "$target" || { echo "U10n visual matrix verifier is not executed" >&2; exit 1; }

  local -a required=(
    "new-project-phone-dark.png"
    "prepare-ready-dark.png"
    "prepare-search-running-dark.png"
    "settings-dark.png"
    "backup-disconnected.png"
    "backup-empty-connected.png"
    "activity-completed.png"
    "destructive-delete-confirmation.png"
  )
  local name
  for name in "${required[@]}"; do
    grep -q ""$name"" "$target" || {
      echo "U10n required screenshot missing from fail-closed matrix: $name" >&2
      exit 1
    }
  done
  grep -q 'Screenshot obrigatório ausente ou vazio' "$target" || {
    echo "U10n missing explicit fail-closed screenshot error" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10n/C8a screenshot guard"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10m materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10n final CI script blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10n/C8a screenshot guard"
