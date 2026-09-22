#!/usr/bin/env bash
# U10q corrective seal: publish screenshots through MediaStore and prove every grouped class executed.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10p.sh"
PATCH="$ROOT/.source-parts/U10qScreenshotMediaStoreAndExecutionCoverage.patch"
PATCH_BLOB="544d61a231aae65d8d9b23b18526b5f64d8f4757"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "9e4b4dbf4516433ca11267724b2fb5d047fd589b"
  "4204e6268a2f10a937b8122bc4d6537b84e9ce00"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10q screenshot/coverage corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10q screenshot/coverage patch blob mismatch" >&2
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
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  grep -q 'uiAutomation.takeScreenshot()' "$helper" || {
    echo "U10q UiAutomation screenshot backend missing" >&2; exit 1;
  }
  grep -q 'MediaStore.Images.Media.RELATIVE_PATH' "$helper" || {
    echo "U10q MediaStore publication missing" >&2; exit 1;
  }
  if grep -q 'executeShellCommand' "$helper"; then
    echo "U10q stale shell screenshot backend remains" >&2
    exit 1
  fi
  grep -q 'remote_root="/sdcard/Pictures/guitarlab-ci-screenshots"' "$ci" || {
    echo "U10q CI MediaStore collection path missing" >&2; exit 1;
  }
  grep -q '^verify_executed_classes()' "$ci" || {
    echo "U10q executed-class proof missing" >&2; exit 1;
  }
  grep -q 'Missing executed classes:' "$ci" || {
    echo "U10q missing-class fail-closed diagnostic absent" >&2; exit 1;
  }
  grep -q '^verify_visual_matrix$' "$ci" || {
    echo "U10q visual matrix fail-closed guard regression" >&2; exit 1;
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10q screenshot publication and execution coverage"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10p materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10q final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10q screenshot publication and execution coverage"
