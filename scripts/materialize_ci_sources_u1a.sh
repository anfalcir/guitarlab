#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u1.sh"
U1A_PATCH="$ROOT/.source-parts/U1aUnifiedDomainCompileCorrective.patch"
U1A_PATCH_SHA256="f6dc617e5bf17d3fc81976cd73686640bba17bb869b1d4bb45ca8464f420bc8a"
TARGET="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedProjectDomain.kt"
TARGET_BLOB="27fedd8cb7c6f9d9ec04d83f8fee333f9062db53"

hash_file() { git -C "$ROOT" hash-object "$1"; }

u1a_ready() {
    [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]
}

[[ -f "$U1A_PATCH" ]] || { echo "Missing U1a source patch: $U1A_PATCH" >&2; exit 1; }
[[ "$(sha256sum "$U1A_PATCH" | awk '{print $1}')" == "$U1A_PATCH_SHA256" ]] || {
    echo "U1a source patch SHA-256 mismatch" >&2
    exit 1
}

if u1a_ready; then
    echo "Source patch chain already materialized through U1a"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U1 materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$U1A_PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$U1A_PATCH"
u1a_ready || { echo "U1a applied but final hash does not match" >&2; exit 1; }
echo "Source patch chain materialized through U1a with verified final hash"
