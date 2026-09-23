#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12o.sh"
PATCH_B64="$ROOT/.source-parts/U12pFirebaseClientAuthVerification.patch.b64"
PATCH_B64_BLOB="264cf8d08978dcdcd851e1e2ae4ea91ef16f4683"
PATCH_BLOB="fba700a39b39f54e5cd35a8a44fe28384562de77"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  ".github/workflows/u7-cloud-backend.yml"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "cloud/remote-separation/scripts/bootstrap-github-actions.sh"
  "cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
)
declare -a HASHES=(
  "2ba4c6325a83f0f0803bc99c10ab5c3c7afc2762"
  "81f082eef7f50e7c6fbc7a74a274cb9cc7354c3e"
  "4a5fb8739254aa466dc5c9a9cd45b8653b2396d0"
  "c710620f4b2243912a6f9da0fef2ddec68b61cdc"
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
  grep -q 'val errorCode = job.errorCode' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  grep -q 'accounts:signUp' "$ROOT/cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
  grep -q 'accounts:delete' "$ROOT/cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
  grep -q 'X-Android-Package' "$ROOT/cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
  grep -q '42c70c79d3b5cb2cfebdf7ff80931bbbf4d10037' "$ROOT/cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
  grep -q 'roles/firebaseauth.admin' "$ROOT/cloud/remote-separation/scripts/bootstrap-github-actions.sh"
  grep -q 'Verify Firebase anonymous client authentication' "$ROOT/.github/workflows/u7-cloud-backend.yml"
  grep -q "contains(github.event.head_commit.message, '\[run u7 cloud\]')" "$ROOT/.github/workflows/u7-cloud-backend.yml"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12p Firebase client-auth verification payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12p base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12p decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12p Firebase client-auth verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12o materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12p final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12p Firebase client-auth verification"
