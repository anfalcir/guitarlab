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
    "app/src/test/java/studio/guitarlab/app/ui/AppTransientFeedbackPolicyTest.kt|14c19afe655c68f6692dbada79d46c81e285d440"
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

H23_FEEDBACK_TEST="app/src/test/java/studio/guitarlab/app/ui/AppTransientFeedbackPolicyTest.kt"
H23_FEEDBACK_TEST_BASE_HASH="5b7d0375814898978564dfc8353ed412bf7666ba"

H23B_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/AppTransientFeedbackPolicy.kt|ee71a316861ade7bef2751a954c31d2f6fb1ff78"
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|39d1277f8356f54614b3ac80b2a4358e867df292"
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|af644197a0e806881ed7e33c1393f020a370de43"
    "app/src/main/java/studio/guitarlab/app/ui/StudioLatencyCalibration.kt|70eb60585f49cf33dce08915c78b16e35261ceb1"
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt|641f05e93b874584bfab2065fea0361d0122be2d"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|008e92474c6643fedcaad6fadbb0cf6c4cc43653"
    "app/src/test/java/studio/guitarlab/app/ui/AppTransientFeedbackPolicyTest.kt|71af0e139e6250de09bcebce83475559ebb3ac51"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/AudioClockAnchorPolicy.kt|89146dee6677c7cbcf4255fccf515714983bc315"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicy.kt|7f43b016fdafd489fe8f8d0776c76aa90d776e02"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/RecordingTimingCompensationPolicy.kt|242d744fe551bdf98889a5bd90db18a7678008fa"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/AudioClockAnchorPolicyTest.kt|78c93c23ef2bfca724d39cd5f163b05219ca5072"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicyTest.kt|e2341c908ee7b65d71579edf8230c55f98c8482c"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/RecordingTimingCompensationPolicyTest.kt|a82daa7f864c40f16e12893a599307e24bd933c0"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/PracticeWorkflowPolicy.kt|4bae5c496a3e7a61d76c398f1281847030cacc39"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/RecordingProjectPolicy.kt|113667a8fe5089b491edc1702bd514a9a16a3e42"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/PracticeWorkflowPolicyTest.kt|100f9e41853a4387c39452ee73ae06a2e7a58787"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/RecordingProjectPolicyTest.kt|b39db48a9b92b8a1c0cbf7bac7821014beee9715"
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt|de99b9b518d53f18b1a41eb569156f33271995f8"
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioRecordingEngine.kt|32e4c32b7ece29754e07b5b0d00ceb0d972689e6"
)

H24_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|643673855dd5683eccf04df614e634827cdd2a2d"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|45d6ae5a51eb5aec60721426ea0a4f02c4cbacc0"
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|d8deca759a39638f507d2c23e883ec1413ac55cd"
    "app/src/androidTest/java/studio/guitarlab/app/HomeProjectLibraryInstrumentedTest.kt|1bafaa45a6372b5728d4a0eb3ba3a90d0eeb7c28"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicy.kt|96a51d1309b2c888770d49d32c58c1a732913462"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicyTest.kt|9b09a64a8d7a173cffc3436f1b2c5e4f041a8f3a"
)

H24A_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|643673855dd5683eccf04df614e634827cdd2a2d"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|45d6ae5a51eb5aec60721426ea0a4f02c4cbacc0"
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|d8deca759a39638f507d2c23e883ec1413ac55cd"
    "app/src/androidTest/java/studio/guitarlab/app/HomeProjectLibraryInstrumentedTest.kt|9ed877ddd44c5d271b69f4519e9cf4db68562493"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicy.kt|96a51d1309b2c888770d49d32c58c1a732913462"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicyTest.kt|9b09a64a8d7a173cffc3436f1b2c5e4f041a8f3a"
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

h23_base_ready() {
    local entry relative expected
    for entry in "${H23_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        if [[ "$relative" == "$H23_FEEDBACK_TEST" ]]; then
            expected="$H23_FEEDBACK_TEST_BASE_HASH"
        fi
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

h23b_ready() {
    local entry relative expected
    for entry in "${H23B_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

h24_ready() {
    local entry relative expected
    for entry in "${H24_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

h24a_ready() {
    local entry relative expected
    for entry in "${H24A_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

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

apply_h23b() {
    local encoded
    encoded="$(mktemp)"
    trap 'rm -f "$encoded"' RETURN
    cat \
        "$ROOT/.source-parts/H23bRecordingTimingFeedbackCorrective.patch.gz.part00" \
        "$ROOT/.source-parts/H23bRecordingTimingFeedbackCorrective.patch.gz.part01" \
        > "$encoded"
    apply_encoded_gzip_patch_once "$encoded"
    rm -f "$encoded"
    trap - RETURN
}

apply_h24() {
    apply_encoded_gzip_patch_once "$ROOT/.source-parts/H24HomeProjectLibrary.patch.gz.part00"
}

apply_h24a() {
    apply_encoded_gzip_patch_once "$ROOT/.source-parts/H24aAndroidTestCompileFix.patch.gz.part00"
}

if h24a_ready; then
    echo "Source patch chain already materialized through H24a"
    exit 0
fi

if h24_ready; then
    apply_h24a
    h24a_ready || { echo "H24a applied but final H24a hashes do not match." >&2; exit 1; }
    echo "Source patch chain materialized through H24a with verified final hashes"
    exit 0
fi

if h23b_ready; then
    apply_h24
    h24_ready || { echo "H24 applied but final H24 hashes do not match." >&2; exit 1; }
    apply_h24a
    h24a_ready || { echo "H24a applied after H24 but final H24a hashes do not match." >&2; exit 1; }
    echo "Source patch chain materialized through H24a with verified final hashes"
    exit 0
fi

if h23_ready; then
    apply_h23b
    h23b_ready || { echo "H23b applied but final H23b hashes do not match." >&2; exit 1; }
    apply_h24
    h24_ready || { echo "H24 applied after H23b but final H24 hashes do not match." >&2; exit 1; }
    apply_h24a
    h24a_ready || { echo "H24a applied after H24 but final H24a hashes do not match." >&2; exit 1; }
    echo "Source patch chain materialized through H24a with verified final hashes"
    exit 0
fi

if h23_base_ready; then
    apply_patch_once "$ROOT/.source-parts/H23aJUnitAnnotation.patch"
    h23_ready || { echo "H23a applied but final H23 hashes do not match." >&2; exit 1; }
    apply_h23b
    h23b_ready || { echo "H23b applied after H23a but final H23b hashes do not match." >&2; exit 1; }
    apply_h24
    h24_ready || { echo "H24 applied after H23b but final H24 hashes do not match." >&2; exit 1; }
    apply_h24a
    h24a_ready || { echo "H24a applied after H24 but final H24a hashes do not match." >&2; exit 1; }
    echo "Source patch chain materialized through H24a with verified final hashes"
    exit 0
fi

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

apply_patch_once "$ROOT/.source-parts/H23aJUnitAnnotation.patch"

if ! h23_ready; then
    echo "H23/H23a materialization completed but final source hashes do not match the audited contract." >&2
    exit 1
fi

apply_h23b
if ! h23b_ready; then
    echo "H23b materialization completed but final source hashes do not match the audited contract." >&2
    exit 1
fi
apply_h24
if ! h24_ready; then
    echo "H24 materialization completed but final source hashes do not match the audited contract." >&2
    exit 1
fi
apply_h24a
if ! h24a_ready; then
    echo "H24a materialization completed but final source hashes do not match the audited contract." >&2
    exit 1
fi
echo "Source patch chain materialized through H24a with verified final hashes"
