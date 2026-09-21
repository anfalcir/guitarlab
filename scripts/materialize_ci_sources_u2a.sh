#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u2.sh"
PATCH_FILE="$ROOT/.source-parts/U2aUnifiedShellCompileCorrective.patch"
PATCH_SHA256="99ade732e5ad81a36ef37a42f2e89008f35c0f150d1dddcaca4a275a6040e7ae"
TARGET="app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
TARGET_BLOB="c5ad7bd0801445ded0b9eb7a2b6c0c7c9d43adff"

hash_file() { git -C "$ROOT" hash-object "$1"; }
ready() { [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]; }

[[ -f "$PATCH_FILE" ]] || { echo "Missing U2a source patch: $PATCH_FILE" >&2; exit 1; }
[[ "$(sha256sum "$PATCH_FILE" | awk '{print $1}')" == "$PATCH_SHA256" ]] || { echo "U2a source patch SHA-256 mismatch" >&2; exit 1; }
if ready; then echo "Source patch chain already materialized through U2a"; exit 0; fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U2 materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$PATCH_FILE" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH_FILE"
ready || { echo "U2a applied but final hash does not match" >&2; exit 1; }
echo "Source patch chain materialized through U2a with verified final hash"
