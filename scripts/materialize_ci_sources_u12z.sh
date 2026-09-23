#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12w.sh"
PATCH_B64="$ROOT/.source-parts/U12zRc12ImportRecoveryAndUx.patch.b64"
PATCH_B64_BLOB="9e78111750b9516d85529b099c88ac66d90c1990"
PATCH_BLOB="d6c05e1f3da40b88da38f263b64c31379ae434bf"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  "app/build.gradle.kts"
  "app/src/androidTest/java/studio/guitarlab/app/HomeProjectLibraryInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityStore.kt"
  "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/CloudSeparationLoginDialog.kt"
  "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt"
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
  "app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicy.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectManagedMediaStore.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/StemSetProjectPublisher.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedOperationDomain.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicyTest.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/StemSetProjectPublisherTest.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/StemSetRecoveryTest.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedOperationDomainTest.kt"
  "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
  "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/ManagedStemSetPublisher.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteCloudAuthClient.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteSeparationNotifier.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteStemStaging.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteWorkerRetryPolicy.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/ManagedStemSetPublisherTest.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifierTest.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicyTest.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteSeparationNotificationPolicyTest.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteStemStagingTest.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteWorkerRetryPolicyTest.kt"
)

declare -a HASHES=(
  "2d83b002a9b1c15af04e74d6da968e582b9b2247"
  "4c9b65fc988a9a59a039f270725604936eed1fbf"
  "06728d7820ba9d53166dd59eeaaa8ac76207cfb5"
  "61f1bfe8318fa67a579ee69868edf1e0031af110"
  "04fe188258c88204e6bbe04cb74d43e88a77bbe6"
  "96ff4ae7db7f256cced701a54b9ca3c5df772b6a"
  "7ef8f6a082a883d1ef54a2204fa56cb2f09d90bd"
  "0ea447861ba33d458b0d37a4f99451d2c1293ea6"
  "8155b33ae2f45718ec68365469ea9e63b2a33859"
  "1068948d52d80e7c8787095f5d2dce4093a8edc4"
  "53b57189c5e430da2caf1eb6e49d1ea1db79f266"
  "1b5f27f5bafc1f5a3b1a38400a71b716834b6787"
  "61e22ca87e940636e29eff6298800b02acb1ff7e"
  "119e488eb3b18f776165e76a6d1bc62169302d78"
  "1b20b033c0f94e907daf2a918a9f5d1b36336143"
  "13458f3cd46cca1e484e74d93f297e0a64e2f313"
  "ef3cb0038484b64d1b9d70b656637761202b7c26"
  "dd6bd456a72971b77bb6e4b6f5cfa334f44500d0"
  "2010cdaf8ba2d674dc1386a4cd54740ab90ecf67"
  "97da5df926de093647bd79e624902d1bf76adc1f"
  "ca7d9878e8d8eb6264c93021ff1c37ceba6b09a7"
  "81247b7177aa26b6f2428b3a7c452a2b056ed882"
  "8e693092c4bfc0ba3e98b4e73c8134d1e548ea8f"
  "a6b026f422e0468ec05edc54689aa8c2bbf2a8e1"
  "6489353d8b0c95f7d1b5f58036cef09381a112c4"
  "fd0c284aec7044c0acf1a0e8df03c70f400f6da4"
  "f298531862ab2c82e3e4103583921c3ab71bb569"
  "27c6bf9b7815d1f6d1e6d25f6b38ec210d801f3d"
  "1c965826e01b64d68e22ba797cd9550b1dd24b55"
  "8a1fb6de3c967e16e70042595c9130f4ef464d0a"
  "61badeffd4a0e2edae45ba9ab8b24b382c2a3ada"
  "1e364fb0013fbe3dd63ba0eccaa0349dfbce3be0"
  "ba5b74d477a8efb7f52b6cca5d5fb8e8143cc149"
  "d2c85ef288b97ffadeb07e3c95d9ea5d7ab6aa40"
  "4cd87facb8a18b9c2fd2e7010e7c33fb1fa39166"
  "3834999f0d28305d3f57843f8f0f18cf02e4dcd9"
  "c23740d337841c498c2bcad866ee30f5b8d54d24"
  "a86fa5c29217413e7e453aa020ba458f1b4419bd"
  "855c8bfdd02837cdc8db323c658ed86c96d2bcc6"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc12"' "$ROOT/app/build.gradle.kts"
  grep -q 'versionCode = 32' "$ROOT/app/build.gradle.kts"
  grep -q 'IMPORT_FAILED' "$ROOT/core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
  grep -q 'interface RemoteStemPayload' "$ROOT/core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
  grep -q 'getFile(partial).await()' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  ! grep -q 'getBytes(stem.bytes)' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'repairLegacyImportExpirations' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt"
  grep -q 'RemoteWorkerRetryPolicy.nextAttempt' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'setOngoing(copy.ongoing)' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteSeparationNotifier.kt"
  grep -q 'POST_NOTIFICATIONS' "$ROOT/app/src/main/AndroidManifest.xml"
  grep -q 'activity-clear-history' "$ROOT/app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  grep -q 'records.filter { it.state.isActive }' "$ROOT/core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedOperationDomain.kt"
  grep -q 'prepare-resume-import' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
  grep -q 'prepare-cloud-auth-action' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
  grep -q 'guitarlab_home_preferences' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  grep -q 'restoreSortOrder' "$ROOT/core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectLibraryPolicy.kt"
  grep -q 'CLIENT_MEMORY_PRESSURE' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt"
  grep -q 'isUnresolved' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12z rc12 payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12z base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12z decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12z rc12 import recovery and UX hardening"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12w materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12z final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12z rc12 import recovery and UX hardening"
