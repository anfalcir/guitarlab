#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_h37.sh"
H37A_PART="$ROOT/.source-parts/H37aDriveCompileCorrective.patch.gz.b64"

H37A_ARCHIVE_SHA256="77f5d9ad6f5ebced2e9763dc13ed37a7ac7b4da3412f40c5f6498e813e5b4565"
H37A_PATCH_SHA256="5b623309e98c2b8f79434db437068ec80f197f855ec7efc8b5f8eb45b3163cae"

H37A_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/backup/DriveAuthorization.kt|be34ca749b70e33eac826eb904a71679796e4c5d"
    "app/src/main/java/studio/guitarlab/app/backup/DriveV3Protocol.kt|0a8b5dc97f8c3760cb3956f22fd80a7b413fe5eb"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }

h37a_ready() {
    local entry relative expected
    for entry in "${H37A_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

decode_h37a_patch() {
    local output="$1" archive actual_archive
    [[ -f "$H37A_PART" ]] || { echo "Missing H37a source archive: $H37A_PART" >&2; return 1; }
    archive="$(mktemp)"
    trap 'rm -f "$archive"' RETURN
    base64 -d "$H37A_PART" > "$archive"
    gzip -t "$archive"
    actual_archive="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual_archive" == "$H37A_ARCHIVE_SHA256" ]] || {
        echo "H37a source archive SHA-256 mismatch: $actual_archive" >&2
        return 1
    }
    gzip -dc "$archive" > "$output"
    rm -f "$archive"
    trap - RETURN
}

verify_h37a_patch() {
    local decoded actual
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h37a_patch "$decoded"
    actual="$(sha256sum "$decoded" | awk '{print $1}')"
    [[ "$actual" == "$H37A_PATCH_SHA256" ]] || {
        echo "H37a source patch SHA-256 mismatch: $actual" >&2
        return 1
    }
    rm -f "$decoded"
    trap - RETURN
}

apply_h37a() {
    local decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h37a_patch "$decoded"
    [[ "$(sha256sum "$decoded" | awk '{print $1}')" == "$H37A_PATCH_SHA256" ]] || {
        echo "H37a source patch changed between verify and apply." >&2
        return 1
    }
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

verify_h37a_patch

if h37a_ready; then
    echo "Source patch chain already materialized through H37a"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing H37 materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
apply_h37a
h37a_ready || { echo "H37a applied but final H37a hashes do not match." >&2; exit 1; }
echo "Source patch chain materialized through H37a with verified final hashes"
