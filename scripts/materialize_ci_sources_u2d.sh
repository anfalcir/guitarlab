#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u2c.sh"
PATCH_FILE="$ROOT/.source-parts/U2dUnifiedShellInputTagCorrective.patch"
PATCH_SHA256="2ba09aac5e6eb7cc4b4f81de4abfad2618d149d1b35e9ad38a80b690fc5bc228"
TARGET="app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt"
TARGET_BLOB="2b8a9deb53cb36419207dd61579ecfff3db8b4df"
hash_file() { git -C "$ROOT" hash-object "$1"; }
ready() { [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]; }
[[ -f "$PATCH_FILE" ]] || { echo "Missing U2d source patch: $PATCH_FILE" >&2; exit 1; }
[[ "$(sha256sum "$PATCH_FILE" | awk '{print $1}')" == "$PATCH_SHA256" ]] || { echo "U2d source patch SHA-256 mismatch" >&2; exit 1; }
if ready; then echo "Source patch chain already materialized through U2d"; exit 0; fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U2c materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$PATCH_FILE" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH_FILE"
ready || { echo "U2d applied but final hash does not match" >&2; exit 1; }
echo "Source patch chain materialized through U2d with verified final hash"
