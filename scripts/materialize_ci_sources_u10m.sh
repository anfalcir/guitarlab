#!/usr/bin/env bash
# U10m/C8a source seal: deterministic screenshot artifacts from existing semantic regression states.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10l.sh"
PATCH="$ROOT/.source-parts/U10mC8ScreenshotArtifacts.patch"
PATCH_BLOB="c71057bf3b84f4901005f12c1f85aa9198b704d2"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/ProjectDeleteConfirmationInstrumentedTest.kt"
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "179c5a82691d61e7a7d66b08bc0910b35792dd42"
  "7b8426fb394dc6143646d7e41d9d1d43d129b595"
  "d5c122fa61352b8bf6df8d906934a858f9fd95fb"
  "59efcbb10027c09968b65fb3a6253bd18b13215a"
  "6633403b6354f6b65f901d11fc75acbe175e3006"
  "e9ce76affa2878f8512d0a061370a3698cba9de4"
  "1e08fd983f70ecd5f0fbbe86303753b746fc2b15"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10m screenshot artifact patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10m screenshot artifact patch blob mismatch" >&2
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
  local shell="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  local settings="$ROOT/app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
  local backup="$ROOT/app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
  local activity="$ROOT/app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
  local destructive="$ROOT/app/src/androidTest/java/studio/guitarlab/app/ProjectDeleteConfirmationInstrumentedTest.kt"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  grep -q 'fun ComposeTestRule.captureCohesionScreenshot' "$helper" || { echo "U10m screenshot helper missing" >&2; exit 1; }
  grep -q 'uiAutomation.takeScreenshot()' "$helper" || { echo "U10m screenshot capture backend missing" >&2; exit 1; }
  grep -q 'File(instrumentation.targetContext.filesDir, "ci-screenshots")' "$helper" || { echo "U10m screenshot storage contract missing" >&2; exit 1; }

  grep -q 'captureCohesionScreenshot("new-project-phone-dark")' "$shell" || { echo "U10m New Project screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("prepare-ready-dark")' "$shell" || { echo "U10m Prepare-ready screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("prepare-search-running-dark")' "$shell" || { echo "U10m Prepare-running screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("settings-dark")' "$settings" || { echo "U10m Settings screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("backup-disconnected")' "$backup" || { echo "U10m Backup blocked screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("backup-empty-connected")' "$backup" || { echo "U10m Backup empty screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("activity-completed")' "$activity" || { echo "U10m Activity screenshot missing" >&2; exit 1; }
  grep -q 'captureCohesionScreenshot("destructive-delete-confirmation")' "$destructive" || { echo "U10m destructive screenshot missing" >&2; exit 1; }

  grep -q '^collect_visual_evidence()' "$ci" || { echo "U10m CI screenshot collector missing" >&2; exit 1; }
  grep -q 'adb exec-out run-as studio.guitarlab.app cat' "$ci" || { echo "U10m run-as screenshot transfer missing" >&2; exit 1; }
  grep -q 'collect_visual_evidence "$slug"' "$ci" || { echo "U10m collector not wired into each API36 group" >&2; exit 1; }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10m/C8a screenshot artifacts"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10l materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10m final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10m/C8a screenshot artifacts"
