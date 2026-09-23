#!/usr/bin/env bash
set -euo pipefail

: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"

OUT="${1:-}"
TOKEN="$(gcloud auth print-access-token)"
ENDPOINT="https://identitytoolkit.googleapis.com/admin/v2/projects/${GBW_GCP_PROJECT}/config"

before="$(mktemp)"
after="$(mktemp)"
patched="$(mktemp)"
trap 'rm -f "$before" "$after" "$patched"' EXIT

curl -fsS -H "Authorization: Bearer $TOKEN" "$ENDPOINT" > "$before"

curl -fsS   -X PATCH   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   "$ENDPOINT?updateMask=signIn.anonymous.enabled"   --data '{"signIn":{"anonymous":{"enabled":true}}}'   > "$patched"

curl -fsS -H "Authorization: Bearer $TOKEN" "$ENDPOINT" > "$after"

python3 - "$before" "$after" "$patched" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    before = json.load(f)
with open(sys.argv[2], encoding="utf-8") as f:
    after = json.load(f)
with open(sys.argv[3], encoding="utf-8") as f:
    patched = json.load(f)

before_enabled = before.get("signIn", {}).get("anonymous", {}).get("enabled")
after_enabled = after.get("signIn", {}).get("anonymous", {}).get("enabled")
patched_enabled = patched.get("signIn", {}).get("anonymous", {}).get("enabled")

assert patched_enabled is True, f"PATCH response did not enable anonymous auth: {patched_enabled!r}"
assert after_enabled is True, f"remote config did not persist anonymous auth: {after_enabled!r}"

print(f"anonymous_auth_before={before_enabled!r}")
print("anonymous_auth_after=ENABLED")
PY

if [[ -n "$OUT" ]]; then
  cp "$after" "$OUT"
fi
