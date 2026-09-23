#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12s.sh"
PATCH_B64="$ROOT/.source-parts/U12tAnonymousBackendAuthorization.patch.b64"
PATCH_B64_BLOB="8d057eb6c021b8b04dac0337da3c45a5435651a7"
PATCH_BLOB="bbaa83c854df5926d83e61a61ee0cf93700e4062"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

FILE="cloud/remote-separation/scripts/verify-anonymous-auth-client.sh"
HASH="aeb6336cf905ec14d09a9a335a57ae9f67bc9fd7"

ready() {
  [[ -f "$ROOT/$FILE" ]] &&
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$HASH" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'remoteBackendStatus' "$ROOT/$FILE"
  grep -q 'anonymous_backend_authorization=PASS' "$ROOT/$FILE"
  grep -q 'Authorization: Bearer' "$ROOT/$FILE"
  grep -q 'accounts:delete' "$ROOT/$FILE"
  grep -q 'versionName = "0.5.0-rc10"' "$ROOT/app/build.gradle.kts"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12t anonymous backend authorization payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12t base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12t decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12t anonymous backend authorization"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12s materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12t final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12t anonymous backend authorization"
