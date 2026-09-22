#!/usr/bin/env bash
# U10f corrective seal: align lifecycle regression with the unified GuitarLab guide; canonical checkpoint.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10e.sh"
PATCH="$ROOT/.source-parts/U10fUnifiedGuideLifecycleCorrective.patch"
PATCH_BLOB="afe45061ee266ea8a2709a536bba48dad70e0d7f"
TARGET="app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
TARGET_BLOB="6f60a15195e4f17b981fdafa30ac4f2399acf2ed"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10f lifecycle-help corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10f lifecycle-help patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  grep -q 'onNodeWithText("Guia do GuitarLab").assertIsDisplayed()' "$target" || {
    echo "U10f unified guide title assertion missing" >&2
    exit 1
  }
  grep -q 'onNodeWithText("Preparar").performScrollTo().assertIsDisplayed()' "$target" || {
    echo "U10f Prepare guide assertion missing" >&2
    exit 1
  }
  grep -q 'onNodeWithText("Atividade, nuvem e backup").performScrollTo().assertIsDisplayed()' "$target" || {
    echo "U10f cloud/activity guide assertion missing" >&2
    exit 1
  }
  if grep -q 'Guia rápido do GuitarLab' "$target"; then
    echo "U10f stale guide title remains" >&2
    exit 1
  fi
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10f with exact blob/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10e materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10f lifecycle test blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10f with exact blob/reverse verification"
