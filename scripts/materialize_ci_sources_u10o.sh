#!/usr/bin/env bash
# U10o corrective seal: use the junit4 ComposeTestRule type for C8 screenshot capture.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10n.sh"
PATCH="$ROOT/.source-parts/U10oScreenshotRuleImportCorrective.patch"
PATCH_BLOB="9fcbfe6576ef1a0573e64119697850e3f2fb380a"
TARGET="app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
TARGET_BLOB="8d833ff9ff52b5f391249ea3336a1d3e054ebeb1"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10o screenshot-rule corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10o patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q '^import androidx\.compose\.ui\.test\.junit4\.ComposeTestRule$' "$target" || {
    echo "U10o junit4 ComposeTestRule import missing" >&2
    exit 1
  }
  if grep -q '^import androidx\.compose\.ui\.test\.ComposeTestRule$' "$target"; then
    echo "U10o stale invalid ComposeTestRule import remains" >&2
    exit 1
  fi
  grep -q 'uiAutomation.takeScreenshot()' "$target" || {
    echo "U10o screenshot backend regression" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10o screenshot-rule corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10n materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10o final screenshot helper blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10o screenshot-rule corrective"
