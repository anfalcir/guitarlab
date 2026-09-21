#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?}"
: "${GBW_BUCKET:?}"
GBW_REGION="${GBW_REGION:-us-central1}"
MODEL_SHA256="${MODEL_SHA256:?}"
STEMS=(drums bass other vocals guitar piano)
RUN_TOKEN="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
UID="guitarlab-u4-ci"
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
PREFIX="remote/v1/users/${UID}/jobs/${JOB_ID}"
INPUT_PATH="${PREFIX}/input/source.wav"
TMP="$(mktemp -d)"
cleanup() {
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
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

gcloud storage cp "$TMP/source.wav" "gs://${GBW_BUCKET}/${INPUT_PATH}" >/dev/null

echo "Executing production gbw-demucs for GuitarLab U4 smoke..."
EXECUTION="$(gcloud beta run jobs execute gbw-demucs   --project "$GBW_GCP_PROJECT" --region "$GBW_REGION"   --update-env-vars "GBW_BUCKET=${GBW_BUCKET},GBW_UID=${UID},GBW_JOB_ID=${JOB_ID},GBW_PROJECT_ID=${PROJECT_ID},GBW_INPUT_PATH=${INPUT_PATH},GBW_INPUT_SHA256=${INPUT_SHA}"   --task-timeout 30m --wait --format='value(metadata.name)')"
test -n "$EXECUTION"

MANIFEST_URI="gs://${GBW_BUCKET}/${PREFIX}/output/result-manifest.json"
gcloud storage cp "$MANIFEST_URI" "$TMP/result-manifest.json" >/dev/null
for stem in "${STEMS[@]}"; do
  gcloud storage cp "gs://${GBW_BUCKET}/${PREFIX}/output/${stem}.wav" "$TMP/${stem}.wav" >/dev/null
done

python3 - "$TMP" "$JOB_ID" "$PROJECT_ID" "$INPUT_SHA" "$MODEL_SHA256" <<'PY'
import hashlib,json,pathlib,struct,sys
root=pathlib.Path(sys.argv[1]); job,project,input_sha,model_sha=sys.argv[2:]
raw=(root/"result-manifest.json").read_bytes(); m=json.loads(raw)
assert m["schemaVersion"]==1
assert m["jobId"]==job and m["projectId"]==project and m["inputSha256"]==input_sha
assert m["engine"]=="demucs.cpp" and m["model"]=="htdemucs_6s" and m["modelSha256"]==model_sha
assert m["sampleRate"]==44100 and m["channels"]==2 and int(m["frames"])>0 and float(m["duration"])>0
expected=["drums","bass","other","vocals","guitar","piano"]
assert [s["name"] for s in m["stems"]]==expected and len(m["stems"])==6
for row in m["stems"]:
    p=root/(row["name"]+".wav")
    data=p.read_bytes()
    assert len(data)==int(row["bytes"]) and len(data)>44
    assert hashlib.sha256(data).hexdigest()==row["sha256"]
    assert data[:4]==b"RIFF" and data[8:12]==b"WAVE"
print("U4 six-stem manifest/integrity contract PASS")
print("manifest_sha256="+hashlib.sha256(raw).hexdigest())
PY

# Worker contract deletes the source only after durable six-stem + manifest publication.
if gcloud storage objects describe "gs://${GBW_BUCKET}/${INPUT_PATH}" >/dev/null 2>&1; then
  echo "::error::Production worker did not purge source after successful publication."
  exit 1
fi

# Simulate the post-local-publication ACK/purge boundary at the cloud plane:
# verify every output exists first, then purge once and prove idempotent absence.
for stem in "${STEMS[@]}"; do
  gcloud storage objects describe "gs://${GBW_BUCKET}/${PREFIX}/output/${stem}.wav" >/dev/null
done
gcloud storage objects describe "$MANIFEST_URI" >/dev/null
gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/output/**" --recursive >/dev/null
for stem in "${STEMS[@]}"; do
  ! gcloud storage objects describe "gs://${GBW_BUCKET}/${PREFIX}/output/${stem}.wav" >/dev/null 2>&1
done
! gcloud storage objects describe "$MANIFEST_URI" >/dev/null 2>&1
gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/output/**" --recursive >/dev/null 2>&1 || true

echo "U4 real Cloud Run smoke PASS: execution=${EXECUTION}; six validated stems; source cleanup PASS; post-validation purge idempotency PASS."
