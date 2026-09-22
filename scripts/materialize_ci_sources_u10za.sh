#!/usr/bin/env bash
# U10za corrective seal: remove the invalid Compose assertion import exposed by U10z androidTest compilation.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10z.sh"
PATCH="$ROOT/.source-parts/U10zaInvalidAssertionImportCorrective.patch"
PATCH_BLOB="bdd8c0a46a4e98a374095b0d4127b16e1e865969"

FILE="app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
FINAL_BLOB="be6566065f594d315e2245faed2e47af458b29c4"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10za assertion-import corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10za patch blob mismatch" >&2
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

  if grep -q '^import androidx\.compose\.ui\.test\.assertDoesNotExist$' "$test_file"; then
    echo "U10za invalid assertDoesNotExist import remains" >&2
    exit 1
  fi
  grep -q 'timeline-ruler-label-1").assertDoesNotExist()' "$test_file" || {
    echo "U10za timeline absence assertion was lost" >&2; exit 1;
  }
  grep -q 'Salvar e exportar").assertDoesNotExist()' "$test_file" || {
    echo "U10za existing member assertion contract was lost" >&2; exit 1;
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10za androidTest assertion import corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10z materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10za final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10za androidTest assertion import corrective"
