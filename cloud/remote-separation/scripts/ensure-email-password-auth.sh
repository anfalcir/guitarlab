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

curl -fsS   -X PATCH   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   "$ENDPOINT?updateMask=signIn.email.enabled,signIn.email.passwordRequired,signIn.anonymous.enabled"   --data '{"signIn":{"email":{"enabled":true,"passwordRequired":true},"anonymous":{"enabled":false}}}'   > "$patched"

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

def state(doc):
    sign = doc.get("signIn", {})
    email = sign.get("email", {})
    anonymous = sign.get("anonymous", {})
    return (
        email.get("enabled"),
        email.get("passwordRequired"),
        anonymous.get("enabled"),
    )

before_state = state(before)
patched_state = state(patched)
after_state = state(after)

assert patched_state == (True, True, False), f"PATCH response mismatch: {patched_state!r}"
assert after_state == (True, True, False), f"remote auth config mismatch: {after_state!r}"

print(f"email_auth_before={before_state[0]!r}")
print(f"password_required_before={before_state[1]!r}")
print(f"anonymous_auth_before={before_state[2]!r}")
print("email_password_auth_after=ENABLED")
print("anonymous_auth_after=DISABLED")
PY

if [[ -n "$OUT" ]]; then
  cp "$after" "$OUT"
fi
