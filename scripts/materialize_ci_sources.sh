#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="$ROOT/scripts/materialize_ci_sources_base_h18a.sh"

hash_file() { git -C "$ROOT" hash-object "$1"; }

H20_POLICY="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutePolicy.kt"
H20_STORE="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt"
H21_THEME="$ROOT/app/src/main/java/studio/guitarlab/app/ui/theme/Theme.kt"
H21_STUDIO="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt"
H22_POLICY="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutePolicy.kt"
H22_STORE="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt"
H22_TEST="$ROOT/app/src/test/java/studio/guitarlab/app/ui/StudioAudioRoutePolicyTest.kt"

H23_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/AppTransientFeedbackPolicy.kt|3312f32ca862044e9151cfebaf931b407d0270f6"
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|39d1277f8356f54614b3ac80b2a4358e867df292"
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|f7a5c9a0e6cf3b44b1d5b206298be6dfa0ffaf74"
    "app/src/main/java/studio/guitarlab/app/ui/StudioLatencyCalibration.kt|28c459f5879065f1fb51166f17d1e12e4614899b"
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt|952a78cf08242e097e1fb31b94147cfe09c8faee"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|008e92474c6643fedcaad6fadbb0cf6c4cc43653"
    "app/src/test/java/studio/guitarlab/app/ui/AppTransientFeedbackPolicyTest.kt|5b7d0375814898978564dfc8353ed412bf7666ba"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/AudioClockAnchorPolicy.kt|d599d4a7919e11bd31c158a93150b6d4d075bb92"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicy.kt|a71337625718e4966279ff0b04e8b4772212a987"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/RecordingTimingCompensationPolicy.kt|2b4cca7e06c0e498457bc1be4e004b9a5b468401"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/AudioClockAnchorPolicyTest.kt|e127462c6d47603e15bf5e7bab157c9a4e29cd95"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicyTest.kt|317f8e1f6c8e1f0c5f6d55069b7e19c6a5bb1480"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/RecordingTimingCompensationPolicyTest.kt|606e0b139298f83b29dad75eeda2fd751bbfcaa9"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/PracticeWorkflowPolicy.kt|4bae5c496a3e7a61d76c398f1281847030cacc39"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/RecordingProjectPolicy.kt|113667a8fe5089b491edc1702bd514a9a16a3e42"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/PracticeWorkflowPolicyTest.kt|100f9e41853a4387c39452ee73ae06a2e7a58787"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/RecordingProjectPolicyTest.kt|213868f4542e6fd3ac4923106ba12521407ab25d"
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt|de99b9b518d53f18b1a41eb569156f33271995f8"
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioRecordingEngine.kt|32e4c32b7ece29754e07b5b0d00ceb0d972689e6"
)

h23_ready() {
    local entry relative expected
    for entry in "${H23_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

if h23_ready; then
    echo "Source patch chain already materialized through H23"
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

H22A_READY=false
if [[ -f "$H22_POLICY" && -f "$H22_STORE" && -f "$H21_STUDIO" && -f "$H22_TEST" ]] \
    && [[ "$(hash_file "$H22_POLICY")" == "fab05208fe0ffc04e63118827482ee721827ac57" ]] \
    && [[ "$(hash_file "$H22_STORE")" == "55926042c204fe30d6677118759447660a902ec0" ]] \
    && [[ "$(hash_file "$H21_STUDIO")" == "a5ba9851b42e894109b4c8b75968c3972206571c" ]] \
    && [[ "$(hash_file "$H22_TEST")" == "460e2d04263a9529afb3dd572d4f7237365c3b4c" ]]; then
    H22A_READY=true
fi

if [[ "$H22A_READY" != true ]]; then
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

    H22_ENCODED="$(mktemp)"
    trap 'rm -f "$H22_ENCODED"' EXIT
    cat \
        "$ROOT/.source-parts/H22AudioRouteSemanticUx.patch.gz.part00" \
        "$ROOT/.source-parts/H22AudioRouteSemanticUx.patch.gz.part01" \
        "$ROOT/.source-parts/H22AudioRouteSemanticUx.patch.gz.part02" \
        > "$H22_ENCODED"
    apply_encoded_gzip_patch_once "$H22_ENCODED"
    rm -f "$H22_ENCODED"
    trap - EXIT

    [[ "$(hash_file "$H22_POLICY")" == "fab05208fe0ffc04e63118827482ee721827ac57" ]]
    [[ "$(hash_file "$H22_STORE")" == "55926042c204fe30d6677118759447660a902ec0" ]]
    [[ "$(hash_file "$H21_STUDIO")" == "a5ba9851b42e894109b4c8b75968c3972206571c" ]]
    [[ "$(hash_file "$H22_TEST")" == "7aecb2736d5beed414e6be6944c040b3e2669e67" ]]
    apply_patch_once "$ROOT/.source-parts/H22aUnitTestMigrationAssertion.patch"
    [[ "$(hash_file "$H22_TEST")" == "460e2d04263a9529afb3dd572d4f7237365c3b4c" ]]
fi

H23_ENCODED="$(mktemp)"
trap 'rm -f "$H23_ENCODED"' EXIT
cat \
    "$ROOT/.source-parts/H23RecordingTimingFeedbackHardening.patch.gz.part00" \
    "$ROOT/.source-parts/H23RecordingTimingFeedbackHardening.patch.gz.part01" \
    "$ROOT/.source-parts/H23RecordingTimingFeedbackHardening.patch.gz.part02" \
    "$ROOT/.source-parts/H23RecordingTimingFeedbackHardening.patch.gz.part03" \
    > "$H23_ENCODED"
apply_encoded_gzip_patch_once "$H23_ENCODED"
rm -f "$H23_ENCODED"
trap - EXIT

if ! h23_ready; then
    echo "H23 materialization completed but final source hashes do not match the audited contract." >&2
    exit 1
fi
echo "Source patch chain materialized through H23 with verified final hashes"
