#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u2a.sh"
PATCH_FILE="$ROOT/.source-parts/U2bUnifiedShellTestImportCorrective.patch"
PATCH_SHA256="838cf84953a7f544caf247ff01977918f3351e1fa2a5ae6d0cdedf1d5a2d8237"
TARGET="app/src/test/java/studio/guitarlab/app/ui/UnifiedProjectShellPolicyTest.kt"
TARGET_BLOB="757adc3ca6aeb25497f616655ce8eadb2ef13164"
hash_file() { git -C "$ROOT" hash-object "$1"; }
ready() { [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]; }
[[ -f "$PATCH_FILE" ]] || { echo "Missing U2b source patch: $PATCH_FILE" >&2; exit 1; }
[[ "$(sha256sum "$PATCH_FILE" | awk '{print $1}')" == "$PATCH_SHA256" ]] || { echo "U2b source patch SHA-256 mismatch" >&2; exit 1; }
if ready; then echo "Source patch chain already materialized through U2b"; exit 0; fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U2a materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$PATCH_FILE" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH_FILE"
ready || { echo "U2b applied but final hash does not match" >&2; exit 1; }
echo "Source patch chain materialized through U2b with verified final hash"
