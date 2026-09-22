#!/usr/bin/env bash
# U10zb corrective seal: align the Studio phone ruler assertion with the adaptive two-label compact-width contract.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10za.sh"
PATCH="$ROOT/.source-parts/U10zbAdaptiveRulerAssertion.patch"
PATCH_BLOB="66eae134a6d868691fdf9010a994ca99e621a5ba"

FILE="app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
FINAL_BLOB="970f932515d766a77d94f69331915ec56edace2d"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10zb adaptive ruler assertion patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10zb patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$FILE" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$FINAL_BLOB" ]]
}

verify_semantics() {
  local test_file="$ROOT/$FILE"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  git -C "$ROOT" diff --check

  grep -q 'timeline-ruler-label-0").assertIsDisplayed()' "$test_file" || {
    echo "U10zb first compact ruler label assertion missing" >&2; exit 1;
  }
  grep -q 'timeline-ruler-label-1").assertIsDisplayed()' "$test_file" || {
    echo "U10zb second compact ruler label assertion missing" >&2; exit 1;
  }
  grep -q 'timeline-ruler-label-2").assertDoesNotExist()' "$test_file" || {
    echo "U10zb compact ruler crowding guard missing" >&2; exit 1;
  }
  if grep -q 'timeline-ruler-label-1").assertDoesNotExist()' "$test_file"; then
    echo "U10zb stale one-label ruler assertion remains" >&2
    exit 1
  fi

  local expected_count
  expected_count="$(sed -n '/local -a expected=(/,/^[[:space:]]*)/p' "$ci" | grep -c '\.png"')"
  [[ "$expected_count" -eq 24 ]] || {
    echo "U10zb expected 24 retained C8 screenshots, found $expected_count" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10zb adaptive Studio ruler assertion"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10za materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10zb final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10zb adaptive Studio ruler assertion"
