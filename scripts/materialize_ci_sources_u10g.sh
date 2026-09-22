#!/usr/bin/env bash
# U10g/C7 responsive-accessibility matrix seal; canonical checkpoint.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10f.sh"
PATCH="$ROOT/.source-parts/U10gResponsiveAccessibilityMatrix.patch"
PATCH_BLOB="d3594b86f678ca65c0ee45683d32790a74561c1e"
TARGET="app/src/androidTest/java/studio/guitarlab/app/CohesionResponsiveAccessibilityInstrumentedTest.kt"
TARGET_BLOB="a51eaceab0f91e72800350fe7376b6d00bdc42cd"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10g responsive-accessibility patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10g responsive-accessibility patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q 'compactLargeFontNewProjectKeepsAllCreationIntentsAndPrimaryActionDiscoverable' "$target" || {
    echo "U10g New Project matrix case missing" >&2; exit 1;
  }
  grep -q 'compactLargeFontSettingsKeepsPrimaryAndDeepControlsReachableInLightTheme' "$target" || {
    echo "U10g Settings matrix case missing" >&2; exit 1;
  }
  grep -q 'compactLargeFontBackupKeepsBlockedCatalogMessageReachable' "$target" || {
    echo "U10g Backup matrix case missing" >&2; exit 1;
  }
  grep -q 'compactLargeFontPreparePreservesProjectNavigationAndJourneyState' "$target" || {
    echo "U10g Prepare matrix case missing" >&2; exit 1;
  }
  grep -q 'wideProjectShellKeepsSingleNavigationGrammarInDarkTheme' "$target" || {
    echo "U10g wide shell matrix case missing" >&2; exit 1;
  }
  grep -q 'statusChipPublishesTextAndStateSemanticsWithoutDependingOnColor' "$target" || {
    echo "U10g status semantics case missing" >&2; exit 1;
  }
  grep -q 'fontScale = 1.3f' "$target" || {
    echo "U10g representative large font scale missing" >&2; exit 1;
  }
  grep -q 'darkTheme = false' "$target" || {
    echo "U10g light-theme coverage missing" >&2; exit 1;
  }
  grep -q 'darkTheme = true' "$target" || {
    echo "U10g dark-theme coverage missing" >&2; exit 1;
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10g/C7 responsive-accessibility matrix"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10f materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10g final test blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10g/C7 responsive-accessibility matrix"
