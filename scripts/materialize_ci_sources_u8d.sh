#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8c.sh"
PATCH="$ROOT/.source-parts/U8dDurableDriveTransaction.patch"
PATCH_SHA256="aef7a49ac6be99a826c4f82a47a05eea81139a4ab70679ff21ff21ea81b49916"
JOURNAL="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveTransactionJournal.kt"
TEST="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveTransactionJournalTest.kt"
JOURNAL_HASH="0f185b295bb7b25c93c415325fbdb266d2d83513"
TEST_HASH="c8a558a696c1f7aa4225669c99150bbb5cf935f9"

ready() {
  [[ -f "$ROOT/$JOURNAL" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$JOURNAL")" == "$JOURNAL_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8d"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U8c materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8d patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$JOURNAL" && ! -e "$ROOT/$TEST" ]] || { echo "U8d refuses to overwrite unexpected source files" >&2; exit 1; }
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8d final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8d with exact blob verification"
