#!/usr/bin/env bash
# U10i/C7 copy/privacy seal: canonical study labels and no internal project id in Activity.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10h.sh"
PATCH="$ROOT/.source-parts/U10iProductCopyPrivacy.patch"
PATCH_BLOB="158d402d9e79c5dac92a6ab3aaf92fc652d694de"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/ui/ProjectExportService.kt"
  "app/src/main/java/studio/guitarlab/app/ui/ProjectMediaDialog.kt"
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
)

declare -a HASHES=(
  "069944c7ae82a87956710d06cc8964e629ee0281"
  "ca26d831fe5aff0d523c0726bde4c80d10fefddd"
  "d798b54e6b8bdf7f6c01c094a58d16d89cd6b1ab"
  "ba4582926fb3574e374854fb7741336f1b955daa"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10i product-copy patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10i product-copy patch blob mismatch" >&2
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
  local export="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProjectExportService.kt"
  local media="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProjectMediaDialog.kt"
  local activity="$ROOT/app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  local test="$ROOT/app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"

  grep -q 'BACKING("Base sem guitarra"' "$export" || { echo "U10i canonical backing label missing" >&2; exit 1; }
  grep -q 'GUITAR("Guitarra de referência"' "$export" || { echo "U10i canonical guitar label missing" >&2; exit 1; }
  grep -q 'MediaGroup("Faixas separadas"' "$media" || { echo "U10i separated-track copy missing" >&2; exit 1; }
  if grep -q 'Stems preparados' "$media"; then
    echo "U10i stale stems heading remains" >&2
    exit 1
  fi
  if grep -q 'Text("Projeto \$it"' "$activity"; then
    echo "U10i Activity still exposes internal project id" >&2
    exit 1
  fi
  grep -q 'project-notification", substring = true).assertDoesNotExist()' "$test" || {
    echo "U10i Activity project-id privacy regression missing" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10i/C7 copy/privacy"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10h materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10i final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10i/C7 copy/privacy"
