#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12p.sh"
PATCH_B64="$ROOT/.source-parts/U12qFirebaseAnonymousAuthEnable.patch.b64"
PATCH_B64_BLOB="76712b96b06af3ec29c86229c83bf814a8801795"
PATCH_BLOB="a2d39af0afa88baccba7710f0b67f3bee2e178a2"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

FILE=".github/workflows/u7-cloud-backend.yml"
HASH="6326a8aa698be8a3a8276eb202b58df869ab015c"

ready() {
  [[ -f "$ROOT/$FILE" ]] &&
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$HASH" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'name: Enable and verify Firebase anonymous authentication' "$ROOT/$FILE"
  grep -q 'id-token: write' "$ROOT/$FILE"
  grep -q 'ensure-anonymous-auth.sh u7-auth-config.json' "$ROOT/$FILE"
  grep -q 'verify-anonymous-auth-client.sh u7-auth-client-evidence.txt' "$ROOT/$FILE"
  grep -q 'gbw-github-deployer@gbwapp-ef048.iam.gserviceaccount.com' "$ROOT/$FILE"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12q Firebase anonymous auth enable payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12q base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12q decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12q Firebase anonymous auth enable"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12p materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12q final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12q Firebase anonymous auth enable"
