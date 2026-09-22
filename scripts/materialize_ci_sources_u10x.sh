#!/usr/bin/env bash
# U10x corrective seal: capture C8 screenshots from Compose surfaces so system/emulator overlays cannot contaminate visual evidence.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10w.sh"
PATCH="$ROOT/.source-parts/U10xComposeSurfaceScreenshotCapture.patch"
PATCH_BLOB="787c0be1787bd02574ae6e98a5dab5c0384d95fe"

FILE="app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
FINAL_BLOB="94d9638f224cabe33cb4c06b1bda8d09fa3d77a7"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10x Compose screenshot capture patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10x screenshot patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$FILE" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$FINAL_BLOB" ]]
}

verify_semantics() {
  local helper="$ROOT/$FILE"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  git -C "$ROOT" diff --check

  grep -q 'import androidx.compose.ui.test.captureToImage' "$helper" || { echo "U10x captureToImage import missing" >&2; exit 1; }
  grep -q 'import androidx.compose.ui.test.onRoot' "$helper" || { echo "U10x onRoot import missing" >&2; exit 1; }
  grep -q 'import androidx.compose.ui.graphics.asAndroidBitmap' "$helper" || { echo "U10x Android bitmap conversion missing" >&2; exit 1; }
  grep -q 'val screenshot = onRoot().captureToImage().asAndroidBitmap()' "$helper" || {
    echo "U10x Compose-surface capture path missing" >&2; exit 1;
  }
  if grep -q 'uiAutomation.takeScreenshot' "$helper"; then
    echo "U10x stale full-display screenshot capture remains" >&2
    exit 1
  fi
  grep -q 'MediaStore.Images.Media.EXTERNAL_CONTENT_URI' "$helper" || { echo "U10x MediaStore publication missing" >&2; exit 1; }

  local expected_count
  expected_count="$(sed -n '/local -a expected=(/,/^[[:space:]]*)/p' "$ci" | grep -c '\.png"')"
  [[ "$expected_count" -eq 20 ]] || { echo "U10x expected 20 retained C8 screenshots, found $expected_count" >&2; exit 1; }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10x clean Compose screenshot capture"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10w materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10x final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10x clean Compose screenshot capture"
