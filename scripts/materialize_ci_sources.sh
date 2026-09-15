#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="$ROOT/scripts/materialize_ci_sources_base_h18a.sh"

hash_file() { git -C "$ROOT" hash-object "$1"; }

H20_POLICY="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutePolicy.kt"
H20_STORE="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt"
H21_THEME="$ROOT/app/src/main/java/studio/guitarlab/app/ui/theme/Theme.kt"
H21_STUDIO="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt"

if [[ -f "$H21_THEME" && -f "$H21_STUDIO" ]] \
    && [[ "$(hash_file "$H21_THEME")" == "5b09422ebd9f507cbb9ae603baa3e2d0a921c776" ]] \
    && [[ "$(hash_file "$H21_STUDIO")" == "4868b26b4bbdd256450a093494c3bb73743a5892" ]]; then
    echo "Source patch chain already materialized through H21"
    exit 0
fi

apply_patch_once() {
    local patch_file="$1"
    if patch --dry-run -p1 -d "$ROOT" < "$patch_file" >/dev/null; then
        patch --batch --forward -p1 -d "$ROOT" < "$patch_file"
    elif patch --dry-run -R -p1 -d "$ROOT" < "$patch_file" >/dev/null; then
        echo "Source patch already materialized: $(basename "$patch_file")"
    else
        echo "Source patch no longer applies cleanly: $patch_file" >&2
        exit 1
    fi
}

apply_encoded_gzip_patch_once() {
    local encoded_archive="$1"
    local tmp_archive tmp_patch
    tmp_archive="$(mktemp)"
    tmp_patch="$(mktemp)"
    trap 'rm -f "$tmp_archive" "$tmp_patch"' RETURN
    base64 -d "$encoded_archive" > "$tmp_archive"
    gzip -t "$tmp_archive"
    gzip -dc "$tmp_archive" > "$tmp_patch"
    apply_patch_once "$tmp_patch"
    rm -f "$tmp_archive" "$tmp_patch"
    trap - RETURN
}

H20_READY=false
if [[ -f "$H20_POLICY" && -f "$H20_STORE" ]] \
    && [[ "$(hash_file "$H20_POLICY")" == "4dbc797e1ce141de6c684d2a93045b0c1a9c3c22" ]] \
    && [[ "$(hash_file "$H20_STORE")" == "4ccd07aa09df82c54ce31c00ae8701e82f07e9f7" ]]; then
    H20_READY=true
fi

if [[ "$H20_READY" != true ]]; then
    bash "$BASE"
    apply_encoded_gzip_patch_once "$ROOT/.source-parts/H20PhysicalOutputCanonicalization.patch.gz"
fi

for part in \
    H21Foundation.patch.gz \
    H21ScreensA.patch.gz \
    H21Settings.patch.gz \
    H21ShellGuide.patch.gz \
    H21Workspace0.patch.gz \
    H21Workspace1.patch.gz \
    H21Workspace2.patch.gz \
    H21Workspace3.patch.gz; do
    apply_encoded_gzip_patch_once "$ROOT/.source-parts/$part"
done

[[ "$(hash_file "$H21_THEME")" == "5b09422ebd9f507cbb9ae603baa3e2d0a921c776" ]]
[[ "$(hash_file "$H21_STUDIO")" == "4868b26b4bbdd256450a093494c3bb73743a5892" ]]
echo "Source patch chain materialized through H21 with verified final hashes"
