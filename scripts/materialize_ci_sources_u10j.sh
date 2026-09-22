#!/usr/bin/env bash
# U10j corrective seal: remove invalid Compose test import from the C7 matrix.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10i.sh"
PATCH="$ROOT/.source-parts/U10jComposeTestImportCorrective.patch"
PATCH_BLOB="12c676d230c1d7f06a0a8bda28754f4cfe8b48b0"
TARGET="app/src/androidTest/java/studio/guitarlab/app/CohesionResponsiveAccessibilityInstrumentedTest.kt"
TARGET_BLOB="e0514321282b717b0b8bc67df4d6d749a1fcd915"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10j Compose-test corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10j patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  if grep -q '^import androidx\.compose\.ui\.test\.onNode$' "$target"; then
    echo "U10j invalid Compose onNode import remains" >&2
    exit 1
  fi
  grep -q 'compose.onNode(hasStateDescription("Falhou"))' "$target" || {
    echo "U10j matrix semantics assertion missing" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10j corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10i materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10j final test blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10j corrective"
