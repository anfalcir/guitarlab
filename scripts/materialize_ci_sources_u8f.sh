#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8e.sh"
PATCH="$ROOT/.source-parts/U8fReachabilityGc.patch"
PATCH_SHA256="623e7ba9713202f072b2f24f7a3dfd868574ad0ea99dd849674aacad14f49521"
GC="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveGarbageCollector.kt"
TEST="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveGarbageCollectorTest.kt"
GC_HASH="7cda3e791f2203dc8dafe857b944855c2bacce79"
TEST_HASH="10d282f7881916e1cc2d5e6f2793d776fabd84dc"

ready() {
  [[ -f "$ROOT/$GC" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$GC")" == "$GC_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8f"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U8e materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8f patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$GC" && ! -e "$ROOT/$TEST" ]] || { echo "U8f refuses to overwrite unexpected source files" >&2; exit 1; }
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8f final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8f with exact blob verification"
