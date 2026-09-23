#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUT="${1:-}"
PACKAGE_NAME="studio.guitarlab.app"
CERT_SHA1="42c70c79d3b5cb2cfebdf7ff80931bbbf4d10037"
PROJECT_ID="gbwapp-ef048"
REGION="us-central1"
RUNTIME="$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
BACKEND_URL="https://${REGION}-${PROJECT_ID}.cloudfunctions.net/remoteBackendStatus"

API_KEY="$(python3 - "$RUNTIME" <<'PY'
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
match = re.search(r'\.setApiKey\("([^"]+)"\)', text)
if not match:
    raise SystemExit("Firebase API key not found in Android runtime")
print(match.group(1))
PY
)"

signup="$(mktemp)"
backend="$(mktemp)"
cleanup="$(mktemp)"
token_file="$(mktemp)"
chmod 600 "$token_file"

cleanup_all() {
  set +e
  if [[ -s "$token_file" ]]; then
    token="$(cat "$token_file")"
    curl -sS       -o "$cleanup"       -X POST       -H 'Content-Type: application/json'       -H "X-Android-Package: $PACKAGE_NAME"       -H "X-Android-Cert: $CERT_SHA1"       "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=$API_KEY"       --data "{\"idToken\":\"$token\"}" >/dev/null 2>&1 || true
  fi
  rm -f "$signup" "$backend" "$cleanup" "$token_file"
}
trap cleanup_all EXIT

signup_status="$(
  curl -sS     -o "$signup"     -w '%{http_code}'     -X POST     -H 'Content-Type: application/json'     -H "X-Android-Package: $PACKAGE_NAME"     -H "X-Android-Cert: $CERT_SHA1"     "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY"     --data '{"returnSecureToken":true}'
)"

if [[ "$signup_status" != "200" ]]; then
  python3 - "$signup" "$signup_status" <<'PY'
import json, sys
status = sys.argv[2]
try:
    body = json.load(open(sys.argv[1], encoding="utf-8"))
except Exception:
    body = {}
error = body.get("error", {})
message = str(error.get("message") or "UNKNOWN").replace("\n", " ")[:160]
print(f"anonymous_client_signup=FAIL http={status} message={message}")
PY
  exit 1
fi

python3 - "$signup" "$token_file" <<'PY'
import json, sys
body = json.load(open(sys.argv[1], encoding="utf-8"))
token = body.get("idToken")
local_id = body.get("localId")
if not token or not local_id:
    raise SystemExit("anonymous signup response missing token or localId")
open(sys.argv[2], "w", encoding="utf-8").write(token)
print("anonymous_client_signup=PASS")
PY

TOKEN="$(cat "$token_file")"
backend_status="$(
  curl -sS     -o "$backend"     -w '%{http_code}'     -X POST     -H 'Content-Type: application/json'     -H "Authorization: Bearer $TOKEN"     "$BACKEND_URL"     --data '{"data":{}}'
)"

if [[ "$backend_status" != "200" ]]; then
  python3 - "$backend" "$backend_status" <<'PY'
import json, sys
status = sys.argv[2]
try:
    body = json.load(open(sys.argv[1], encoding="utf-8"))
except Exception:
    body = {}
error = body.get("error", {})
code = str(error.get("status") or "UNKNOWN")
message = str(error.get("message") or "UNKNOWN").replace("\n", " ")[:160]
print(f"anonymous_backend_authorization=FAIL http={status} code={code} message={message}")
PY
  exit 1
fi

python3 - "$backend" <<'PY'
import json, sys
body = json.load(open(sys.argv[1], encoding="utf-8"))
payload = body.get("result")
if payload is None:
    payload = body.get("data")
if not isinstance(payload, dict):
    raise SystemExit("anonymous_backend_authorization=FAIL malformed callable response")
if payload.get("available") is not True:
    raise SystemExit(f"anonymous_backend_authorization=FAIL available={payload.get('available')!r}")
if payload.get("schemaVersion") != 1:
    raise SystemExit(f"anonymous_backend_authorization=FAIL schemaVersion={payload.get('schemaVersion')!r}")
print("anonymous_backend_authorization=PASS")
PY

cleanup_status="$(
  curl -sS     -o "$cleanup"     -w '%{http_code}'     -X POST     -H 'Content-Type: application/json'     -H "X-Android-Package: $PACKAGE_NAME"     -H "X-Android-Cert: $CERT_SHA1"     "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=$API_KEY"     --data "{\"idToken\":\"$TOKEN\"}"
)"

[[ "$cleanup_status" == "200" ]] || {
  echo "anonymous_client_cleanup=FAIL http=$cleanup_status" >&2
  exit 1
}
: > "$token_file"

{
  printf 'anonymous_client_signup=PASS\n'
  printf 'anonymous_backend_authorization=PASS\n'
  printf 'anonymous_client_cleanup=PASS\n'
  printf 'package=%s\n' "$PACKAGE_NAME"
  printf 'certificate_sha1=%s\n' "$CERT_SHA1"
} | if [[ -n "$OUT" ]]; then tee "$OUT"; else cat; fi
