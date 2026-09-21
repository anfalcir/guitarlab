#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u4.sh"
PATCH_B64_PREFIX="$ROOT/.source-parts/U5PreparedStudio.patch.gz.b64.part"
PATCH_B64_SHA256="cd55519ad275594abdf771a7f972d39edf7d90f22851df2fb66e4a1f22d57c6a"
PATCH_GZ_SHA256="4c5c140b416ec3d437b13f9ce1f2a2b077c86e76c4b6bf464e9e2ba3de7082e5"
PATCH_SHA256="0991cfccf7d4ff4a3445e4b6e8cbd9704fc817c9acf387ab0a81714d3326bda7"

U4_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/GuitarLabApplication.kt|af9f72cf916c6f1cba6d9674cef11693dc3acd5f"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|e906291ffeb44afd44c3392ad993ca6a835596d2"
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|34eb8f4f20e80065e35b53a1b4ee0a0e1d983b2d"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectManagedMediaStore.kt|9934714f4c10214734cf359cb89c2eb5669c2ff3"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/StemSetProjectPublisher.kt|ff1eca605a3d9fe9bf7c97e8cb2331ae2a3b97a9"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/StemSetProjectPublisherTest.kt|d2fbbb13202aaf0c67ceb04b8102aaafbeb12e33"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/StemSetRecoveryTest.kt|a14190198ec62ed9d8f2f7539c2ad2622c25cbed"
)

U5_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt|679d8557d77fbb304a71b0967db62b4064082722"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|3dd997087b3138c9c95befe2bf226e50044ee04e"
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt|a72b17e2360a7e15547b37af73a666f2ad3e305c"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|eb49248319d8814748c617e3f55cef55daf47acc"
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|56a690b5e63851861dfd7389e90a1b4e0669ca04"
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectValidator.kt|22445da5c90e0538c7f9af85fcfc28161c869554"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectManagedMediaStore.kt|b6421ddafac0d2a6c81ea83194facd8a2912507a"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/PreparedReferencePipeline.kt|fbd08dd485abaad8707d670087143194e022679d"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferencePipelineTest.kt|f37a84247da6787314ca425f35219b32a0b44373"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }
checks_ready() {
    local array_name="$1" entry relative expected
    local -n checks_ref="$array_name"
    for entry in "${checks_ref[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}
u5_ready() {
    local entry relative expected
    for entry in "${U5_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

if u5_ready; then
    echo "Source patch chain already materialized through U5"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U4 materializer: $PREVIOUS" >&2; exit 1; }
PATCH_PARTS=("${PATCH_B64_PREFIX}"*)
[[ ${#PATCH_PARTS[@]} -gt 0 && -f "${PATCH_PARTS[0]}" ]] || { echo "Missing U5 source patch payload parts" >&2; exit 1; }
if checks_ready U4_CHECKS; then
    echo "Verified exact U4 terminal source; skipping destructive re-materialization"
else
    bash "$PREVIOUS"
fi

TMP_B64="$(mktemp)"
cat "${PATCH_PARTS[@]}" > "$TMP_B64"
[[ "$(sha256sum "$TMP_B64" | awk '{print $1}')" == "$PATCH_B64_SHA256" ]] || { echo "U5 base64 payload SHA-256 mismatch" >&2; exit 1; }
TMP_GZ="$(mktemp)"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_B64" "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$TMP_B64" > "$TMP_GZ"
[[ "$(sha256sum "$TMP_GZ" | awk '{print $1}')" == "$PATCH_GZ_SHA256" ]] || { echo "U5 compressed patch SHA-256 mismatch" >&2; exit 1; }
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
[[ "$(sha256sum "$TMP_PATCH" | awk '{print $1}')" == "$PATCH_SHA256" ]] || { echo "U5 decompressed patch SHA-256 mismatch" >&2; exit 1; }
patch --dry-run -p1 -d "$ROOT" < "$TMP_PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$TMP_PATCH"
u5_ready || { echo "U5 applied but final hashes do not match" >&2; exit 1; }
echo "Source patch chain materialized through U5 with verified final hashes"
