#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
hash_file() { git -C "$ROOT" hash-object "$1"; }
H29_PATCH_PART="$ROOT/.source-parts/H29RecordingSessionHealth.patch.gz.b64"
H29_ARCHIVE_SHA256="2f5dff49aeeb271aadf4992f85f0dbf451a75f20fc4c72a750f09f993d8cc4ab"
H29_PATCH_SHA256="229998a0fb2198d8ecf82cba2f48eb7d66929ddf7e8b9398d6b017d8c04d0640"
H30_PATCH_PART="$ROOT/.source-parts/H30RecoveryUsbResilience.patch.gz.b64"
H30_ARCHIVE_SHA256="4b72aead443f21b2f61055a73e22d5c1e524afb368d74773d61c1b7dfd2463b1"
H30_PATCH_SHA256="26d4408812d7d8382b3e52d6542dce369071a8eb278ab8c240745e09c802b2bd"
H31_PATCH_PART="$ROOT/.source-parts/H31TakesDiagnostics.patch.gz.b64"
H31_ARCHIVE_SHA256="a90c1577bbb17c943742b5ca6b654f0de845f9bdd654d4a604b5d60241807630"
H31_PATCH_SHA256="0031367ba9b61057524056540a9b57a29aecb65cd4eb2d25854057ab170eda1d"
H32_PATCH_PART="$ROOT/.source-parts/H32TenMinuteQualityGate.patch.gz.b64"
H32_ARCHIVE_SHA256="41c5b3aba13e70b688b630bdbc5035ae92905257bd01db61b2cb6f69ba2e6435"
H32_PATCH_SHA256="63d62e3734ec0081b309bceffef05bd18cbc7c6d77ad4fc695a5534c8c3e5fb4"
H33_PATCH_PART="$ROOT/.source-parts/H33ExternalControl.patch.gz.b64"
H33_ARCHIVE_SHA256="d88a320ba2323ebac7a9db24d1f9314af270cc595d209d1bbfdfefc81be0f6a9"
H33_PATCH_SHA256="46880223ffd10711bbf660feedb705ea9c3fa6cd6a111017bd3b6ad32e7172dc"
H33A_PATCH_PART="$ROOT/.source-parts/H33aExternalControlCiCorrective.patch.gz.b64"
H33A_ARCHIVE_SHA256="63d2abf7b51e2e1a448c2743d8e119ac3da4d25a3f6212f6e789229ac2ae55f2"
H33A_PATCH_SHA256="4c25592453728272888f26abbd4e58c58d273e38709da1909892bf8389b120f1"
H33B_PATCH_PART="$ROOT/.source-parts/H33bExternalControlScrollTestCorrective.patch.gz.b64"
H33B_ARCHIVE_SHA256="838bd9927ee5fefb23c4038da5c1e8cd68ad8d354a5409eab6d572e0e6c6d02d"
H33B_PATCH_SHA256="c2bf706a0f2cb10d9c9f106732b317f48f1e239f923973e67056f4a0db54bca7"
H34_PATCH_PART="$ROOT/.source-parts/H34FineLatencyRange.patch.gz.b64"
H34_ARCHIVE_SHA256="d7ea906f7300d7b00c8a384ac945e140f386e7faf0023dad1f0672b8cda17bad"
H34_PATCH_SHA256="4b47bdc21b58c68e5ca9616314dca495ca5c5f5b72a1be76d25c1dc9116565c0"
H35_PATCH_PART="$ROOT/.source-parts/H35TakeSyncQuietCalibration.patch.gz.b64"
H35_ARCHIVE_SHA256="38cc7f6ab3098ad253a3584d6e96a1780d7b316f8d8506fab981597a6d050a90"
H35_PATCH_SHA256="3a255b8bad4ce79463293e290ad36d66d0031983d30b443cf07441353d286043"
H35A_PATCH_PART="$ROOT/.source-parts/H35aLintPermissionCorrective.patch.gz.b64"
H35A_ARCHIVE_SHA256="eb766693ffdf31c611ea038ba70ce46378b81d067a27f092cc40e960c2de8bb7"
H35A_PATCH_SHA256="8fb5be6b03b6f8897ddb78dc38ff1c1ad565767160d11e3cba5007f4320b9179"
H36_PATCH_PART="$ROOT/.source-parts/H36SettingsUxPolish.patch.gz.b64"
H36_ARCHIVE_SHA256="22895aa6e1d39a3c6988f503467e763893b0448ca1b087ba23614a1a2b162eaf"
H36_PATCH_SHA256="9489fe121cbbc6c43bf9675701cd74c2556c7d8c1461c48030745711c1b54852"

H29_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/AudioProbeScreen.kt|4dcaee3e2691206601c53ccafacba8575f5eda9e"
    "app/src/main/java/studio/guitarlab/app/ui/AudioProbeViewModel.kt|1facaa7e4ec97d573b166fd13ebde0a9d66fd891"
    "app/src/main/java/studio/guitarlab/app/ui/RecordingSessionHealthStore.kt|941194a80f85eaf6f1eba04f3481a57ba5ef562c"
    "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt|931af1e66fee42e6ed7377c73fa8ab87fa641f44"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|452117f0429bfab0f1da79317fccb6ed573fec0c"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/RecordingSessionHealth.kt|b773df1c45aa8fea2df0b323146f79c7b0729525"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/RecordingTimingCompensationPolicy.kt|26dfa9b8d21cc1ec8258ab07f68ad0154eb47997"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/RecordingSessionHealthTest.kt|eb5162cb6583ec57cd3ac7e24b266fd848c38f4a"
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt|9366d7fa6a5dd2c39be11b1ce122840f42826a3a"
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioRecordingEngine.kt|50dd319428389cc5128b72263c56e5c0f60a3897"
)
H30_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt|7c25005677539e79552e8a4bcdd584d935e3b874"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|c568a43b31acea379c3131b7d0ec4bf4f5e4c2b8"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/AudioRouteSessionPolicy.kt|8ecd99f72b26af2e92bf8a0a9148409cbf1f1ed1"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/AudioRouteSessionPolicyTest.kt|a440d2f99a6771eaa8a1ce50ad51057dc259d9e6"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectRecordingMediaStore.kt|7ba4b17b0273fd0d6aa87cfde64ba10188a84467"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/RecordingRecoveryPolicy.kt|8ad11f6877d44556206108c2a1394067b74d24d3"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/RecordingRecoveryPolicyTest.kt|12a2790b3c51d2c59b8815abfa3213f53e9f7b2c"
)
H31_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/AudioProbeViewModel.kt|2d4fba816d70ee023b513d46a517c248c2a6e540"
    "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt|c22242d284fe0891ccba526b30d04ee93a31692a"
    "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt|d557fdaea3830c127b983e38570c782b89c2b635"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|d139d741c0dfb756c04f8fb6b7f3f31f1f94322b"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/AudioProbeReportFormatter.kt|5b8deb85b63382f409d671a5acac56be8ae9876f"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/AudioProbeReportFormatterTest.kt|077083adca217f75be56f58a67757c672a96900e"
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt|5404854b71532288441d890f67db95ed6dc3f588"
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectValidator.kt|2f4263a7e475e4872586bdc500849e9dd6d6c981"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectCodec.kt|9e17f765781c996d924991843bfe23516f7c7d35"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/TakeManagementPolicy.kt|e67db39f38a3112075532c5c4b35d44c4f9832e8"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/TakeManagementPolicyTest.kt|86ad2c9462cd25d187e8749cebfba4f4859fb3ca"
)
H32_CHECKS=(
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/RecordingDurationTimingStressTest.kt|437575324ea46ceb5afbc8f8530176ecaaf94710"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/SongSessionQualityGateTest.kt|ec3b8d4548e6cc79afd0c55af05c507a484aa3c6"
)
H33_CHECKS=(
    "app/src/androidTest/java/studio/guitarlab/app/ExternalControlSettingsInstrumentedTest.kt|1cd028f119dde80228a365f924119f1108535207"
    "app/src/main/java/studio/guitarlab/app/MainActivity.kt|10b17c05d01edc8a3e1964764d60c19418db398e"
    "app/src/main/java/studio/guitarlab/app/ui/AndroidExternalMidiRuntime.kt|198b98a3ec4e6cc23502f8626e5a3551912ddb25"
    "app/src/main/java/studio/guitarlab/app/ui/ExternalControlHub.kt|63222de54d31ead4d3a52abf17f0efda2b52ab07"
    "app/src/main/java/studio/guitarlab/app/ui/ExternalControlStore.kt|5d8698e43bcd81b60ae9f159c429d97dbf983107"
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|957b1fb8144faf283bf4e56f83d642a612225a6c"
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt|abe6b0d16f352abb904051dee999c1af0a5e714a"
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|f8db8ffaa7173d90b25083542b7163923133ff62"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ExternalControlPolicy.kt|92bbf7bbba82729a4d3ea41e2d0190d283799d11"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ExternalControlPolicyTest.kt|09824c95c4cc30d16b6c5b78c6732fed5205f4c8"
)
H33A_CHECKS=(
    "app/src/androidTest/java/studio/guitarlab/app/ExternalControlSettingsInstrumentedTest.kt|6277df8aac413cb9f45d03293923be71ba1fab2c"
    "app/src/main/java/studio/guitarlab/app/MainActivity.kt|70b22de5011ab2233b7633326436ce689ee52045"
)
H33B_CHECKS=(
    "app/src/androidTest/java/studio/guitarlab/app/ExternalControlSettingsInstrumentedTest.kt|dac03e9f668e44f37744ce4bf9d7a958b2c15cef"
    "app/src/main/java/studio/guitarlab/app/MainActivity.kt|70b22de5011ab2233b7633326436ce689ee52045"
)
H34_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|9b47f0d91a7aab651a81ff9e0753fa0b45785b7f"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicy.kt|cb40c760708adb7c649775d35feda8b9066fb53d"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicyTest.kt|badaf95ae2187b0281a54fc31d8180233fc021b0"
)
H35_CHECKS=(
    "app/src/androidTest/java/studio/guitarlab/app/SettingsCalibrationModalInstrumentedTest.kt|ff7ba85afe249de15317af3ae287468b7a71ff1f"
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|871a0470dac647d09d3e868f911c34505c80eee6"
    "app/src/main/java/studio/guitarlab/app/ui/StudioLatencyCalibration.kt|335064b36639480e60178c5c414fd6d3e9ad29d1"
    "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt|d6601560b43fd4c3f8b0254be13156badb9dcb2a"
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|ed03b956ee8aa49551bfa2786cf8b060d9bca56e"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|1604363d7e1801c3a9dc3c7833ed6abfc2aa76e0"
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/LatencyCalibrationStimulusPolicy.kt|d899c898d91b674ae8baf4ec14d2c1093f4e9e35"
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/LatencyCalibrationStimulusPolicyTest.kt|7a09cc120536fbfd2379d8d1badd5ce1797fbd65"
    "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt|60b4cf1d1dc79b9240d260ccad2c4bf074cf6285"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/TakeManagementPolicy.kt|885e8299afd51361186d6bd1a4a45a9953ff6e45"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectCodecCompatibilityTest.kt|dcc828a0c57ad6a1f5f379170f817e1ea2146332"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/TakeManagementPolicyTest.kt|dcfaf426a2692f6e1ca0eee58b061c5fc0ce6688"
)
H35A_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/StudioLatencyCalibration.kt|4316af223c9d12a722fb52a0e05f2a1eaf665104"
)
H36_CHECKS=(
    "app/src/androidTest/java/studio/guitarlab/app/SettingsCalibrationModalInstrumentedTest.kt|df31fc2dcc05ef619dc79062e9b29666ed176dd4"
    "app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt|5f8560042219f51216e02637e3b48f22ca947623"
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|6c98b72b674743eeeebfe3991c0636e824ef8a09"
)

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
h29_ready() { checks_ready H29_CHECKS; }
h30_ready() { checks_ready H30_CHECKS; }
h31_ready() { checks_ready H31_CHECKS; }
h32_ready() { checks_ready H32_CHECKS; }
h33_ready() { checks_ready H33_CHECKS; }
h33a_ready() { checks_ready H33A_CHECKS; }
h33b_ready() { checks_ready H33B_CHECKS; }
h34_ready() { checks_ready H34_CHECKS; }
h35_ready() { checks_ready H35_CHECKS; }
h35a_ready() { checks_ready H35A_CHECKS; }
h36_ready() { checks_ready H36_CHECKS; }

decode_verified_patch() {
    local label="$1" encoded="$2" archive_sha="$3" patch_sha="$4" output="$5" archive actual
    [[ -f "$encoded" ]] || { echo "Missing $label source archive: $encoded" >&2; return 1; }
    archive="$(mktemp)"
    trap 'rm -f "$archive"' RETURN
    base64 -d "$encoded" > "$archive"
    gzip -t "$archive"
    actual="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual" == "$archive_sha" ]] || { echo "$label source archive SHA-256 mismatch: $actual" >&2; return 1; }
    gzip -dc "$archive" > "$output"
    actual="$(sha256sum "$output" | awk '{print $1}')"
    [[ "$actual" == "$patch_sha" ]] || { echo "$label source patch SHA-256 mismatch: $actual" >&2; return 1; }
    rm -f "$archive"
    trap - RETURN
}

verify_new_patch() {
    local label="$1" encoded="$2" archive_sha="$3" patch_sha="$4" decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_verified_patch "$label" "$encoded" "$archive_sha" "$patch_sha" "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

apply_new_patch() {
    local label="$1" encoded="$2" archive_sha="$3" patch_sha="$4" decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_verified_patch "$label" "$encoded" "$archive_sha" "$patch_sha" "$decoded"
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

verify_new_patch H29 "$H29_PATCH_PART" "$H29_ARCHIVE_SHA256" "$H29_PATCH_SHA256"
verify_new_patch H30 "$H30_PATCH_PART" "$H30_ARCHIVE_SHA256" "$H30_PATCH_SHA256"
verify_new_patch H31 "$H31_PATCH_PART" "$H31_ARCHIVE_SHA256" "$H31_PATCH_SHA256"
verify_new_patch H32 "$H32_PATCH_PART" "$H32_ARCHIVE_SHA256" "$H32_PATCH_SHA256"
verify_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
verify_new_patch H33a "$H33A_PATCH_PART" "$H33A_ARCHIVE_SHA256" "$H33A_PATCH_SHA256"
verify_new_patch H33b "$H33B_PATCH_PART" "$H33B_ARCHIVE_SHA256" "$H33B_PATCH_SHA256"
verify_new_patch H34 "$H34_PATCH_PART" "$H34_ARCHIVE_SHA256" "$H34_PATCH_SHA256"
verify_new_patch H35 "$H35_PATCH_PART" "$H35_ARCHIVE_SHA256" "$H35_PATCH_SHA256"
verify_new_patch H35a "$H35A_PATCH_PART" "$H35A_ARCHIVE_SHA256" "$H35A_PATCH_SHA256"
verify_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"

if h36_ready; then
    echo "Source patch tail already materialized through H36"
    exit 0
fi
if h35a_ready; then
    apply_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"
    h36_ready || { echo "H36 applied but final H36 hashes do not match." >&2; exit 1; }
    echo "Source patch tail materialized through H36 with verified final hashes"
    exit 0
fi
if h35_ready; then
    apply_new_patch H35a "$H35A_PATCH_PART" "$H35A_ARCHIVE_SHA256" "$H35A_PATCH_SHA256"
    h35a_ready || { echo "H35a applied but final H35a hashes do not match." >&2; exit 1; }
    apply_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"
    h36_ready || { echo "H36 applied but final H36 hashes do not match." >&2; exit 1; }
    echo "Source patch tail materialized through H36 with verified final hashes"
    exit 0
fi
if h34_ready; then
    apply_new_patch H35 "$H35_PATCH_PART" "$H35_ARCHIVE_SHA256" "$H35_PATCH_SHA256"
    h35_ready || { echo "H35 applied but final H35 hashes do not match." >&2; exit 1; }
    apply_new_patch H35a "$H35A_PATCH_PART" "$H35A_ARCHIVE_SHA256" "$H35A_PATCH_SHA256"
    h35a_ready || { echo "H35a applied but final H35a hashes do not match." >&2; exit 1; }
    apply_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"
    h36_ready || { echo "H36 applied but final H36 hashes do not match." >&2; exit 1; }
    echo "Source patch tail materialized through H36 with verified final hashes"
    exit 0
fi
if h33b_ready; then
    apply_new_patch H34 "$H34_PATCH_PART" "$H34_ARCHIVE_SHA256" "$H34_PATCH_SHA256"
    h34_ready || { echo "H34 applied but final H34 hashes do not match." >&2; exit 1; }
    apply_new_patch H35 "$H35_PATCH_PART" "$H35_ARCHIVE_SHA256" "$H35_PATCH_SHA256"
    h35_ready || { echo "H35 applied but final H35 hashes do not match." >&2; exit 1; }
    apply_new_patch H35a "$H35A_PATCH_PART" "$H35A_ARCHIVE_SHA256" "$H35A_PATCH_SHA256"
    h35a_ready || { echo "H35a applied but final H35a hashes do not match." >&2; exit 1; }
    apply_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"
    h36_ready || { echo "H36 applied but final H36 hashes do not match." >&2; exit 1; }
    echo "Source patch tail materialized through H36 with verified final hashes"
    exit 0
fi
if h33a_ready; then
    apply_new_patch H33b "$H33B_PATCH_PART" "$H33B_ARCHIVE_SHA256" "$H33B_PATCH_SHA256"
    h33b_ready || { echo "H33b applied but final H33b hashes do not match." >&2; exit 1; }
    apply_new_patch H34 "$H34_PATCH_PART" "$H34_ARCHIVE_SHA256" "$H34_PATCH_SHA256"
    h34_ready || { echo "H34 applied but final H34 hashes do not match." >&2; exit 1; }
    apply_new_patch H35 "$H35_PATCH_PART" "$H35_ARCHIVE_SHA256" "$H35_PATCH_SHA256"
    h35_ready || { echo "H35 applied but final H35 hashes do not match." >&2; exit 1; }
    apply_new_patch H35a "$H35A_PATCH_PART" "$H35A_ARCHIVE_SHA256" "$H35A_PATCH_SHA256"
    h35a_ready || { echo "H35a applied but final H35a hashes do not match." >&2; exit 1; }
    apply_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"
    h36_ready || { echo "H36 applied but final H36 hashes do not match." >&2; exit 1; }
    echo "Source patch tail materialized through H36 with verified final hashes"
    exit 0
fi

# Detect the highest valid materialized stage. Later blocks legitimately modify files
# whose earlier-stage hashes no longer match, so never walk backwards from H33/H32.
if h33_ready; then
    :
elif h32_ready; then
    apply_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
    h33_ready || { echo "H33 applied but final H33 hashes do not match." >&2; exit 1; }
elif h31_ready; then
    apply_new_patch H32 "$H32_PATCH_PART" "$H32_ARCHIVE_SHA256" "$H32_PATCH_SHA256"
    h32_ready || { echo "H32 applied but final H32 hashes do not match." >&2; exit 1; }
    apply_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
    h33_ready || { echo "H33 applied but final H33 hashes do not match." >&2; exit 1; }
elif h30_ready; then
    apply_new_patch H31 "$H31_PATCH_PART" "$H31_ARCHIVE_SHA256" "$H31_PATCH_SHA256"
    h31_ready || { echo "H31 applied but final H31 hashes do not match." >&2; exit 1; }
    apply_new_patch H32 "$H32_PATCH_PART" "$H32_ARCHIVE_SHA256" "$H32_PATCH_SHA256"
    h32_ready || { echo "H32 applied but final H32 hashes do not match." >&2; exit 1; }
    apply_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
    h33_ready || { echo "H33 applied but final H33 hashes do not match." >&2; exit 1; }
elif h29_ready; then
    apply_new_patch H30 "$H30_PATCH_PART" "$H30_ARCHIVE_SHA256" "$H30_PATCH_SHA256"
    h30_ready || { echo "H30 applied but final H30 hashes do not match." >&2; exit 1; }
    apply_new_patch H31 "$H31_PATCH_PART" "$H31_ARCHIVE_SHA256" "$H31_PATCH_SHA256"
    h31_ready || { echo "H31 applied but final H31 hashes do not match." >&2; exit 1; }
    apply_new_patch H32 "$H32_PATCH_PART" "$H32_ARCHIVE_SHA256" "$H32_PATCH_SHA256"
    h32_ready || { echo "H32 applied but final H32 hashes do not match." >&2; exit 1; }
    apply_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
    h33_ready || { echo "H33 applied but final H33 hashes do not match." >&2; exit 1; }
else
    apply_new_patch H29 "$H29_PATCH_PART" "$H29_ARCHIVE_SHA256" "$H29_PATCH_SHA256"
    h29_ready || { echo "H29 applied but final H29 hashes do not match." >&2; exit 1; }
    apply_new_patch H30 "$H30_PATCH_PART" "$H30_ARCHIVE_SHA256" "$H30_PATCH_SHA256"
    h30_ready || { echo "H30 applied but final H30 hashes do not match." >&2; exit 1; }
    apply_new_patch H31 "$H31_PATCH_PART" "$H31_ARCHIVE_SHA256" "$H31_PATCH_SHA256"
    h31_ready || { echo "H31 applied but final H31 hashes do not match." >&2; exit 1; }
    apply_new_patch H32 "$H32_PATCH_PART" "$H32_ARCHIVE_SHA256" "$H32_PATCH_SHA256"
    h32_ready || { echo "H32 applied but final H32 hashes do not match." >&2; exit 1; }
    apply_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
    h33_ready || { echo "H33 applied but final H33 hashes do not match." >&2; exit 1; }
fi

apply_new_patch H33a "$H33A_PATCH_PART" "$H33A_ARCHIVE_SHA256" "$H33A_PATCH_SHA256"
h33a_ready || { echo "H33a applied but final H33a hashes do not match." >&2; exit 1; }

apply_new_patch H33b "$H33B_PATCH_PART" "$H33B_ARCHIVE_SHA256" "$H33B_PATCH_SHA256"
h33b_ready || { echo "H33b applied but final H33b hashes do not match." >&2; exit 1; }

apply_new_patch H34 "$H34_PATCH_PART" "$H34_ARCHIVE_SHA256" "$H34_PATCH_SHA256"
h34_ready || { echo "H34 applied but final H34 hashes do not match." >&2; exit 1; }

apply_new_patch H35 "$H35_PATCH_PART" "$H35_ARCHIVE_SHA256" "$H35_PATCH_SHA256"
h35_ready || { echo "H35 applied but final H35 hashes do not match." >&2; exit 1; }

apply_new_patch H35a "$H35A_PATCH_PART" "$H35A_ARCHIVE_SHA256" "$H35A_PATCH_SHA256"
h35a_ready || { echo "H35a applied but final H35a hashes do not match." >&2; exit 1; }

apply_new_patch H36 "$H36_PATCH_PART" "$H36_ARCHIVE_SHA256" "$H36_PATCH_SHA256"
h36_ready || { echo "H36 applied but final H36 hashes do not match." >&2; exit 1; }

echo "Source patch tail materialized through H36 with verified final hashes"
