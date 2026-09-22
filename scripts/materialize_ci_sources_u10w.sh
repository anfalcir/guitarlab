#!/usr/bin/env bash
# U10w corrective seal: use the deterministic Prepare running-state tag for the C8 separation screenshot.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10v.sh"
PATCH="$ROOT/.source-parts/U10wSeparationScreenshotSelector.patch"
PATCH_BLOB="1bceb25dcb0fa0ec883e3dc79b63bc55ccf40b2a"

FILE="app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
FINAL_BLOB="0f4c5250cc668a4ceea8f5addc9e0eff657f17df"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10w separation screenshot selector patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10w selector patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$FILE" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$FINAL_BLOB" ]]
}

verify_semantics() {
  local test_file="$ROOT/$FILE"
  git -C "$ROOT" diff --check
  grep -q 'onNodeWithTag("prepare-safe-state-separation-running").assertIsDisplayed()' "$test_file" || {
    echo "U10w deterministic separation-running selector missing" >&2; exit 1;
  }
  grep -q 'captureCohesionScreenshot("prepare-separating-dark")' "$test_file" || {
    echo "U10w separation screenshot evidence missing" >&2; exit 1;
  }
  if grep -q 'onNodeWithText("Separando a música em seis faixas…").performScrollTo().assertIsDisplayed()' "$test_file"; then
    echo "U10w stale ambiguous separation selector remains" >&2
    exit 1
  fi
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10w deterministic separation screenshot selector"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10v materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10w final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10w deterministic separation screenshot selector"
