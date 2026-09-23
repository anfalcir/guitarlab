#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12t.sh"
PATCH_B64="$ROOT/.source-parts/U12uStableFirebaseAccountAuth.patch.b64"
PATCH_B64_BLOB="96f4bbe227907058b6bce8013f08a8778109d658"
PATCH_BLOB="b46f144bcfaada86671c456f74fb8bf34d91abeb"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  ".github/workflows/u7-cloud-backend.yml"
  "app/build.gradle.kts"
  "app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt"
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  "app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt"
  "cloud/remote-separation/firebase.json"
  "cloud/remote-separation/scripts/deploy-functions.sh"
  "cloud/remote-separation/scripts/ensure-anonymous-auth.sh"
  "cloud/remote-separation/scripts/ensure-email-password-auth.sh"
  "cloud/remote-separation/scripts/verify-allowlisted-auth-users.sh"
  "cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteBackend.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteCloudAuthClient.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteCloudAuthPolicyTest.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifierTest.kt"
)

declare -a HASHES=(
  "e9c916c7930ef98bf28ef0595b0af4699671ad2d"
  "c6d1ea5cd2d0d08df0089bfb3d3dfb98c9b4a022"
  "26bb5b496a2152316dcaf9c978371df17d6dc9eb"
  "f2a685b461f5ec690dadd26da4316d4f6d584f28"
  "d027ef1163733bac3f00934e33bcbb4c794bd68b"
  "4ed6ad45f4d93d26577ab3115f79f530d0a6a570"
  "14f464344ce3b2fd836ef8c87068f03ec4b319f3"
  "683722c280b78ef49415673d56d82ab7b9f26fa0"
  "9c7fb3ca62817ad187b353e6ca06feae9fe632c5"
  "2ad3444175f10269438d2ebfcd5e1ff18e778a24"
  "0d4051b507dd0a870b3569b8f8fea235e9636c2a"
  "3ffce62a32c57a8a21c8e50a6eb25066a6807c6b"
  "ee528995c8c3b95ec59061e606b9bb9046461688"
  "a15209b09a81bc8fb15db7082a19e3d628d629cd"
  "bc5439a8c20f07878e5187af718b296c82ce1e96"
  "14304705df0855630d697d2b5b35f8049c26f641"
  "fc225b5f16b75583605436b3834343acb82a73e7"
  "8ca0646bff6fbbc44f21b9f9739fda6f8b4c0e33"
  "0fb25db21b089d56e397e152edd4368e2f163c2c"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc11"' "$ROOT/app/build.gradle.kts"
  grep -q 'versionCode = 31' "$ROOT/app/build.gradle.kts"
  grep -q 'signInWithEmailAndPassword' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteCloudAuthClient.kt"
  ! grep -R -q 'signInAnonymously' "$ROOT/platform/separation/src/main/kotlin"
  grep -q 'ACCOUNT_NOT_AUTHORIZED' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteCloudAuthClient.kt"
  grep -q 'settings-cloud-auth-dialog' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
  grep -q 'remoteCloudAuth.currentSession() == null' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  grep -q '"emailPassword": true' "$ROOT/cloud/remote-separation/firebase.json"
  grep -q '"anonymous": false' "$ROOT/cloud/remote-separation/firebase.json"
  grep -q 'allowlisted_password_users=PASS' "$ROOT/cloud/remote-separation/scripts/verify-allowlisted-auth-users.sh"
  grep -q 'ensure-email-password-auth.sh' "$ROOT/cloud/remote-separation/scripts/deploy-functions.sh"
  grep -q 'GBW_ALLOWED_UIDS: ${{ secrets.GBW_ALLOWED_UIDS }}' "$ROOT/.github/workflows/u7-cloud-backend.yml"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12u stable Firebase account auth payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12u base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12u decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12u stable Firebase account auth"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12t materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12u final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12u stable Firebase account auth"
