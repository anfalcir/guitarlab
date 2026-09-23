#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12r.sh"
PATCH_B64="$ROOT/.source-parts/U12sDecoupleAuthGate.patch.b64"
PATCH_B64_BLOB="be49001f349269a5478baa923253d3695200e184"
PATCH_BLOB="460b03bab200dac7d8af0f4d3b26ddf53066adc6"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

FILE=".github/workflows/u7-cloud-backend.yml"
HASH="c272d3cccd618ecf178b2f3ca0a47d69d1eaa284"

ready() {
  [[ -f "$ROOT/$FILE" ]] &&
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$HASH" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'name: Enable and verify Firebase anonymous authentication' "$ROOT/$FILE"
  ! awk '/auth_config:/,/deploy:/' "$ROOT/$FILE" | grep -q 'needs: verify'
  grep -q 'for attempt in 1 2 3 4' "$ROOT/$FILE"
  grep -q 'ensure-anonymous-auth.sh u7-auth-config.json' "$ROOT/$FILE"
  grep -q 'verify-anonymous-auth-client.sh u7-auth-client-evidence.txt' "$ROOT/$FILE"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12s Auth gate decoupling payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12s base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12s decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12s Auth gate decoupling"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12r materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12s final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12s Auth gate decoupling"
