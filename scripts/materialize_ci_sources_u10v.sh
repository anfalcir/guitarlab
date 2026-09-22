#!/usr/bin/env bash
# U10v closure candidate: retain the complete C8 visual matrix across major states and representative viewports/themes.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10u.sh"
PATCH="$ROOT/.source-parts/U10vC8VisualMatrixClosure.patch"
PATCH_BLOB="371b94d98bc8aba43964df2975803dcc78658706"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/CohesionResponsiveAccessibilityInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/TargetTabletGeometryInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "e3608a55dd5b049a1568ddc39140eb23d720c618"
  "354b15615e24f9728f24173a67ee208d87031c56"
  "39c1a743bd332204ba7b9e02d7c0436f9c6e5b84"
  "d2e550c7139defb3472b12c783c088e573fb4bd1"
  "31768478b9e2f4136cf57cace31be6735e7eb063"
  "47ddc3d830cd00ed0e206d7727119486d33099b9"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10v C8 visual-matrix patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10v C8 visual-matrix patch blob mismatch" >&2
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
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"
  local shell="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  local lifecycle="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
  local responsive="$ROOT/app/src/androidTest/java/studio/guitarlab/app/CohesionResponsiveAccessibilityInstrumentedTest.kt"
  local tablet="$ROOT/app/src/androidTest/java/studio/guitarlab/app/TargetTabletGeometryInstrumentedTest.kt"
  local activity="$ROOT/app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"

  git -C "$ROOT" diff --check

  grep -q 'captureCohesionScreenshot("prepare-separating-dark")' "$shell" || { echo "U10v Prepare separating evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("prepare-failed-dark")' "$shell" || { echo "U10v Prepare failure evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("export-ready-dark")' "$shell" || { echo "U10v Export ready evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("export-running-light")' "$shell" || { echo "U10v Export running/light evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("home-empty-phone")' "$lifecycle" || { echo "U10v Home empty evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("home-with-project-phone")' "$lifecycle" || { echo "U10v Home populated evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("studio-normal-phone")' "$lifecycle" || { echo "U10v Studio evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("export-error-missing-project-phone")' "$lifecycle" || { echo "U10v Export error evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("activity-mixed")' "$activity" || { echo "U10v Activity mixed-state evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("settings-compact-large-font-light")' "$responsive" || { echo "U10v light/font-scale evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("new-project-compact-large-font-dark")' "$responsive" || { echo "U10v compact-width/font-scale evidence missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("home-target-tablet-landscape")' "$tablet" || { echo "U10v target-tablet evidence missing" >&2; exit 1; }

  local expected_count
  expected_count="$(sed -n '/local -a expected=(/,/^[[:space:]]*)/p' "$ci" | grep -c '.png"')"
  [[ "$expected_count" -eq 20 ]] || { echo "U10v expected exactly 20 retained C8 screenshots, found $expected_count" >&2; exit 1; }

  grep -q '^verify_executed_classes()' "$ci" || { echo "U10v executed-class proof missing" >&2; exit 1; }
  grep -q '^verify_visual_matrix$' "$ci" || { echo "U10v visual matrix fail-closed guard missing" >&2; exit 1; }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10v C8 visual-matrix closure candidate"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10u materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10v final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10v C8 visual-matrix closure candidate"
