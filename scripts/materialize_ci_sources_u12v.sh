#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12u.sh"
PATCH_B64="$ROOT/.source-parts/U12vEmailAuthFalseField.patch.b64"
PATCH_B64_BLOB="7f1bca81f56967b4c87302914038d0ab0bc09573"
PATCH_BLOB="2a5612d2a1290796e1d63b7b38ee2bdbcee10c76"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

FILE="cloud/remote-separation/scripts/ensure-email-password-auth.sh"
HASH="9dacf805aec0b1d2427eeab28a7cc0058e34aa99"

ready() {
  [[ -f "$ROOT/$FILE" ]] &&
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$HASH" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'anonymous.get("enabled") is True' "$ROOT/$FILE"
  grep -q 'email_password_auth_after=ENABLED' "$ROOT/$FILE"
  grep -q 'anonymous_auth_after=DISABLED' "$ROOT/$FILE"
  grep -q 'versionName = "0.5.0-rc11"' "$ROOT/app/build.gradle.kts"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12v email-auth false-field payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12v base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12v decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12v email-auth false-field fix"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12u materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12v final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12v email-auth false-field fix"
