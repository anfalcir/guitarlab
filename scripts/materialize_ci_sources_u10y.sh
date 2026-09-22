#!/usr/bin/env bash
# U10y corrective seal: keep clean Compose screenshots deterministic when popups/dialogs create multiple roots.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10x.sh"
PATCH="$ROOT/.source-parts/U10yDeterministicComposeScreenshotRoots.patch"
PATCH_BLOB="4c341c12aeb65f7124e236b4ee4655f497e61fce"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/ProjectDeleteConfirmationInstrumentedTest.kt"
)

declare -a HASHES=(
  "ff3a6de20f61e3d9282d9f0516e37176c35dd156"
  "73b3b2cfea9f6903153f1d184bdfe39131baad9d"
  "2efdd9da9fe16c5d9f21ac122ea2c83c2afb81f8"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10y deterministic screenshot-roots patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10y screenshot-roots patch blob mismatch" >&2
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
  local helper="$ROOT/app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  local lifecycle="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
  local destructive="$ROOT/app/src/androidTest/java/studio/guitarlab/app/ProjectDeleteConfirmationInstrumentedTest.kt"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  git -C "$ROOT" diff --check

  grep -q 'fun ComposeTestRule.captureCohesionScreenshot(name: String, testTag: String? = null)' "$helper" || {
    echo "U10y optional screenshot testTag contract missing" >&2; exit 1;
  }
  grep -q 'testTag?.let(::onNodeWithTag) ?: onRoot()' "$helper" || {
    echo "U10y tagged/root screenshot selection missing" >&2; exit 1;
  }
  grep -q 'captureCohesionScreenshot("destructive-delete-confirmation", testTag = "home-delete-confirmation")' "$destructive" || {
    echo "U10y destructive dialog targeted capture missing" >&2; exit 1;
  }

  local capture_line menu_line
  capture_line="$(grep -n 'captureCohesionScreenshot("home-with-project-phone")' "$lifecycle" | cut -d: -f1)"
  menu_line="$(grep -n 'onNodeWithContentDescription(menuDescription)' "$lifecycle" | tail -1 | cut -d: -f1)"
  [[ -n "$capture_line" && -n "$menu_line" && "$capture_line" -lt "$menu_line" ]] || {
    echo "U10y Home screenshot is not captured before popup creation" >&2; exit 1;
  }

  local expected_count
  expected_count="$(sed -n '/local -a expected=(/,/^[[:space:]]*)/p' "$ci" | grep -c '\.png"')"
  [[ "$expected_count" -eq 20 ]] || { echo "U10y expected 20 retained C8 screenshots, found $expected_count" >&2; exit 1; }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10y deterministic Compose screenshot roots"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10x materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10y final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10y deterministic Compose screenshot roots"
