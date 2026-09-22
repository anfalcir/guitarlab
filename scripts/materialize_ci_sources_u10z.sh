#!/usr/bin/env bash
# U10z closure candidate: complete the C8 visual matrix and fix visual defects revealed by clean screenshot review.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10y.sh"
PATCH="$ROOT/.source-parts/U10zC8VisualClosure.patch"
PATCH_BLOB="5335c4d8f04618fdb55f0b23f219c4d862faac5f"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
  "app/src/main/java/studio/guitarlab/app/ui/ProjectShellScaffold.kt"
  "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt"
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "e0c262d1a45ac0fafd51d823d3cd012fca36fd49"
  "9e1c361100973d252d560495cd54b6cff35d03e1"
  "6b7e65c07b23906cc408de23a7c7fd2558ebc756"
  "e6a022728bafba2ecc040c3a53bb0ba1c382a272"
  "af9eccd1178644e1d5a5cb131734548329ec77ab"
  "ea9242f874c597adf1ff2281471f89ee7ee26172"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10z C8 visual closure patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10z C8 visual closure patch blob mismatch" >&2
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
  local backup="$ROOT/app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
  local helper="$ROOT/app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  local lifecycle="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
  local shell="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProjectShellScaffold.kt"
  local studio="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  git -C "$ROOT" diff --check

  grep -q 'testTag?.let(::onNodeWithTag) ?: onRoot()' "$helper" || {
    echo "U10z clean Compose screenshot selection missing" >&2; exit 1;
  }
  if grep -q 'uiAutomation.takeScreenshot' "$helper"; then
    echo "U10z stale full-display screenshot backend remains" >&2
    exit 1
  fi

  grep -q 'background(MaterialTheme.colorScheme.background)' "$shell" || {
    echo "U10z themed project-workspace background missing" >&2; exit 1;
  }
  grep -q 'testTag("project-shell-content")' "$shell" || {
    echo "U10z project-shell content evidence tag missing" >&2; exit 1;
  }

  grep -q 'projectEndFrame <= 0L -> 1' "$studio" || {
    echo "U10z zero-duration timeline ruler guard missing" >&2; exit 1;
  }
  grep -q 'maxWidth < 260.dp -> 2' "$studio" || {
    echo "U10z compact timeline ruler adaptation missing" >&2; exit 1;
  }
  grep -q 'testTag("timeline-ruler-label-$step")' "$studio" || {
    echo "U10z timeline ruler label evidence tags missing" >&2; exit 1;
  }

  grep -q 'timeline-ruler-label-1").assertDoesNotExist()' "$lifecycle" || {
    echo "U10z zero-duration ruler regression assertion missing" >&2; exit 1;
  }
  grep -q 'captureCohesionScreenshot("home-active-operation-phone")' "$lifecycle" || {
    echo "U10z Home active-operation evidence missing" >&2; exit 1;
  }
  grep -q 'captureCohesionScreenshot("studio-reference-update-phone")' "$lifecycle" || {
    echo "U10z Studio prepared-reference evidence missing" >&2; exit 1;
  }

  grep -q 'captureCohesionScreenshot("backup-restore-version")' "$backup" || {
    echo "U10z backup restore evidence missing" >&2; exit 1;
  }
  grep -q 'captureCohesionScreenshot("backup-conflict")' "$backup" || {
    echo "U10z backup conflict evidence missing" >&2; exit 1;
  }

  local expected_count
  expected_count="$(sed -n '/local -a expected=(/,/^[[:space:]]*)/p' "$ci" | grep -c '\.png"')"
  [[ "$expected_count" -eq 24 ]] || {
    echo "U10z expected exactly 24 retained C8 screenshots, found $expected_count" >&2
    exit 1
  }

  grep -q '^verify_executed_classes()' "$ci" || { echo "U10z executed-class proof missing" >&2; exit 1; }
  grep -q '^verify_visual_matrix$' "$ci" || { echo "U10z visual matrix fail-closed guard missing" >&2; exit 1; }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10z complete C8 visual closure candidate"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10y materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10z final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10z complete C8 visual closure candidate"
