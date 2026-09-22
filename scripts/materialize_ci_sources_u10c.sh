#!/usr/bin/env bash
# U10c/C7c source seal: visible semantic Activity states backed by a shared status chip.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10b.sh"
PATCH="$ROOT/.source-parts/U10cActivityStateAccessibility.patch"
PATCH_BLOB="d3368ecb218073c02bfe2ddf7a1bf5532945df62"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/ProductUiPrimitives.kt"
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
)

declare -a HASHES=(
  "01fffb5494a3545c5fa254314e2f8c6549855489"
  "c9030e5ec26a62f1d4767dd0507937f0cb0fe3f6"
  "711e55c1c6010a0eded743adb5a446519a2eb7aa"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10c Activity-state patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10c Activity-state patch blob mismatch" >&2
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
  local activity="$ROOT/app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  local test="$ROOT/app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"

  grep -q 'fun ProductStatusChip' "$primitive" || { echo "U10c shared status chip missing" >&2; exit 1; }
  grep -q 'shape = MaterialTheme.shapes.small' "$primitive" || { echo "U10c status chip shape not theme-bound" >&2; exit 1; }
  grep -q 'stateDescription = label' "$primitive" || { echo "U10c status semantics missing" >&2; exit 1; }
  grep -q 'ProductStatusChip(' "$activity" || { echo "U10c Activity does not expose shared status chip" >&2; exit 1; }
  grep -q 'UnifiedOperationState.SUCCEEDED -> "Concluída"' "$activity" || { echo "U10c visible success state missing" >&2; exit 1; }
  grep -q 'UnifiedOperationState.FAILED -> "Falhou"' "$activity" || { echo "U10c visible failure state missing" >&2; exit 1; }
  grep -q 'onNodeWithText("Concluída").assertIsDisplayed()' "$test" || {
    echo "U10c Activity status instrumentation missing" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10c/C7c with blob/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10b materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10c final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10c/C7c with exact blob/reverse verification"
