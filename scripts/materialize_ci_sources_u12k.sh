#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12j.sh"
PATCH_B64="$ROOT/.source-parts/U12kRemoteOrphanReconciliation.patch.b64"
PATCH_B64_BLOB="9405a896b60e63a6f9feff119c2e19e8d86331b9"
PATCH_BLOB="890565cc1f37adef4ae75e38b20042ffb80a5c3e"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  "app/build.gradle.kts"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
  "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicyTest.kt"
)

declare -a HASHES=(
  "62ffb8bb6006d96d88e011e7a4e2d08d0526321c"
  "c56ad2f0cd482cbfa5beff7c3f1ac2e967815f5d"
  "152d189d8a8256ffdbfd66674cf28cee26056596"
  "9436e8c27c9308761fb408b3d47f3d218a176466"
  "3c735aad9845d50fd4fc65e60cfee4911a8621b0"
  "a2e5df01a85d1eb236a8cd2cfbb87b012ccb3572"
  "3d641a15ff5e1efa99f13ccce81eafa72a8e9df5"
  "cbdb8cd2890e8d9d7ab8804dcdbec29ddd0b7a4e"
  "73e8d61edc5d108587f6056a5eb2867439063918"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc9"' "$ROOT/app/build.gradle.kts"
  grep -q 'versionCode = 29' "$ROOT/app/build.gradle.kts"
  grep -q 'object RemoteMissingPolicy' "$ROOT/core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
  grep -q 'ERROR_REMOTE_JOB_NOT_FOUND' "$ROOT/core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
  grep -q 'backend.status(id)' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'CANCEL_UNCONFIRMED_' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'shouldRestoreAfterTerminalJob' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt"
  grep -q 'selectLatestForProject' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt"
  grep -q 'cancelRequestedWithoutRemoteTerminalizesLocally' "$ROOT/core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt"
  grep -q 'cancellationRetriesAreBoundedAndTerminalize' "$ROOT/platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicyTest.kt"
  grep -q 'recoveredOrphanExposesRetryAndPreservesAcceptedSource' "$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12k remote orphan reconciliation payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12k base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12k decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12k remote orphan reconciliation"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12j materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12k final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12k remote orphan reconciliation"
