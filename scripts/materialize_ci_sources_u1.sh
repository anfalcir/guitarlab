#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_h37b.sh"
U1_PATCH="$ROOT/.source-parts/U1UnifiedProjectDomain.patch"
U1_PATCH_SHA256="85013ead07723cfa659575792b8f5fc3b3964126e7a144fdc9a397de43e66ffb"

U1_CHECKS=(
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt|a1bc98b6a99d5b9d3839d727673b4399a8de2ee9"
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectValidator.kt|df3d76f554166cf55e7c21349748015f48b73bc4"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectCodec.kt|b5800e0f98b019cf89082c1ccf02a6a2aa9878d6"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectBundleReader.kt|e0e0ac6fe9c687fe9740fc60749bf8307eadb5f5"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectBundleWriter.kt|be441550583a7a4b308521cd1c3dc6a9c6b48a9a"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/FileProjectRepository.kt|ef711bcc110a52e4b23255ca1d2e1046c97a0172"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedProjectDomain.kt|8c1485a553946b94ec879286168bad28368f87f8"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedProjectDomainTest.kt|e6aa9a88d4ebdf2700c4c8b3f99057e6ababb917"
    "core/model/src/test/kotlin/studio/guitarlab/core/model/UnifiedAssetValidationTest.kt|e064be4387aeb22d4628e9046e19e655dc84b62a"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }

u1_ready() {
    local entry relative expected
    for entry in "${U1_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

[[ -f "$U1_PATCH" ]] || { echo "Missing U1 source patch: $U1_PATCH" >&2; exit 1; }
[[ "$(sha256sum "$U1_PATCH" | awk '{print $1}')" == "$U1_PATCH_SHA256" ]] || {
    echo "U1 source patch SHA-256 mismatch" >&2
    exit 1
}

if u1_ready; then
    echo "Source patch chain already materialized through U1"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing H37b materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$U1_PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$U1_PATCH"
u1_ready || { echo "U1 applied but final hashes do not match" >&2; exit 1; }
echo "Source patch chain materialized through U1 with verified final hashes"
