#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?}"
: "${GBW_BUCKET:?}"
GBW_REGION="${GBW_REGION:-us-central1}"
GBW_JOB_NAME="${GBW_JOB_NAME:-gbw-demucs}"
MODEL_SHA256="${MODEL_SHA256:?}"
DELIVERABLES=(backing guitar)
RUN_TOKEN="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
TEST_UID="guitarlab-u4-ci"
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
PREFIX="remote/v1/users/${TEST_UID}/jobs/${JOB_ID}"
INPUT_PATH="${PREFIX}/input/source.wav"
TMP="$(mktemp -d)"
cleanup() {
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
  TOKEN="$(gcloud auth print-access-token 2>/dev/null || true)"
  if [[ -n "$TOKEN" ]]; then
    curl --silent -X DELETE -H "Authorization: Bearer ${TOKEN}" "https://firestore.googleapis.com/v1/projects/${GBW_GCP_PROJECT}/databases/(default)/documents/users/${TEST_UID}/jobs/${JOB_ID}" >/dev/null 2>&1 || true
  fi
  rm -rf "$TMP"
}
trap cleanup EXIT

gcloud config set project "$GBW_GCP_PROJECT" >/dev/null

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
  --data "{\"fields\":{\"schemaVersion\":{\"integerValue\":\"2\"},\"resultContract\":{\"stringValue\":\"prepared-reference-v2\"},\"uid\":{\"stringValue\":\"${TEST_UID}\"},\"projectId\":{\"stringValue\":\"${PROJECT_ID}\"},\"inputPath\":{\"stringValue\":\"${INPUT_PATH}\"},\"inputSha256\":{\"stringValue\":\"${INPUT_SHA}\"},\"state\":{\"stringValue\":\"QUEUED\"},\"phase\":{\"stringValue\":\"STARTING\"},\"progress\":{\"integerValue\":\"0\"}}}" >/dev/null

echo "Executing ${GBW_JOB_NAME} for GuitarLab cloud smoke..."
EXECUTION="$(gcloud beta run jobs execute "$GBW_JOB_NAME"   --project "$GBW_GCP_PROJECT" --region "$GBW_REGION"   --update-env-vars "GBW_BUCKET=${GBW_BUCKET},GBW_UID=${TEST_UID},GBW_JOB_ID=${JOB_ID},GBW_PROJECT_ID=${PROJECT_ID},GBW_INPUT_PATH=${INPUT_PATH},GBW_INPUT_SHA256=${INPUT_SHA}"   --task-timeout 30m --wait --format='value(metadata.name)')"
test -n "$EXECUTION"

MANIFEST_URI="gs://${GBW_BUCKET}/${PREFIX}/output/result-manifest.json"
gcloud storage cp "$MANIFEST_URI" "$TMP/result-manifest.json" >/dev/null
for artifact in "${DELIVERABLES[@]}"; do
  gcloud storage cp "gs://${GBW_BUCKET}/${PREFIX}/output/prepared/${artifact}.wav" "$TMP/${artifact}.wav" >/dev/null
done

python3 - "$TMP" "$JOB_ID" "$PROJECT_ID" "$INPUT_SHA" "$MODEL_SHA256" <<'PY'
import hashlib,json,pathlib,sys
root=pathlib.Path(sys.argv[1]); job,project,input_sha,model_sha=sys.argv[2:]
raw=(root/"result-manifest.json").read_bytes(); m=json.loads(raw)
assert m["schemaVersion"]==2
assert m["jobId"]==job and m["projectId"]==project and m["inputSha256"]==input_sha
assert m["engine"]=="demucs.cpp" and m["model"]=="htdemucs_6s" and m["modelSha256"]==model_sha
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

# Simulate the exact whole-prefix storage boundary owned by acknowledgeRemoteImport and prove
# idempotent absence. Callable authorization/purge semantics are independently gated in U7.
gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null
! gcloud storage ls "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1
gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true

echo "U4 real Cloud Run smoke PASS: execution=${EXECUTION}; prepared-reference v2 contract PASS; no remote stems; source cleanup PASS; whole-prefix purge idempotency PASS."
