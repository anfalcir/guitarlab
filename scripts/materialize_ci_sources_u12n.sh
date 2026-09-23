#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12l.sh"
PATCH_B64="$ROOT/.source-parts/U12nIdentityToolkitAuthProvisioning.patch.b64"
PATCH_B64_BLOB="5f26caaff02b7400a561714a8de744c9cebdc8dd"
PATCH_BLOB="4a5564f5036ec1d7d31aad034248723883832767"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

declare -a FILES=(
  ".github/workflows/u7-cloud-backend.yml"
  "cloud/remote-separation/scripts/deploy-functions.sh"
  "cloud/remote-separation/scripts/ensure-anonymous-auth.sh"
)

declare -a HASHES=(
  "64a6481dc5a3d65925870ae769f0d9f008ebb5b3"
  "80ff89f4d09751643dc17ca288f42aa373ed4baa"
  "f1f3637b7880e8b2ea00663e593befdf4be66e49"
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
  grep -q 'identitytoolkit.googleapis.com/admin/v2/projects/' "$ROOT/cloud/remote-separation/scripts/ensure-anonymous-auth.sh"
  grep -q 'updateMask=signIn.anonymous.enabled' "$ROOT/cloud/remote-separation/scripts/ensure-anonymous-auth.sh"
  grep -q 'anonymous_auth_after=ENABLED' "$ROOT/cloud/remote-separation/scripts/ensure-anonymous-auth.sh"
  grep -q 'ensure-anonymous-auth.sh u7-auth-config.json' "$ROOT/.github/workflows/u7-cloud-backend.yml"
  ! grep -q -- '--only auth' "$ROOT/.github/workflows/u7-cloud-backend.yml"
  grep -q 'ensure-anonymous-auth.sh' "$ROOT/cloud/remote-separation/scripts/deploy-functions.sh"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12n Identity Toolkit auth provisioning payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12n base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12n decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12n Identity Toolkit auth provisioning"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12l materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12n final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12n Identity Toolkit auth provisioning"
