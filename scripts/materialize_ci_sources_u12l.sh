#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12k.sh"
PATCH_B64="$ROOT/.source-parts/U12lFirebasePreflightActivityHardening.patch.b64"
PATCH_B64_BLOB="e2fdcd72cab7554538993101c25e7c2d558e0c82"
PATCH_BLOB="dac2d9d2a47c44e31a8e6b094d0a15608eb37426"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  ".github/workflows/u7-cloud-backend.yml"
  "app/build.gradle.kts"
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityStore.kt"
  "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt"
  "app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt"
  "cloud/remote-separation/firebase.json"
  "cloud/remote-separation/scripts/deploy-functions.sh"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedOperationDomain.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedOperationDomainTest.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteBackend.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifierTest.kt"
)

declare -a HASHES=(
  "d78bee29312641473e6e895c1037226b350d8f7b"
  "a323fd0ed37c71317411c6576633321f69d52a11"
  "0f1e395b8483c1720ac108e8580d76a344cd5c10"
  "83f149df8c45ddc1e0d9c0d62af74dee56e37624"
  "fa199b2d7dd6d59c5001af7c9f9d7114e520ff56"
  "d961486eea8409c7aa4e9cc782e1b3cb1fe261b7"
  "049befaff959dc95994298f72043bc48dedd3ce2"
  "cbae0ec290e016905cf30f4a8203c9d881fda1e7"
  "0e3c472508881719dfb517b96981f4163a1a95a4"
  "74a23018a8ff65ac2105f1cc4b649fd779050417"
  "6de747797bda724203b4852de52fd42270880877"
  "a82d882d4e55f1b691e24d6dd4ce7cbad60c4b58"
  "c5c5b3ad7ad1e99799e0a8f87c6c5bf2194c9791"
  "88d683f01a25d94dafba0d8787193839820ff18f"
  "983ee86fba2a354c6606ad4b04cb59e05a6bdd09"
  "9dff052068a8cb280e7e136a0e3039c9c60bb14f"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc10"' "$ROOT/app/build.gradle.kts"
  grep -q 'versionCode = 30' "$ROOT/app/build.gradle.kts"
  grep -q 'AUTH_PROVIDER_DISABLED' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt"
  grep -q 'FIRESTORE_PERMISSION_DENIED' "$ROOT/platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifierTest.kt"
  grep -q 'failure.durableCode(runAttemptCount)' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'activity-clear-resolved' "$ROOT/app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  grep -q 'terminalizeMissingProjects' "$ROOT/core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedOperationDomain.kt"
  grep -q 'activityStore.reconcileMissingProjects' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  grep -q '"anonymous": true' "$ROOT/cloud/remote-separation/firebase.json"
  grep -q -- '--only auth' "$ROOT/.github/workflows/u7-cloud-backend.yml"
  grep -q 'anonymous_auth=ENABLED' "$ROOT/.github/workflows/u7-cloud-backend.yml"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12l Firebase preflight/activity hardening payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12l base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12l decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12l Firebase preflight/activity hardening"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12k materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12l final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12l Firebase preflight/activity hardening"
