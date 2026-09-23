#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12a.sh"
PATCH="$ROOT/.source-parts/U12dApi36ComposeStateCorrective.patch"
PATCH_BLOB="0fdcfb0a0b218313109d21d884f07ab8924f9d68"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
TARGET_BLOB="5a22a48e4c4f0c25b95934b5affcee01b7b67145"

ready() {
  [[ -f "$TARGET" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'val searchBusy = androidx.compose.runtime.mutableStateOf(false)' "$TARGET"
  grep -q 'searchBusy = searchBusy.value' "$TARGET"
  grep -q 'searchBusy.value = true' "$TARGET"
}

[[ -f "$PATCH" ]] || { echo "Missing U12d API36 Compose-state corrective patch" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12d patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U12d API36 Compose-state corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12a materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12d final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U12d API36 Compose-state corrective"
