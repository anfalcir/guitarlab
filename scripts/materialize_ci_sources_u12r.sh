#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12q.sh"
PATCH_B64="$ROOT/.source-parts/U12rExternalDependencyRetry.patch.b64"
PATCH_B64_BLOB="742d0711119b600cd9f6f3b64653d5819d033bf2"
PATCH_BLOB="daad992ed8bd11766deddf91214424a93923d928"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

FILE=".github/workflows/u7-cloud-backend.yml"
HASH="6d0b4f20959668915b782ea9d69a5d16b88fe4e6"

ready() {
  [[ -f "$ROOT/$FILE" ]] &&
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$HASH" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'name: Enable and verify Firebase anonymous authentication' "$ROOT/$FILE"
  grep -q 'for attempt in 1 2 3 4' "$ROOT/$FILE"
  grep -q 'retrying external dependency fetch' "$ROOT/$FILE"
  grep -q 'ensure-anonymous-auth.sh u7-auth-config.json' "$ROOT/$FILE"
  grep -q 'verify-anonymous-auth-client.sh u7-auth-client-evidence.txt' "$ROOT/$FILE"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12r external dependency retry payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12r base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12r decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12r external dependency retry"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12q materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12r final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12r external dependency retry"
