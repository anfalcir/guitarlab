#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u2b.sh"
PATCH_FILE="$ROOT/.source-parts/U2cUnifiedShellScrollTestCorrective.patch"
PATCH_SHA256="44d2150be8ecf3e42313b7fca7c4364d4d8634ead2de1f39aa9059032d94b940"
TARGET="app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
TARGET_BLOB="649be7c5027e277ef7ff216fd2014b37a4764d61"
hash_file() { git -C "$ROOT" hash-object "$1"; }
ready() { [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]; }
[[ -f "$PATCH_FILE" ]] || { echo "Missing U2c source patch: $PATCH_FILE" >&2; exit 1; }
[[ "$(sha256sum "$PATCH_FILE" | awk '{print $1}')" == "$PATCH_SHA256" ]] || { echo "U2c source patch SHA-256 mismatch" >&2; exit 1; }
if ready; then echo "Source patch chain already materialized through U2c"; exit 0; fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U2b materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$PATCH_FILE" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH_FILE"
ready || { echo "U2c applied but final hash does not match" >&2; exit 1; }
echo "Source patch chain materialized through U2c with verified final hash"
