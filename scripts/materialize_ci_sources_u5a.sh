#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u5.sh"
PATCH="$ROOT/.source-parts/U5aSmartCastCorrective.patch"
PATCH_SHA256="c7289cd267f8bbc03122cabf8c655d1303c9a9cdc4d02226c20f6abf27c9117f"
TARGET="core/project/src/main/kotlin/studio/guitarlab/core/project/PreparedReferencePipeline.kt"
TARGET_HASH="b09d04deda1016903d19b5c661553bfdce639bad"

hash_file() { git -C "$ROOT" hash-object "$1"; }

if [[ -f "$ROOT/$TARGET" && "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]]; then
  echo "Source patch chain already materialized through U5a"
  exit 0
fi

bash "$PREVIOUS"
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
patch --dry-run -p1 -d "$ROOT" < "$PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH"
[[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]] || {
  echo "U5a applied but final hash does not match" >&2
  exit 1
}
echo "Source patch chain materialized through U5a with verified final hash"
