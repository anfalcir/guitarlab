#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u6a.sh"
PATCH="$ROOT/.source-parts/U6bApi36CompileCorrective.patch"
PATCH_SHA256="b55c8c0a7e47fd68af1e828587224797fc34b64484af393c3ef78acb09919cd6"
TARGET="app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
TARGET_HASH="76ad445197325d34e6beb803fb078d0cc2b59003"

hash_file() { git -C "$ROOT" hash-object "$1"; }

if [[ -f "$ROOT/$TARGET" && "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]]; then
  echo "Source patch chain already materialized through U6b"
  exit 0
fi

bash "$PREVIOUS"
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
[[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_HASH" ]] || {
  echo "U6b applied but final hash does not match" >&2
  exit 1
}
echo "Source patch chain materialized through U6b with verified final hash"
