#!/usr/bin/env bash
# U10k corrective seal: remove invalid Compose assertion import from Activity deep-link regression; canonical checkpoint.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10j.sh"
PATCH="$ROOT/.source-parts/U10kComposeAssertionImportCorrective.patch"
PATCH_BLOB="c094b759444961ce981ea453c658aa0a3573e491"
TARGET="app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
TARGET_BLOB="860640593be0518298d9f33d4305eb9525942a37"

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10k Compose assertion corrective patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10k patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  [[ -f "$ROOT/$TARGET" ]] &&
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

verify_semantics() {
  local target="$ROOT/$TARGET"
  if grep -q '^import androidx\.compose\.ui\.test\.assertDoesNotExist$' "$target"; then
    echo "U10k invalid Compose assertDoesNotExist import remains" >&2
    exit 1
  fi
  grep -q 'onNodeWithText("project-notification", substring = true).assertDoesNotExist()' "$target" || {
    echo "U10k project-id privacy assertion missing" >&2
    exit 1
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10k corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10j materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10k final test blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10k corrective"
