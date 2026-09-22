#!/usr/bin/env bash
# U10t corrective seal: run every API36 regression group through deterministic JUnit suites.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10r.sh"
PATCH="$ROOT/.source-parts/U10tApi36SuiteExecution.patch"
PATCH_BLOB="7613bee8901ee73ef65e9e40546c954047069202"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/Api36RegressionSuites.kt"
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "c53c227bb5d8075b93a5744b98c692f5ab606b29"
  "cf6fab138af9ad3dbd755cb7b35d3c4514c8a14d"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10t API36 suite execution patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10t API36 suite execution patch blob mismatch" >&2
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
  local suites="$ROOT/app/src/androidTest/java/studio/guitarlab/app/Api36RegressionSuites.kt"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  git -C "$ROOT" diff --check

  [[ "$(grep -c '^class Api36.*Suite$' "$suites")" -eq 5 ]] || {
    echo "U10t expected exactly five API36 suite aggregators" >&2; exit 1;
  }
  grep -q '@RunWith(Suite::class)' "$suites" || {
    echo "U10t JUnit Suite runner contract missing" >&2; exit 1;
  }
  grep -q '^suite_filter()' "$ci" || {
    echo "U10t suite filter missing" >&2; exit 1;
  }
  if grep -q '^class_filter()' "$ci"; then
    echo "U10t stale comma-separated multi-class filter remains" >&2
    exit 1
  fi
  grep -q '^verify_executed_classes()' "$ci" || {
    echo "U10t executed-class fail-closed proof missing" >&2; exit 1;
  }
  grep -q 'Missing executed classes:' "$ci" || {
    echo "U10t missing-class diagnostic absent" >&2; exit 1;
  }
  for suite in \
    Api36ProjectNavigationSuite \
    Api36StudioPracticeSuite \
    Api36ImportControlsSettingsSuite \
    Api36ExportBackupMasterSuite \
    Api36TabletGeometrySuite; do
    grep -q "$suite" "$ci" || {
      echo "U10t suite not wired in CI: $suite" >&2; exit 1;
    }
  done
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10t deterministic API36 suites"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10r materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10t final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10t deterministic API36 suites"
