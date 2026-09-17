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

if h33_ready; then
    echo "Source patch tail already materialized through H33"
    exit 0
fi
if ! h29_ready; then
    apply_new_patch H29 "$H29_PATCH_PART" "$H29_ARCHIVE_SHA256" "$H29_PATCH_SHA256"
    h29_ready || { echo "H29 applied but final H29 hashes do not match." >&2; exit 1; }
fi
if ! h30_ready; then
    apply_new_patch H30 "$H30_PATCH_PART" "$H30_ARCHIVE_SHA256" "$H30_PATCH_SHA256"
    h30_ready || { echo "H30 applied but final H30 hashes do not match." >&2; exit 1; }
fi
if ! h31_ready; then
    apply_new_patch H31 "$H31_PATCH_PART" "$H31_ARCHIVE_SHA256" "$H31_PATCH_SHA256"
    h31_ready || { echo "H31 applied but final H31 hashes do not match." >&2; exit 1; }
fi
if ! h32_ready; then
    apply_new_patch H32 "$H32_PATCH_PART" "$H32_ARCHIVE_SHA256" "$H32_PATCH_SHA256"
    h32_ready || { echo "H32 applied but final H32 hashes do not match." >&2; exit 1; }
fi
if ! h33_ready; then
    apply_new_patch H33 "$H33_PATCH_PART" "$H33_ARCHIVE_SHA256" "$H33_PATCH_SHA256"
    h33_ready || { echo "H33 applied but final H33 hashes do not match." >&2; exit 1; }
fi

echo "Source patch tail materialized through H33 with verified final hashes"
