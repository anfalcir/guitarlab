#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u6b.sh"
PATCH="$ROOT/.source-parts/U6cApi36ImportCorrective.patch"
PATCH_SHA256="b8baa384fb59d68a6a089f4fecd45aa20545f5c6d5b4f90155780b6e70c896aa"
TARGET="app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
TARGET_HASH="528ea83199fa0465ad4518da12abce45302ba473"

hash_file() { git -C "$ROOT" hash-object "$1"; }

if [[ -f "$ROOT/$TARGET" && "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]]; then
  echo "Source patch chain already materialized through U6c"
  exit 0
fi

bash "$PREVIOUS"
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
[[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]] || {
  echo "U6c applied but final hash does not match" >&2
  exit 1
}
echo "Source patch chain materialized through U6c with verified final hash"
