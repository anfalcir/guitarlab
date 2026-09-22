#!/usr/bin/env bash
# U10a/C7a source seal: one canonical product-wide help model with contextual Studio subset.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8m.sh"
PATCH="$ROOT/.source-parts/U10aProductHelpCohesion.patch"
PATCH_BLOB="c2182ac2a45a92e3be97e09055f233955b6e7943"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
)

declare -a HASHES=(
  "fb1eb7e9faf5e531ea8588e09b30d8b11438206f"
  "c88f702e86c773a24cd349eb245fba9a2011155b"
  "b44757a18debba6afa4acd0e31569fe6ee8c2b35"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10a product-help patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10a product-help patch blob mismatch" >&2
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
  local guide="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt"
  local home="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  local test="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"

  grep -q 'fun GuitarLabUserGuideDialog' "$guide" || { echo "U10a product-wide guide missing" >&2; exit 1; }
  grep -q 'fun StudioUserGuideDialog' "$guide" || { echo "U10a Studio contextual guide missing" >&2; exit 1; }
  grep -q 'GuitarLabGuideContent.studio' "$guide" || { echo "U10a Studio guide is not sourced from canonical content" >&2; exit 1; }
  grep -q 'GuitarLabUserGuideDialog(onDismiss' "$home" || { echo "U10a Home is not routed to product-wide help" >&2; exit 1; }
  if grep -q 'StudioUserGuideDialog(onDismiss' "$home"; then
    echo "U10a Home still routes to Studio-only help" >&2
    exit 1
  fi
  grep -q 'productWideHelpCoversWholeWorkflowAndStudioHelpRemainsContextual' "$test" || {
    echo "U10a help-scope instrumentation missing" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10a/C7a with blob/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8m materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10a final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10a/C7a with exact blob/reverse verification"
