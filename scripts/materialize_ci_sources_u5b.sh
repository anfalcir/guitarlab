#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u5a.sh"
PATCH="$ROOT/.source-parts/U5bWorkspaceSmartCastCorrective.patch"
PATCH_SHA256="968b727ba3ee65e29ce840445a063023d5418cb5e480eee70f5337162bf917d5"
TARGET="app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
TARGET_HASH="19a9f6b1eebdb1936a589622aabbfff1403b7bbb"

hash_file() { git -C "$ROOT" hash-object "$1"; }

if [[ -f "$ROOT/$TARGET" && "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]]; then
  echo "Source patch chain already materialized through U5b"
  exit 0
fi

bash "$PREVIOUS"
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
patch --dry-run -p1 -d "$ROOT" < "$PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH"
[[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]] || {
  echo "U5b applied but final hash does not match" >&2
  exit 1
}
echo "Source patch chain materialized through U5b with verified final hash"
