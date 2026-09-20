#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_h37a.sh"
H37B_PART="$ROOT/.source-parts/H37bDriveTokenCacheHardening.patch.gz.b64"

H37B_ARCHIVE_SHA256="e9e79f2c46e47f0e04ccf0f9aaead83908afa04cb3f99c81bbd3438d250cc824"
H37B_PATCH_SHA256="9ebfb1e69a6ad4e888e9782c07d54f9566630814ba40b8f6163e82727bc85c5d"

H37B_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/backup/DriveAuthorization.kt|4e727ae8d2a7e24a8e2c960380ade30ad1be1bf3"
    "app/src/main/java/studio/guitarlab/app/backup/DriveV3Protocol.kt|83a46bcd0d98007c376b3476e99a97d3d6a96a9b"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }

h37b_ready() {
    local entry relative expected
    for entry in "${H37B_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

decode_h37b_patch() {
    local output="$1" archive actual_archive
    [[ -f "$H37B_PART" ]] || { echo "Missing H37b source archive: $H37B_PART" >&2; return 1; }
    archive="$(mktemp)"
    trap 'rm -f "$archive"' RETURN
    base64 -d "$H37B_PART" > "$archive"
    gzip -t "$archive"
    actual_archive="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual_archive" == "$H37B_ARCHIVE_SHA256" ]] || {
        echo "H37b source archive SHA-256 mismatch: $actual_archive" >&2
        return 1
    }
    gzip -dc "$archive" > "$output"
    rm -f "$archive"
    trap - RETURN
}

verify_h37b_patch() {
    local decoded actual
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h37b_patch "$decoded"
    actual="$(sha256sum "$decoded" | awk '{print $1}')"
    [[ "$actual" == "$H37B_PATCH_SHA256" ]] || {
        echo "H37b source patch SHA-256 mismatch: $actual" >&2
        return 1
    }
    rm -f "$decoded"
    trap - RETURN
}

apply_h37b() {
    local decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h37b_patch "$decoded"
    [[ "$(sha256sum "$decoded" | awk '{print $1}')" == "$H37B_PATCH_SHA256" ]] || {
        echo "H37b source patch changed between verify and apply." >&2
        return 1
    }
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

verify_h37b_patch

if h37b_ready; then
    echo "Source patch chain already materialized through H37b"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing H37a materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
apply_h37b
h37b_ready || { echo "H37b applied but final H37b hashes do not match." >&2; exit 1; }
echo "Source patch chain materialized through H37b with verified final hashes"
