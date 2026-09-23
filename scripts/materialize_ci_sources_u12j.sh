#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12i.sh"
PATCH_B64="$ROOT/.source-parts/U12jRemoteSeparationDurability.patch.b64"
PATCH_B64_BLOB="1bd266bb9b9044c4ecf34665bcaa0bccf61b96a1"
PATCH_BLOB="598e821ebb529a105b5adf6725ef0c3c19ee4c95"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  "app/build.gradle.kts"
  "cloud/remote-separation/functions/src/index.ts"
  "cloud/remote-separation/functions/src/policy.ts"
  "cloud/remote-separation/functions/src/test/policy.test.ts"
  "cloud/remote-separation/schemas/remote-job.schema.json"
  "cloud/remote-separation/worker/gbw_worker.py"
  "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt"
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicyTest.kt"
)

declare -a HASHES=(
  "0c8cb950d4307ab4314b72c32b59157e7e305c56"
  "c6cef7d20d4ce953d3bc55800e1684dd8341a42d"
  "47989be3c808aad326b2d220c3e7a0ce2d3e39a1"
  "09c40f3bb340a03f36f49171dc7bd821568824bc"
  "77b8dafbb8a678f186aaa1d9143bb7686c092b88"
  "5f50ca3c2939f1c3deda62b51a184abfa616e852"
  "b8cb6e0486952489ae0e37ef36b194a84dec9c66"
  "7c0d98b12be226f16fce2a21f627ff9a72c581d1"
  "927b2249425e80ac89144b3392b9e9690f868280"
  "f57c0064101fec551a6c893d86ad306e8471ce9a"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc8"' "$ROOT/app/build.gradle.kts"
  grep -q 'sourceAssetId' "$ROOT/cloud/remote-separation/functions/src/index.ts"
  grep -q 'RemoteJobState.UPLOADING' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'recoverOrphanedProjects' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  grep -q 'job was cancelled before result commit' "$ROOT/cloud/remote-separation/worker/gbw_worker.py"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12j remote-separation durability payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || { echo "U12j base64 payload blob mismatch" >&2; exit 1; }
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || { echo "U12j decoded patch blob mismatch" >&2; exit 1; }

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12j remote-separation durability corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12i materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12j final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12j remote-separation durability corrective"
