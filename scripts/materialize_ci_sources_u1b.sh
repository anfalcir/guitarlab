#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u1a.sh"
U1B_PATCH="$ROOT/.source-parts/U1bUnifiedDomainTestFixtureCorrective.patch"
U1B_PATCH_SHA256="59810ef249923135b69693a724c539488335980cf705d38440dd76a002c2446b"
TARGET="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedProjectDomainTest.kt"
TARGET_BLOB="82065c2d462d3c9afd6c179a0fa05c4ff72951eb"

hash_file() { git -C "$ROOT" hash-object "$1"; }
u1b_ready() { [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_file "$ROOT/$TARGET")" == "$TARGET_BLOB" ]]; }

[[ -f "$U1B_PATCH" ]] || { echo "Missing U1b source patch: $U1B_PATCH" >&2; exit 1; }
[[ "$(sha256sum "$U1B_PATCH" | awk '{print $1}')" == "$U1B_PATCH_SHA256" ]] || {
    echo "U1b source patch SHA-256 mismatch" >&2
    exit 1
}

if u1b_ready; then
    echo "Source patch chain already materialized through U1b"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U1a materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$U1B_PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$U1B_PATCH"
u1b_ready || { echo "U1b applied but final hash does not match" >&2; exit 1; }
echo "Source patch chain materialized through U1b with verified final hash"
