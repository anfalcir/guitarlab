#!/usr/bin/env bash
# U10l corrective seal: prove compact Settings reachability through its scroll container.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10k.sh"
PATCH="$ROOT/.source-parts/U10lSettingsReachabilityCorrective.patch"
PATCH_BLOB="471b0956394d2c1ae9da2f7fe59e731fa8e99345"
TARGET="app/src/androidTest/java/studio/guitarlab/app/CohesionResponsiveAccessibilityInstrumentedTest.kt"
TARGET_BLOB="8228d963d92460cd783bf8e2fa8001a0f8416f41"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10l Settings reachability patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10l patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q 'performScrollToNode(hasTestTag("settings-open-calibration"))' "$target" || {
    echo "U10l compact Settings calibration reachability assertion missing" >&2
    exit 1
  }
  grep -q 'onNodeWithTag("settings-open-calibration").assertIsDisplayed()' "$target" || {
    echo "U10l Settings calibration visibility assertion missing" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10l corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10k materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10l final test blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10l corrective"
