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
PLACEHOLDER_REVIEW_FINAL_BLOB="b8d17823cab11c22959948f6ffb041afba02b2d6"
PLACEHOLDER_ACTUAL_BLOB="$(git -C "$ROOT" hash-object "$PLACEHOLDER_TARGET")"
if [[ "$PLACEHOLDER_ACTUAL_BLOB" == "$PLACEHOLDER_FINAL_BLOB" || "$PLACEHOLDER_ACTUAL_BLOB" == "$PLACEHOLDER_REVIEW_FINAL_BLOB" ]]; then
    echo "Source patch chain already materialized: StudioPlaceholderScreen RC3"
else
    if [[ "$PLACEHOLDER_ACTUAL_BLOB" != "$PLACEHOLDER_GEOMETRY_BLOB" ]]; then
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

STUDIO_SHELL_TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt"
STUDIO_GUIDE_TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt"
STUDIO_SHELL_RC3_FINAL_BLOB="7fc93de8cf0dfce7d47cc97b306231d0466771db"
STUDIO_GUIDE_REVIEW_FINAL_BLOB="2118a200e84ba3797e113d162d76d8263d932d42"
if [[ "$(git -C "$ROOT" hash-object "$STUDIO_SHELL_TARGET")" == "$STUDIO_SHELL_RC3_FINAL_BLOB" && "$(git -C "$ROOT" hash-object "$STUDIO_GUIDE_TARGET")" == "$STUDIO_GUIDE_REVIEW_FINAL_BLOB" ]]; then
    echo "Source patch chain already materialized: Studio shell/guide physical-review state"
else
    apply_patch_once "$ROOT/.source-parts/UIShellGuideCopy.rc3.patch"
fi

apply_patch_once "$ROOT/.source-parts/UITestsCopy.rc3.patch"
apply_patch_once "$ROOT/.source-parts/UIProjectFactoryCopy.rc3.patch"

apply_encoded_gzip_patch_once() {
    local encoded_archive="$1"
    local tmp_archive
    local tmp_patch
    tmp_archive="$(mktemp)"
    tmp_patch="$(mktemp)"
    trap 'rm -f "$tmp_archive" "$tmp_patch"' RETURN
    base64 -d "$encoded_archive" > "$tmp_archive"
    gzip -dc "$tmp_archive" > "$tmp_patch"
    apply_patch_once "$tmp_patch"
    rm -f "$tmp_archive" "$tmp_patch"
    trap - RETURN
}

# Compact text-safe archive of the post-#613 physical-review source/test delta.
# The compatibility patch below replaces one Compose test assertion that is unavailable in the
# project's current UI-test API. Guard the pair as one materialization unit so a second invocation
# never tries to reverse-match the pre-compatibility snapshot.
AUTO_SECTIONS_TEST_TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/AutoSectionsSlotInstrumentedTest.kt"
HOME_REVIEW_TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
COUNTDOWN_REVIEW_TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/RecordingCountdownOverlayInstrumentedTest.kt"
AUTO_SECTIONS_TEST_COMPAT_BLOB="5fbca446823979b97e4b6010f3117601d76ab367"
HOME_REVIEW_FINAL_BLOB="fbe48b557e4c8f1d5cf6a46a11034ceeb0cfdbdc"
COUNTDOWN_REVIEW_FINAL_BLOB="91a75d0fe0504411c608a1bd7b0fc5fe27dffa82"
if [[ -f "$AUTO_SECTIONS_TEST_TARGET" && -f "$HOME_REVIEW_TARGET" && -f "$COUNTDOWN_REVIEW_TARGET" ]] \
    && [[ "$(git -C "$ROOT" hash-object "$AUTO_SECTIONS_TEST_TARGET")" == "$AUTO_SECTIONS_TEST_COMPAT_BLOB" ]] \
    && [[ "$(git -C "$ROOT" hash-object "$HOME_REVIEW_TARGET")" == "$HOME_REVIEW_FINAL_BLOB" ]] \
    && [[ "$(git -C "$ROOT" hash-object "$COUNTDOWN_REVIEW_TARGET")" == "$COUNTDOWN_REVIEW_FINAL_BLOB" ]]; then
    echo "Source patch chain already materialized: post-#613 physical-review + Compose test compatibility"
else
    apply_encoded_gzip_patch_once "$ROOT/.source-parts/RC3PhysicalReviewUx.patch.gz"
    apply_patch_once "$ROOT/.source-parts/RC3PhysicalReviewUxTestCompat.patch"
fi

# Physical editing/recording hardening. Keep the stages independent and ordered so regressions
# remain bisectable: interaction -> lineage -> drag transaction -> timing -> waveform -> integrated tests.
apply_patch_once "$ROOT/.source-parts/H1TrimHardening.patch"
apply_patch_once "$ROOT/.source-parts/H2ClipLifecycle.patch"
apply_patch_once "$ROOT/.source-parts/H3DragTransaction.patch"
apply_encoded_gzip_patch_once "$ROOT/.source-parts/H4RecordingSync.patch.gz"
apply_encoded_gzip_patch_once "$ROOT/.source-parts/H5LiveWaveform.patch.gz"
apply_encoded_gzip_patch_once "$ROOT/.source-parts/H6IntegratedRegression.patch.gz"
apply_encoded_gzip_patch_once "$ROOT/.source-parts/H6UserGuideSync.patch.gz"
