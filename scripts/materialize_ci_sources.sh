#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidAudioProbeEngine.kt"
mkdir -p "$(dirname "$TARGET")"
cat "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part00 \
    "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part01 \
    "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part02 \
    "$ROOT"/.source-parts/AndroidAudioProbeEngine.kt.part03 > "$TARGET"

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

apply_guarded_patch() {
    local patch_file="$1"
    local target_file="$2"
    local expected_base_blob="$3"
    local expected_final_blob="$4"
    local actual_blob
    actual_blob="$(git -C "$ROOT" hash-object "$target_file")"

    if [[ "$actual_blob" == "$expected_final_blob" ]]; then
        echo "Source patch already materialized: $(basename "$patch_file")"
        return
    fi
    if [[ "$actual_blob" != "$expected_base_blob" ]]; then
        echo "Unexpected source base for $(basename "$target_file"): $actual_blob" >&2
        echo "Expected base $expected_base_blob or materialized $expected_final_blob" >&2
        exit 1
    fi

    patch --batch --forward -p1 -d "$ROOT" < "$patch_file"
    actual_blob="$(git -C "$ROOT" hash-object "$target_file")"
    if [[ "$actual_blob" != "$expected_final_blob" ]]; then
        echo "Materialized source hash mismatch for $(basename "$target_file"): $actual_blob" >&2
        exit 1
    fi
}

apply_patch_once "$ROOT/.source-parts/TimelineMarkerRail.rc3.patch"
STUDIO_VIEW_MODEL_TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt"
STUDIO_VIEW_MODEL_RC3_BLOB="64db74cfa94ca0484f3bfd585b0f40f2c0faea09"
STUDIO_VIEW_MODEL_FINAL_BLOB="3fb7186a56b17c095545caa63fd9317003ce79a2"
if [[ "$(git -C "$ROOT" hash-object "$STUDIO_VIEW_MODEL_TARGET")" == "$STUDIO_VIEW_MODEL_FINAL_BLOB" ]]; then
    echo "Source patch chain already materialized: StudioViewModel RC3"
else
    if [[ "$(git -C "$ROOT" hash-object "$STUDIO_VIEW_MODEL_TARGET")" != "$STUDIO_VIEW_MODEL_RC3_BLOB" ]]; then
        apply_guarded_patch \
            "$ROOT/.source-parts/StudioViewModel.rc3.patch" \
            "$STUDIO_VIEW_MODEL_TARGET" \
            "6b55be5224638ca83c30bc52a2cd2cee1db920a9" \
            "$STUDIO_VIEW_MODEL_RC3_BLOB"
    fi
    apply_guarded_patch \
        "$ROOT/.source-parts/StudioViewModelCopyConsistency.rc3.patch" \
        "$STUDIO_VIEW_MODEL_TARGET" \
        "$STUDIO_VIEW_MODEL_RC3_BLOB" \
        "$STUDIO_VIEW_MODEL_FINAL_BLOB"
fi
PLACEHOLDER_TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt"
PLACEHOLDER_GEOMETRY_BLOB="9f9822545e29c8564824a8fdf74dd9fcbc302d60"
PLACEHOLDER_FINAL_BLOB="d1b8640d500d39a36136934ebf20d10cad77bf28"
if [[ "$(git -C "$ROOT" hash-object "$PLACEHOLDER_TARGET")" == "$PLACEHOLDER_FINAL_BLOB" ]]; then
    echo "Source patch chain already materialized: StudioPlaceholderScreen RC3"
else
    if [[ "$(git -C "$ROOT" hash-object "$PLACEHOLDER_TARGET")" != "$PLACEHOLDER_GEOMETRY_BLOB" ]]; then
        apply_guarded_patch \
            "$ROOT/.source-parts/StudioPlaceholderScreen.rc3.patch" \
            "$PLACEHOLDER_TARGET" \
            "f94875be1d33177a996c212f7c7f3568bb683bf3" \
            "5fee1b1c89795aa6f5c11b094a5d9177b7ff3be5"
        apply_guarded_patch \
            "$ROOT/.source-parts/StudioPlaceholderGeometry.rc3.patch" \
            "$PLACEHOLDER_TARGET" \
            "5fee1b1c89795aa6f5c11b094a5d9177b7ff3be5" \
            "$PLACEHOLDER_GEOMETRY_BLOB"
    fi
    apply_guarded_patch \
        "$ROOT/.source-parts/StudioCopyConsistency.rc3.patch" \
        "$PLACEHOLDER_TARGET" \
        "$PLACEHOLDER_GEOMETRY_BLOB" \
        "$PLACEHOLDER_FINAL_BLOB"
fi

apply_patch_once "$ROOT/.source-parts/UIAudioMixerCopy.rc3.patch"
apply_patch_once "$ROOT/.source-parts/UISettingsCopy.rc3.patch"
apply_patch_once "$ROOT/.source-parts/UIShellGuideCopy.rc3.patch"
apply_patch_once "$ROOT/.source-parts/UITestsCopy.rc3.patch"
apply_patch_once "$ROOT/.source-parts/UIProjectFactoryCopy.rc3.patch"
