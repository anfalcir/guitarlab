#!/usr/bin/env bash
# U10u corrective seal: repair latent API36 test contracts exposed by deterministic suite execution.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10t.sh"
PATCH="$ROOT/.source-parts/U10uApi36LatentTestCorrectives.patch"
PATCH_BLOB="8b299c0739a25f4dde81171ee117c93127778385"

FILE="app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
FINAL_BLOB="7512952c2005a74adb4e91345f657d81c14df7b5"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10u API36 latent-test corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10u API36 latent-test patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$FILE" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$FINAL_BLOB" ]]
}

verify_semantics() {
  local test_file="$ROOT/$FILE"
  git -C "$ROOT" diff --check
  grep -q 'sourceAssetId = "source"' "$test_file" || {
    echo "U10u ready-fixture source contract missing" >&2; exit 1;
  }
  grep -q 'sourceReplacementModeReturnsPickersWithoutDroppingProtectedSource' "$test_file" || {
    echo "U10u source-replacement split regression missing" >&2; exit 1;
  }
  grep -q 'technicalAssetDetailsStayCollapsedUntilExplicitlyExpanded' "$test_file" || {
    echo "U10u technical-details split regression missing" >&2; exit 1;
  }
  grep -q 'studioHelpRemainsContextual' "$test_file" || {
    echo "U10u Studio-help split regression missing" >&2; exit 1;
  }
  grep -q 'onNodeWithTag("prepare-safe-state-separation-failed")' "$test_file" || {
    echo "U10u deterministic separation-failure selector missing" >&2; exit 1;
  }
  [[ "$(grep -c '@Test' "$test_file")" -eq 16 ]] || {
    echo "U10u expected 16 UnifiedProjectShell instrumented tests" >&2; exit 1;
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10u API36 latent-test correctives"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10t materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10u final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10u API36 latent-test correctives"
