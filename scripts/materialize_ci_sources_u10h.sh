#!/usr/bin/env bash
# U10h corrective seal: classify the C7 matrix in fail-closed API36 coverage; canonical checkpoint.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10g.sh"
PATCH="$ROOT/.source-parts/U10hApi36CohesionCoverage.patch"
PATCH_BLOB="43c3e12f460c707a85e91742d8c09ab9b60e2075"
TARGET="scripts/ci_run_api36_regression_groups.sh"
TARGET_BLOB="ae9ad1c44065c0886ba5f1e4260649f105ee917a"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10h API36 coverage patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10h API36 coverage patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q 'CohesionResponsiveAccessibilityInstrumentedTest' "$target" || {
    echo "U10h C7 matrix is not classified in API36 groups" >&2
    exit 1
  }
  awk '
    /GROUP1_CLASSES=\(/ { in_group=1 }
    in_group && /CohesionResponsiveAccessibilityInstrumentedTest/ { found=1 }
    in_group && /^\)/ { exit(found ? 0 : 1) }
    END { if (!found) exit 1 }
  ' "$target" || {
    echo "U10h C7 matrix must belong to API36 group 1" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10h API36 cohesion coverage"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10g materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10h final CI grouping blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10h API36 cohesion coverage"
