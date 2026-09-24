#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?}"
: "${GBW_BUCKET:?}"
GBW_REGION="${GBW_REGION:-us-central1}"
GBW_JOB_NAME="${GBW_JOB_NAME:-gbw-demucs}"
MODEL_SHA256="${MODEL_SHA256:?}"
: "${GBW_ALLOWED_UIDS:?}"
DELIVERABLES=(backing guitar)
RUN_TOKEN="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
TEST_UID="$(printf '%s' "${GBW_ALLOWED_UIDS%%,*}" | xargs)"
test -n "$TEST_UID"
JOB_ID="$(python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
)"
PROJECT_ID="$(python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
)"
SOURCE_ASSET_ID="u4-source-${RUN_TOKEN//[^A-Za-z0-9._:-]/-}"
RECOVERY_JOB_ID="$(python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
)"
PREFIX="remote/v1/users/${TEST_UID}/jobs/${JOB_ID}"
RECOVERY_PREFIX="remote/v1/users/${TEST_UID}/jobs/${RECOVERY_JOB_ID}"
INPUT_PATH="${PREFIX}/input/source.wav"
TMP="$(mktemp -d)"
cleanup() {
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
  gcloud storage rm "gs://${GBW_BUCKET}/${RECOVERY_PREFIX}/**" --recursive >/dev/null 2>&1 || true
  TOKEN="$(gcloud auth print-access-token 2>/dev/null || true)"
  if [[ -n "$TOKEN" ]]; then
    curl --silent -X DELETE -H "Authorization: Bearer ${TOKEN}" "https://firestore.googleapis.com/v1/projects/${GBW_GCP_PROJECT}/databases/(default)/documents/users/${TEST_UID}/jobs/${JOB_ID}" >/dev/null 2>&1 || true
  fi
  rm -rf "$TMP"
}
trap cleanup EXIT

gcloud config set project "$GBW_GCP_PROJECT" >/dev/null

# Obtain a short-lived Firebase ID token for the already-allowlisted UID. This
# is CI-only bootstrap: it does not enable anonymous auth, add an allowed UID,
# change a password, or alter the production authorization policy.
CUSTOM_TOKEN="$(
  cd cloud/remote-separation/functions
  node - "$TEST_UID" <<'NODE'
const {applicationDefault, initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
(async () => {
  const uid = process.argv[2];
  const app = initializeApp(
    {credential: applicationDefault(), projectId: process.env.GBW_GCP_PROJECT},
    "u4-real-ack",
  );
  process.stdout.write(await getAuth(app).createCustomToken(uid));
})().catch(error => {
  console.error(String(error && error.stack ? error.stack : error));
  process.exit(1);
});
NODE
)"
test -n "$CUSTOM_TOKEN"
FIREBASE_API_KEY="$(
  python3 - <<'PY'
import pathlib,re
text=pathlib.Path("platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt").read_text()
match=re.search(r'\.setApiKey\("([^"]+)"\)', text)
if not match:
    raise SystemExit("Firebase API key configuration not found")
print(match.group(1))
PY
)"
AUTH_JSON="$(
  curl --fail-with-body --show-error --silent     -H "Content-Type: application/json"     -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${FIREBASE_API_KEY}"     --data "{\"token\":\"${CUSTOM_TOKEN}\",\"returnSecureToken\":true}"
)"
AUTH_JSON_PATH="$TMP/firebase-auth-response.json"
printf '%s' "$AUTH_JSON" > "$AUTH_JSON_PATH"
FIREBASE_ID_TOKEN="$(python3 - "$TEST_UID" "$GBW_GCP_PROJECT" "$AUTH_JSON_PATH" <<'PY'
import base64
import json
import sys

expected_uid, project_id, response_path = sys.argv[1:]
with open(response_path, encoding="utf-8") as response_file:
    response = json.load(response_file)
token = response.get("idToken")
if not isinstance(token, str) or not token:
    raise SystemExit("Firebase custom-token exchange returned no ID token")
try:
    encoded_claims = token.split(".")[1]
    encoded_claims += "=" * (-len(encoded_claims) % 4)
    claims = json.loads(base64.urlsafe_b64decode(encoded_claims))
except (IndexError, ValueError, json.JSONDecodeError) as exc:
    raise SystemExit(f"Firebase custom-token exchange returned a malformed ID token: {exc}") from exc
if claims.get("sub") != expected_uid or claims.get("user_id") != expected_uid:
    raise SystemExit("Firebase ID token UID mismatch")
if claims.get("aud") != project_id:
    raise SystemExit("Firebase ID token audience mismatch")
if claims.get("iss") != f"https://securetoken.google.com/{project_id}":
    raise SystemExit("Firebase ID token issuer mismatch")
print(token)
PY
)"
test -n "$FIREBASE_ID_TOKEN"

# Deterministic 2 s stereo WAV. The real production Demucs job processes it.
python3 - "$TMP/source.wav" <<'PY'
import math, struct, sys, wave
p=sys.argv[1]; sr=44100; n=sr*2
with wave.open(p,"wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(sr)
    frames=bytearray()
    for i in range(n):
        a=int(5000*math.sin(2*math.pi*220*i/sr)); b=int(4000*math.sin(2*math.pi*330*i/sr))
        frames += struct.pack("<hh",a,b)
    w.writeframes(frames)
PY
INPUT_SHA="$(sha256sum "$TMP/source.wav" | awk '{print $1}')"

gcloud storage cp "$TMP/source.wav" "gs://${GBW_BUCKET}/${INPUT_PATH}" --content-type="audio/wav" --custom-metadata="sha256=${INPUT_SHA},projectId=${PROJECT_ID},jobId=${JOB_ID}" >/dev/null

# The production worker updates an existing Firestore job document. The real callable
# creates this before dispatch; this isolated smoke mirrors that durable precondition.
ACCESS_TOKEN="$(gcloud auth print-access-token)"
DOC_URL="https://firestore.googleapis.com/v1/projects/${GBW_GCP_PROJECT}/databases/(default)/documents/users/${TEST_UID}/jobs?documentId=${JOB_ID}"
curl --fail-with-body --show-error -X POST "$DOC_URL" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "Content-Type: application/json" \
  -H "X-Goog-User-Project: ${GBW_GCP_PROJECT}" \
  --data "{\"fields\":{\"schemaVersion\":{\"integerValue\":\"2\"},\"resultContract\":{\"stringValue\":\"prepared-reference-v2\"},\"workerContract\":{\"stringValue\":\"official-demucs-pytorch-v1\"},\"uid\":{\"stringValue\":\"${TEST_UID}\"},\"projectId\":{\"stringValue\":\"${PROJECT_ID}\"},\"sourceAssetId\":{\"stringValue\":\"${SOURCE_ASSET_ID}\"},\"inputPath\":{\"stringValue\":\"${INPUT_PATH}\"},\"inputSha256\":{\"stringValue\":\"${INPUT_SHA}\"},\"state\":{\"stringValue\":\"QUEUED\"},\"phase\":{\"stringValue\":\"STARTING\"},\"progress\":{\"integerValue\":\"0\"},\"remoteCleanupState\":{\"stringValue\":\"NONE\"}}}" >/dev/null

echo "Executing ${GBW_JOB_NAME} for GuitarLab cloud smoke..."
EXECUTION="$(gcloud beta run jobs execute "$GBW_JOB_NAME"   --project "$GBW_GCP_PROJECT" --region "$GBW_REGION"   --update-env-vars "GBW_BUCKET=${GBW_BUCKET},GBW_UID=${TEST_UID},GBW_JOB_ID=${JOB_ID},GBW_PROJECT_ID=${PROJECT_ID},GBW_INPUT_PATH=${INPUT_PATH},GBW_INPUT_SHA256=${INPUT_SHA}"   --task-timeout 30m --wait --format='value(metadata.name)')"
test -n "$EXECUTION"

MANIFEST_URI="gs://${GBW_BUCKET}/${PREFIX}/output/result-manifest.json"
gcloud storage cp "$MANIFEST_URI" "$TMP/result-manifest.json" >/dev/null
for artifact in "${DELIVERABLES[@]}"; do
  gcloud storage cp "gs://${GBW_BUCKET}/${PREFIX}/output/prepared/${artifact}.wav" "$TMP/${artifact}.wav" >/dev/null
done

python3 - "$TMP" "$JOB_ID" "$PROJECT_ID" "$INPUT_SHA" "$MODEL_SHA256" "$TEST_UID" "$PREFIX" <<'PY'
import hashlib,json,pathlib,sys
root=pathlib.Path(sys.argv[1]); job,project,input_sha,model_sha,uid,prefix=sys.argv[2:]
raw=(root/"result-manifest.json").read_bytes(); m=json.loads(raw)
assert m["schemaVersion"]==2
assert m["jobId"]==job and m["uid"]==uid and m["projectId"]==project and m["inputSha256"]==input_sha
assert m["engine"]=="demucs-pytorch" and m["model"]=="htdemucs_6s" and m["modelSha256"]==model_sha
assert m["device"]=="cpu" and int(m["shifts"])==1 and abs(float(m["overlap"])-0.5)<1e-12
assert m["demucsVersion"]=="4.1.0" and m["pytorchVersion"]=="2.14.0+cpu"
assert int(m["modelBytes"])==54996327
assert m["sampleRate"]==44100 and m["channels"]==2 and int(m["frames"])>0 and float(m["duration"])>0
assert "stems" not in m
recipe=m["referenceRecipe"]
assert recipe["version"]=="prepared-reference-v2"
assert float(recipe["targetPeakDbfs"])==-1.0 and float(recipe["sharedGainDb"])<=0.0
assert set(recipe["backingStems"])=={"drums","bass","other","vocals","piano"}
assert recipe["guitarStem"]=="guitar"
rows=m["deliverables"]
assert [row["name"] for row in rows]==["backing","guitar"]
assert [row["role"] for row in rows]==["REFERENCE_BACKING","REFERENCE_GUITAR"]
for row in rows:
    assert row["path"]==f"{prefix}/output/prepared/{row['name']}.wav"
    p=root/(row["name"]+".wav")
    data=p.read_bytes()
    assert len(data)==int(row["bytes"]) and len(data)>44
    assert hashlib.sha256(data).hexdigest()==row["sha256"]
    assert data[:4]==b"RIFF" and data[8:12]==b"WAVE"
    assert row["sampleRate"]==44100 and row["channels"]==2 and int(row["frames"])==int(m["frames"])
    assert row["encoding"]=="FLOAT32_LE"
print("U4 prepared-reference v2 manifest/integrity contract PASS")
print("manifest_sha256="+hashlib.sha256(raw).hexdigest())
PY
MANIFEST_SHA="$(sha256sum "$TMP/result-manifest.json" | awk '{print $1}')"

# Worker contract deletes the source only after durable prepared-reference + manifest publication.
if gcloud storage objects describe "gs://${GBW_BUCKET}/${INPUT_PATH}" >/dev/null 2>&1; then
  echo "::error::Production worker did not purge source after successful publication."
  exit 1
fi

# Prove v2 never uploads six intermediate stems and only publishes the two product artifacts.
for stem in drums bass other vocals guitar piano; do
  ! gcloud storage objects describe "gs://${GBW_BUCKET}/${PREFIX}/output/${stem}.wav" >/dev/null 2>&1
done
for artifact in "${DELIVERABLES[@]}"; do
  gcloud storage objects describe "gs://${GBW_BUCKET}/${PREFIX}/output/prepared/${artifact}.wav" >/dev/null
done
gcloud storage objects describe "$MANIFEST_URI" >/dev/null

if [[ "${GBW_RECOVERY_GATE_REQUIRED:-1}" == "1" ]]; then
# Prove authoritative same-generation discovery/adoption before ACK.
FIND_URL="https://${GBW_REGION}-${GBW_GCP_PROJECT}.cloudfunctions.net/findRecoverableRemoteSeparation"
FIND_PAYLOAD="{\"data\":{\"projectId\":\"${PROJECT_ID}\",\"sourceAssetId\":\"${SOURCE_ASSET_ID}\",\"inputSha256\":\"${INPUT_SHA}\"}}"
FIND_RESPONSE="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}"     -H "Content-Type: application/json"     -X POST "$FIND_URL"     --data "$FIND_PAYLOAD"
)"
python3 - "$FIND_RESPONSE" "$JOB_ID" <<'PY'
import json,sys
payload=json.loads(sys.argv[1])
result=payload.get("result") or {}
assert result.get("found") is True, payload
assert result.get("disposition")=="EXISTING_SAME_GENERATION", payload
assert result.get("jobId")==sys.argv[2], payload
assert result.get("state")=="COMPLETED", payload
PY

RECOVERY_INPUT_PATH="${RECOVERY_PREFIX}/input/source.wav"
gcloud storage cp "$TMP/source.wav" "gs://${GBW_BUCKET}/${RECOVERY_INPUT_PATH}" --content-type="audio/wav" --custom-metadata="sha256=${INPUT_SHA},projectId=${PROJECT_ID},jobId=${RECOVERY_JOB_ID}" >/dev/null
STATUS_URL="https://${GBW_REGION}-${GBW_GCP_PROJECT}.cloudfunctions.net/remoteBackendStatus"
STATUS_PAYLOAD='{"data":{}}'
STATUS_BEFORE="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}"     -H "Content-Type: application/json"     -X POST "$STATUS_URL"     --data "$STATUS_PAYLOAD"
)"
EXECUTIONS_BEFORE="$(gcloud run jobs executions list --job "$GBW_JOB_NAME" --region "$GBW_REGION" --project "$GBW_GCP_PROJECT" --format='value(name)' | sort)"

ENQUEUE_URL="https://${GBW_REGION}-${GBW_GCP_PROJECT}.cloudfunctions.net/enqueueRemoteSeparation"
ENQUEUE_PAYLOAD="{\"data\":{\"jobId\":\"${RECOVERY_JOB_ID}\",\"projectId\":\"${PROJECT_ID}\",\"sourceAssetId\":\"${SOURCE_ASSET_ID}\",\"inputSha256\":\"${INPUT_SHA}\",\"inputPath\":\"${RECOVERY_INPUT_PATH}\"}}"
ENQUEUE_RESPONSE="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}"     -H "Content-Type: application/json"     -X POST "$ENQUEUE_URL"     --data "$ENQUEUE_PAYLOAD"
)"
python3 - "$ENQUEUE_RESPONSE" "$JOB_ID" "$RECOVERY_JOB_ID" <<'PY'
import json,sys
payload=json.loads(sys.argv[1])
result=payload.get("result") or {}
assert result.get("requestedJobId")==sys.argv[3], payload
assert result.get("effectiveJobId")==sys.argv[2], payload
assert result.get("disposition")=="EXISTING_SAME_GENERATION", payload
assert result.get("state")=="COMPLETED", payload
PY

! gcloud storage ls "gs://${GBW_BUCKET}/${RECOVERY_PREFIX}/**" --recursive >/dev/null 2>&1
RECOVERY_DOC="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${ACCESS_TOKEN}"     -H "X-Goog-User-Project: ${GBW_GCP_PROJECT}"     "https://firestore.googleapis.com/v1/projects/${GBW_GCP_PROJECT}/databases/(default)/documents/users/${TEST_UID}/jobs/${RECOVERY_JOB_ID}"
)" || true
python3 - "$RECOVERY_DOC" <<'PY'
import json,sys
payload=json.loads(sys.argv[1])
assert "error" in payload and int(payload["error"].get("code",0))==404, payload
PY
STATUS_AFTER="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}"     -H "Content-Type: application/json"     -X POST "$STATUS_URL"     --data "$STATUS_PAYLOAD"
)"
python3 - "$STATUS_BEFORE" "$STATUS_AFTER" <<'PY'
import json,sys
before=(json.loads(sys.argv[1]).get("result") or {})
after=(json.loads(sys.argv[2]).get("result") or {})
assert int(before.get("acceptedJobs",-1)) == int(after.get("acceptedJobs",-2)), (before,after)
PY
EXECUTIONS_AFTER="$(gcloud run jobs executions list --job "$GBW_JOB_NAME" --region "$GBW_REGION" --project "$GBW_GCP_PROJECT" --format='value(name)' | sort)"
test "$EXECUTIONS_BEFORE" = "$EXECUTIONS_AFTER"
echo "U4 same-generation recovery PASS: completed original adopted; quota unchanged; no second Cloud Run execution; retry upload cleaned."
else
  echo "U4 same-generation recovery gate skipped for worker-only shadow stage; production callable contract is unchanged until controlled production deploy."
fi
# Call the production callable with a real Firebase-authenticated allowlisted identity.
ACK_URL="https://${GBW_REGION}-${GBW_GCP_PROJECT}.cloudfunctions.net/acknowledgeRemoteImport"
ACK_PAYLOAD="{\"data\":{\"jobId\":\"${JOB_ID}\",\"projectId\":\"${PROJECT_ID}\",\"resultManifestSha256\":\"${MANIFEST_SHA}\"}}"
ACK_RESPONSE="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}"     -H "Content-Type: application/json"     -X POST "$ACK_URL"     --data "$ACK_PAYLOAD"
)"
python3 - "$ACK_RESPONSE" <<'PY'
import json,sys
payload=json.loads(sys.argv[1])
result=payload.get("result") or {}
assert result.get("state")=="IMPORTED", payload
assert result.get("cleanup")=="PURGED", payload
PY

if gcloud storage ls "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1; then
  echo "::error::acknowledgeRemoteImport left residual job objects in Storage."
  exit 1
fi

FINAL_DOC="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${ACCESS_TOKEN}"     -H "X-Goog-User-Project: ${GBW_GCP_PROJECT}"     "https://firestore.googleapis.com/v1/projects/${GBW_GCP_PROJECT}/databases/(default)/documents/users/${TEST_UID}/jobs/${JOB_ID}"
)"
python3 - "$FINAL_DOC" <<'PY'
import json,sys
fields=json.loads(sys.argv[1])["fields"]
assert fields["state"]["stringValue"]=="IMPORTED"
assert fields["remoteCleanupState"]["stringValue"]=="PURGED"
assert fields.get("remotePurgedAt",{}).get("timestampValue")
PY

ACK_AGAIN="$(
  curl --fail-with-body --show-error --silent     -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}"     -H "Content-Type: application/json"     -X POST "$ACK_URL"     --data "$ACK_PAYLOAD"
)"
python3 - "$ACK_AGAIN" <<'PY'
import json,sys
result=(json.loads(sys.argv[1]).get("result") or {})
assert result.get("state")=="IMPORTED"
assert result.get("cleanup")=="PURGED"
assert result.get("idempotent") is True
PY
! gcloud storage ls "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1

echo "U4 real Cloud Run smoke PASS: execution=${EXECUTION}; prepared-reference v2 contract PASS; no remote stems; source cleanup PASS; real ACK PASS; Firestore IMPORTED/PURGED PASS; whole-prefix purge idempotency PASS."
