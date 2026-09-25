#!/usr/bin/env bash
set -euo pipefail

: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
: "${GBW_PINNED_IMAGE:?Set GBW_PINNED_IMAGE}"

SOURCE_FILE="${GBW_W5_SOURCE_FILE:-temporario/mmf-misery-85cdef6e6bd5671e320fc503db582f23eecd1a75097c5cc2b5994f8e693d9ec2.m4a}"
SOURCE_SHA256="${GBW_W5_SOURCE_SHA256:-85cdef6e6bd5671e320fc503db582f23eecd1a75097c5cc2b5994f8e693d9ec2}"
EXPECTED_IMAGE="${GBW_W5_EXPECTED_IMAGE:-us-central1-docker.pkg.dev/gbwapp-ef048/gbw/remote-worker@sha256:6a6d5017e0e2c9bf3800bca3a3c3988ed9398add4ef2c3097241a7a260893dbe}"
EXPECTED_ENGINE_STRATEGY="pytorch-cpu-s1-o0.5-t8"
GBW_REGION="${GBW_REGION:-us-central1}"
WORKER_SA="gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"

test -f "$SOURCE_FILE"
test "$GBW_PINNED_IMAGE" = "$EXPECTED_IMAGE"
command -v ffmpeg >/dev/null
command -v ffprobe >/dev/null

actual_source_sha="$(sha256sum "$SOURCE_FILE" | awk '{print $1}')"
test "$actual_source_sha" = "$SOURCE_SHA256"

RUN_TOKEN="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
PREFIX="diagnostics/u12-rc20/w5/${RUN_TOKEN}"
INPUT_PATH="${PREFIX}/input.wav"
BUNDLE_PREFIX="${PREFIX}/bundle"
ARTIFACT_DIR="artifacts/u12-rc20-w5/${RUN_TOKEN}"
JOB_NAME="$(printf 'guitarlab-w5-%s' "${GITHUB_RUN_ID:-local}" | tr '[:upper:]_' '[:lower:]-' | cut -c1-63)"
TMP="$(mktemp -d)"

mkdir -p "$ARTIFACT_DIR/stems" "$ARTIFACT_DIR/prepared"

cleanup() {
  gcloud run jobs delete "$JOB_NAME" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --quiet >/dev/null 2>&1 || true
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
  rm -rf "$TMP"
}
trap cleanup EXIT

ffmpeg -hide_banner -loglevel error -y \
  -i "$SOURCE_FILE" \
  -vn -ac 2 -ar 44100 -c:a pcm_f32le \
  "$TMP/input.wav"

canonical_sha="$(sha256sum "$TMP/input.wav" | awk '{print $1}')"
duration_seconds="$(ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$TMP/input.wav")"
gcloud storage cp "$TMP/input.wav" "gs://${GBW_BUCKET}/${INPUT_PATH}" --content-type=audio/wav >/dev/null

gcloud run jobs deploy "$JOB_NAME" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --image "$GBW_PINNED_IMAGE" \
  --service-account "$WORKER_SA" \
  --tasks 1 \
  --parallelism 1 \
  --cpu 8 \
  --memory 16Gi \
  --task-timeout 30m \
  --max-retries 0 \
  --command /opt/venv/bin/python3 \
  --args /app/benchmark.py \
  --set-env-vars "GBW_BENCH_PROJECT=${GBW_GCP_PROJECT},GBW_BENCH_BUCKET=${GBW_BUCKET},GBW_BENCH_INPUT_PATH=${INPUT_PATH},GBW_BENCH_STRATEGY=cpu8_s1_o05,GBW_BENCH_MEMORY_GIB=16,GBW_BENCH_VCPU_PRICE_PER_SECOND=0.000018,GBW_BENCH_MEMORY_PRICE_PER_GIB_SECOND=0.000002,GBW_BENCH_BUNDLE_PREFIX=${BUNDLE_PREFIX},GBW_BENCH_IMAGE_IDENTITY=${GBW_PINNED_IMAGE},GBW_BENCH_PRICING_BASIS=W5-owner-listening,GBW_BENCH_WARM_PROBE=0,GBW_MODEL_REPO=/opt/demucs/models" \
  --clear-volumes \
  --clear-volume-mounts \
  --quiet

actual_image="$(gcloud run jobs describe "$JOB_NAME" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --format='value(spec.template.spec.template.spec.containers[0].image)')"
test "$actual_image" = "$GBW_PINNED_IMAGE"

started="$(date +%s)"
execution="$(gcloud run jobs execute "$JOB_NAME" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --task-timeout 30m \
  --wait \
  --format='value(metadata.name)')"
ended="$(date +%s)"
test -n "$execution"

gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/SHA256SUMS" "$ARTIFACT_DIR/SHA256SUMS" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/benchmark.json" "$ARTIFACT_DIR/benchmark.json" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/recombined.wav" "$ARTIFACT_DIR/recombined.wav" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/stems/*.wav" "$ARTIFACT_DIR/stems/" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/prepared/*.wav" "$ARTIFACT_DIR/prepared/" >/dev/null

(
  cd "$ARTIFACT_DIR"
  sha256sum -c SHA256SUMS
)

python3 - "$ARTIFACT_DIR/benchmark.json" "$GBW_PINNED_IMAGE" "$EXPECTED_ENGINE_STRATEGY" <<'PY'
import json, sys
path, image, expected_strategy = sys.argv[1:]
data=json.load(open(path, encoding="utf-8"))
assert data["engine"]=="demucs-pytorch"
assert data["model"]=="htdemucs_6s"
assert data["modelSha256"]=="34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd"
assert data["strategy"]==expected_strategy
assert data["device"]=="cpu"
assert data["shifts"]==1
assert abs(float(data["overlap"])-0.5)<1e-12
assert data.get("imageIdentity")==image
assert set(data["stemMetrics"])=={"drums","bass","other","vocals","guitar","piano"}
assert set(data["preparedSha256"])=={"backing","guitar"}
rejects=[row for row in data["qualityFindings"] if row.get("severity")=="reject"]
assert not rejects, rejects
assert int(data["totalMs"]) <= 900_000, data["totalMs"]
print(json.dumps({
    "status":"PASS",
    "totalMs":data["totalMs"],
    "inferenceMs":data["inferenceMs"],
    "renderMs":data["renderMs"],
    "maxRssKiB":data["maxRssKiB"],
    "estimatedCostUsd":data["estimatedCostUsd"],
    "qualityFindings":data["qualityFindings"],
    "energyShareByStem":data["qualitySummary"]["energyShareByStem"],
}, sort_keys=True))
PY

cat > "$ARTIFACT_DIR/W5_IDENTITY.txt" <<EOF
source_file=$(basename "$SOURCE_FILE")
source_sha256=$actual_source_sha
canonical_input_sha256=$canonical_sha
canonical_duration_seconds=$duration_seconds
producer_sha=${GITHUB_SHA:-local}
image=$GBW_PINNED_IMAGE
execution=$execution
engine=demucs-pytorch
model=htdemucs_6s
model_sha256=34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd
strategy=$EXPECTED_ENGINE_STRATEGY
vcpu=8
memory_gib=16
cloud_wall_seconds=$((ended-started))
owner_listening=REQUIRED_NOT_YET_PASSED
EOF

cat > "$ARTIFACT_DIR/README-W5.txt" <<'EOF'
RC20 W5 owner listening bundle

Listen in this order:
1. prepared/backing.wav
2. prepared/guitar.wav
3. stems/drums.wav
4. stems/bass.wav
5. stems/other.wav
6. stems/vocals.wav
7. stems/guitar.wav
8. stems/piano.wav
9. recombined.wav

PASS requires musically usable separation and plausible stem distribution.
Programmatic PASS alone does not authorize production cutover.
EOF

printf 'w5_shadow_listening_bundle=PASS execution=%s artifact_dir=%s\n' "$execution" "$ARTIFACT_DIR"
