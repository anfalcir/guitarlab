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
apply_patch_once "$ROOT/.source-parts/H6WaveformTestFix.patch"
apply_encoded_gzip_patch_once "$ROOT/.source-parts/H6UserGuideSync.patch.gz"

# Physical Review II hardening. H7-H10 deliberately form one guarded unit because H10 refines
# StudioViewModel after H7; checking final target blobs keeps repeated materialization idempotent
# without reverse-applying an earlier patch through later edits.
PHYSICAL_REVIEW_II_VIEW_MODEL="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt"
PHYSICAL_REVIEW_II_PLACEHOLDER="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt"
PHYSICAL_REVIEW_II_WAVEFORM="$ROOT/core/project/src/main/kotlin/studio/guitarlab/core/project/LiveWaveformAccumulator.kt"
PHYSICAL_REVIEW_II_GUIDE="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt"
PHYSICAL_REVIE]}%%}QI9MA=IPôˆ‘I==P½…ÁÀ½ÍÉŒ½µ…¥¸½©…Ù„½ÍÑÕ‘¥¼½Õ¥Ñ…É±…ˆ½…ÁÀ½Õ¤½QÉ…¹ÍÁ½ÉÑ	…È¹­Ðˆ)A!eM%1}IY%]}%%}5%aHôˆ‘I==P½…ÁÀ½ÍÉŒ½µ…¥¸½©…Ù„½ÍÑÕ‘¥¼½Õ¥Ñ…É±…ˆ½…ÁÀ½Õ¤½5¥á•É½¬¹­Ðˆ)¥˜ml€µ˜€ˆ‘A!eM%1}IY%]}%%}Y%]}5=0ˆ€˜˜€µ˜€ˆ‘A!eM%1}IY%]}%%}A1!=1Hˆ€˜˜€µ˜€ˆ‘A!uM%1}IY%]}%%}]Y=I4ˆp(€€€€€€˜˜€µ˜€ˆ‘A!uM%1}IY%]}%%}U%ˆ€˜˜€µ˜€ˆ‘A!eM%1}IY%]}%%}QI9MA=IPˆ€˜˜€µ˜€ˆ‘A!uM%1}IY%]}%%}5%aHˆutp(€€€€˜˜ml€ˆ¡¥Ð€µ€ˆ‘I==Pˆ¡…Í µ½‰©•Ð€ˆ‘A!eM%1}IY%]}%%}Y%]}5=0ˆ¤ˆ€ôô€ˆÝŒÔÙÁˆÉŒÜá˜ÁˆÐÀá‘˜ÌÐÅá„ÌÜÐÑ••”ÕŒØå‰äÌˆutp(€€€€˜˜ml€ˆ¡¥Ð€µ€ˆ‘I==Pˆ¡…Í µ½‰©•Ð€ˆ‘A!eM%1}IY%]}%%}A1!=1H„¤ˆ€ôô€ˆÌÔá‘ÜÕÐÕ˜ÍŒÈÔØÄÙ•ˆÜÌÝ”äå‘‘‰„ÐÉ™”ÀÉ„ÀÔÌˆutp(€€€€˜˜ml€ˆ¡¥Ð€µ€ˆ‘I==Pˆ¡…Í µ½‰©•Ð€ˆ‘A!eM%1}IY%]}%%}]Y=I4ˆ¤ˆ€ôô€ˆÀÝ…ˆå„ÜÌäÀÈÁ”áÀÙˆØàÄÝˆÄÄààá˜äåå‘åˆÅˆˆutp(€€€€˜˜ml€ˆ¡¥Ð€µ€ˆ‘I==Pˆ¡…Í µ½‰©•Ð€ˆ‘A!uM%1}IY%]}%%}U%ˆ¤ˆ€ôô€ˆÔåÑ”Õ”ÈÍ„ÉÔååˆÈàÔÝ”ÔÌÈÍ˜Å‰…•”ÁˆÀÁ„Éˆˆutp(€€€€˜˜ml€ˆ¡¥Ð€µ€ˆ‘I==Pˆ¡…Í µ½‰©•Ð€ˆ‘A!eM%1}IY%]}%%}QI9MA=IPˆ¤ˆ€ôô€‰˜àÉˆÜÔÄÀÉ…˜àÌÁ”àÄàáŒØÈÅˆÐÜÐÁ•ÔÌØÌÁ„ÁŒäˆutp(€€€€˜˜ml€ˆ¡¥Ð€µ€ˆ‘I==Pˆ¡…Í µ½‰©•Ð€ˆ‘A!uM%1}IY%]}%%}5%aHˆ¤ˆ€ôô€ˆÜÔÕ™ÄÔÝ˜àÙ„Ý™•„á„ÀÍ”ÉÉŒáˆÜäåÑ•˜Á‘™ŒÐÄˆutìÑ¡•¸(€€€•¡¼€‰M½ÕÉ”Á…Ñ ¡…¥¸…±É•…‘äµ…Ñ•É¥…±¥é•èA¡åÍ¥…°I•Ù¥•Ü%$ Üµ ÄÀˆ)•±Í”(€€€…ÁÁ±å}•¹½‘•‘}é¥Á}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÝMÑ…Ñ•1•Ù•±QÉ…¹ÍÁ½ÉÐ¹Á…Ñ ¹èˆ(€€€…ÁÁ±å}•¹½‘•‘}é¥Á}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ á]½É­ÍÁ…•±½Ü¹Á…Ñ ¹èˆ(€€€…ÁÁ±å}•¹½‘•‘}é¥Á}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ å1¥Ù•]…Ù•™½ÉµMÑ…‰¥±¥Ñä¹Á…Ñ ¹èˆ(€€€…ÁÁ±å}•¹½‘•‘}é¥Á}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÁMÑ…Ñ•I…•Õ¥‘”¹Á…Ñ ¹èˆ)™¤((ŒA¡åÍ¥…°I•Ù¥•Ü%%$€¼ ÄÄè5¥á•ÈÍ•µ•¹Ñ•µ‰…ÈÍ¥µÁ±¥™¥…Ñ¥½¸°Ý…Ù•™½É´ÑÉ…¬Í•±•Ñ¥½¸…¹(Œ±¥Ù”É•½É‘¥¹œA•…¬½I5LÁÉ½©•Ñ¥½¸¸Q¡¥Ì¥Ì½¹”™¥¹…°Á…Ñ …™Ñ•È Üµ ÄÀ°Í¼…ÁÁ±å}Á…Ñ¡}½¹”(ŒÁÉ½Ù¥‘•Ì‰½Ñ ™½ÉÝ…É…ÁÁ±¥…Ñ¥½¸…¹É•Ù•ÉÍ”µµ…Ñ ¥‘•µÁ½Ñ•¹äÝ¡¥±”™…¥±¥¹œ±½Í•½¸‘É¥™Ð¸)…ÁÁ±å}•¹½‘•‘}é¥Á}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÅ5¥á•É]…Ù•™½Éµ5•Ñ•É¥¹œ¹Á…Ñ ¹èˆ((Œ$€ŒØÄà•áÁ½Í•„‘•Ñ•Éµ¥¹¥ÍÑ¥ŒÉ…”¥¸Ñ¡” à™¥ÉÍÐµÑ…ÀQÉ¥´•¹ÑÉäèÑ¡”µ•¹Ô±¥¬‘•™•ÉÉ•(Œ‰•¥¹QÉ¥´Ñ¡É½Õ Á•¹‘¥¹œ±½…°½µÁ½Í”ÍÑ…Ñ”€¬1…Õ¹¡•‘™™•Ð¸ ÄÅ„É•µ½Ù•ÌÑ¡…Ð…Íå¹¡É½¹½ÕÌ(Œ¡…¹‘½™˜…¹‘¥ÍÁ…Ñ¡•Ì‰•¥¹QÉ¥´Íå¹¡É½¹½ÕÍ±ä…™Ñ•È±½Í¥¹œÑ¡”µ•¹Ô¸)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÅQÉ¥µ¹ÑÉåI…•¥à¹Á…Ñ ˆ((Œ$€ŒØÄä•áÁ½Í•„Í•µ…¹Ñ¥ÌÉ•É•ÍÍ¥½¸¥¹ÑÉ½‘Õ•‰ä ÄÄÝ…Ù•™½É´Í•±•Ñ¥½¸è…¹•ÍÑ½È½‘¥Í…‰±•(Œ±¥­…‰±•Ìµ•É•QÉ¥µ!…¹‘±”‘•Í•¹‘…¹ÑÌ½ÕÐ½˜Ñ¡”µ•É•…•ÍÍ¥‰¥±¥ÑäÑÉ•”¸ ÄÅˆµ½Ù•Ì±…¹”(ŒÍ•±•Ñ¥½¸Ñ¼„Í¥‰±¥¹œ‰…­É½Õ¹Ñ…É•Ð…¹É•µ½Ù•Ì±¥À±¥­…‰±”Í•µ…¹Ñ¥ÌÝ¡¥±”ÑÉ¥µµ¥¹œ¸)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÅ‰]…Ù•™½ÉµM•±•Ñ¥½¹M•µ…¹Ñ¥Ì¹Á…Ñ ˆ((ŒA¡åÍ¥…°I•Ù¥•Ü%P€¼ ÄÈµ ÄÔè±½‰…°±•Ù•°Ý½É­™±½Ü°QÉ¥´ÉÕ±•È±…É¥Ñä°5¥á•È½Ù•É™±½Ü…¹(ŒÉ•Í¥‘•¹ÐMÑÕ‘¥¼¹…Ù¥…Ñ¥½¸É•ÑÕÉ¸¸-••À½É‘•É•½‰¥Í•Ñ…‰±”…¹™…¥°±½Í•½¸Í½ÕÉ”‘É¥™Ð¸)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÉ1•Ù•±¹¥¹”¹Á…Ñ ˆ)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÉ1•Ù•±U¤¹Á…Ñ ˆ)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÍQÉ¥µIÕ±•È¹Á…Ñ ˆ)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÑ5¥á•É!½É¥é½¹Ñ…±MÉ½±°¹Á…Ñ ˆ)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÑ…5¥á•ÉMÉ½±±Y¥•ÝÁ½ÉÑI•É•ÍÍ¥½¸¹Á…Ñ ˆ)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÕI•Í¥‘•¹ÑMÑÕ‘¥½I•ÑÕÉ¸¹Á…Ñ ˆ((Œ¥¹…°‰…Í•±¥¹”Á½±¥Í €¼ ÄØè±…É¥™äÁÉ…Ñ¥”µ‰…ÈÍ•µ…¹Ñ¥Ì…¹½µÁ…ÐQÉ¥´ÁÉ½©•Ñ¥½¸¸(ŒÁÁ±¥•ÍÑÉ¥Ñ±ä…™Ñ•È ÄÔÍ¼Ñ¡”€ŒØÈÐ‘¥¥Ñ…±±ä¡½µ½±½…Ñ• ÄÈµ ÄÔ¡…¥¸É•µ…¥¹ÌÑÉ…•…‰±”¸)…ÁÁ±å}•¹½‘•‘}é¥Á}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÙ¥¹…±U¥QÉ¥µ=Ù•É±…ä¹Á…Ñ ¹èˆ((Œ¥¹…°Á¡åÍ¥…°µÁ½±¥Í ½ÉÉ•Ñ¥½¸€¼ ÄÜè­••ÀUPÑ¥­Ì½¹™¥¹•Ñ¼Ñ¡”Ñ¥µ”ÉÕ±•È…¹(Œ•¹Ñ•È©ÕÍÑ•Ì½;µÙ•¥ÌÝ¥Ñ¡¥¸¥ÑÌ‘•‘¥…Ñ•ÁÉ…Ñ¥”µ‰…ÈÍ•µ•¹Ð¸)…ÁÁ±å}Á…Ñ¡}½¹”€ˆ‘I==P¼¹Í½ÕÉ”µÁ…ÉÑÌ½ ÄÝÕÑIÕ±•ÉAÉ…Ñ¥•MÁ…¥¹œ¹Á…Ñ ˆ(