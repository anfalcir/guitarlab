#!/usr/bin/env bash
set -euo pipefail

: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
: "${GBW_PINNED_IMAGE:?Set GBW_PINNED_IMAGE to the exact digest-pinned shadow image}"

GBW_REGION="${GBW_REGION:-us-central1}"
VARIANT="${GBW_W4_VARIANT:-cpu8-baseline}"
STRATEGY="${GBW_W4_STRATEGY:-cpu8_s1_o05}"
VCPU="${GBW_W4_VCPU:-8}"
case "$STRATEGY" in
  cpu8_s1_o05) EXPECTED_ENGINE_STRATEGY="pytorch-cpu-s1-o0.5-t8" ;;
  cpu4_s1_o05) EXPECTED_ENGINE_STRATEGY="pytorch-cpu-s1-o0.5-t4" ;;
  *)
    echo "Unsupported W4 matrix strategy: $STRATEGY" >&2
    exit 2
    ;;
esac
MEMORY_GIB="${GBW_W4_MEMORY_GIB:-16}"
MEMORY_RESOURCE="${GBW_W4_MEMORY_RESOURCE:-16Gi}"
WARM_PROBE="${GBW_W4_WARM_PROBE:-1}"
MODEL_PROBE="${GBW_W4_MODEL_PROBE:-1}"
RUN_BASE="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}-${VARIANT}"
RUN_TOKEN="$(printf '%s' "$RUN_BASE" | tr '[:upper:]_' '[:lower:]-')"
BENCH_JOB="$(printf 'guitarlab-w4-%s-%s' "${GITHUB_RUN_ID:-local}" "$VARIANT" | tr '[:upper:]_' '[:lower:]-' | cut -c1-63)"
PROBE_JOB="$(printf 'guitarlab-w4p-%s-%s' "${GITHUB_RUN_ID:-local}" "$VARIANT" | tr '[:upper:]_' '[:lower:]-' | cut -c1-63)"
PREFIX="diagnostics/u12-rc20/w4/${RUN_TOKEN}"
INPUT_PATH="${PREFIX}/input.wav"
BUNDLE_PREFIX="${PREFIX}/bundle"
PROBE_PATH="${PREFIX}/model-probe.json"
ARTIFACT_DIR="artifacts/u12-rc20-w4/${RUN_TOKEN}"
WORKER_SA="gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"

VCPU_PRICE="${GBW_W4_VCPU_PRICE_PER_SECOND:-0.000018}"
MEMORY_PRICE="${GBW_W4_MEMORY_PRICE_PER_GIB_SECOND:-0.000002}"
PRICING_BASIS="${GBW_W4_PRICING_BASIS:-Cloud Run Jobs Tier-1 on-demand rates checked 2026-09-24}"

mkdir -p "$ARTIFACT_DIR"
TMP="$(mktemp -d)"
cleanup() {
  gcloud run jobs delete "$BENCH_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --quiet >/dev/null 2>&1 || true
  gcloud run jobs delete "$PROBE_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --quiet >/dev/null 2>&1 || true
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
  rm -rf "$TMP"
}
trap cleanup EXIT

python3 scripts/u12_generate_benchmark_fixture.py "$TMP/w4-fixture.wav" --seconds 180
FIXTURE_SHA="$(sha256sum "$TMP/w4-fixture.wav" | awk '{print $1}')"
gcloud storage cp "$TMP/w4-fixture.wav" "gs://${GBW_BUCKET}/${INPUT_PATH}" --content-type=audio/wav >/dev/null

PROBE_WALL_SECONDS=""
if [[ "$MODEL_PROBE" == "1" ]]; then
  gcloud run jobs deploy "$PROBE_JOB" \
    --project "$GBW_GCP_PROJECT" \
    --region "$GBW_REGION" \
    --image "$GBW_PINNED_IMAGE" \
    --service-account "$WORKER_SA" \
    --tasks 1 \
    --parallelism 1 \
    --cpu "$VCPU" \
    --memory "$MEMORY_RESOURCE" \
    --task-timeout 15m \
    --max-retries 0 \
    --command /opt/venv/bin/python3 \
    --args /app/model_probe.py \
    --set-env-vars "GBW_PROBE_PROJECT=${GBW_GCP_PROJECT},GBW_PROBE_BUCKET=${GBW_BUCKET},GBW_PROBE_OUTPUT_PATH=${PROBE_PATH},GBW_MODEL_REPO=/opt/demucs/models" \
    --clear-volumes \
    --clear-volume-mounts \
    --quiet
  probe_started="$(date +%s)"
  probe_execution="$(gcloud run jobs execute "$PROBE_JOB" \
    --project "$GBW_GCP_PROJECT" \
    --region "$GBW_REGION" \
    --task-timeout 15m \
    --wait \
    --format='value(metadata.name)')"
  probe_ended="$(date +%s)"
  test -n "$probe_execution"
  PROBE_WALL_SECONDS="$((probe_ended-probe_started))"
  gcloud storage cp "gs://${GBW_BUCKET}/${PROBE_PATH}" "$ARTIFACT_DIR/model-probe.json" >/dev/null
  python3 - "$ARTIFACT_DIR/model-probe.json" <<'PY'
import json, sys
data=json.load(open(sys.argv[1], encoding="utf-8"))
assert data["model"]=="htdemucs_6s"
assert data["sources"]==["drums","bass","other","vocals","guitar","piano"]
assert int(data["importMs"])>=0 and int(data["modelLoadMs"])>=0
print(json.dumps(data, sort_keys=True))
PY
fi

gcloud run jobs deploy "$BENCH_JOB" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --image "$GBW_PINNED_IMAGE" \
  --service-account "$WORKER_SA" \
  --tasks 1 \
  --parallelism 1 \
  --cpu "$VCPU" \
  --memory "$MEMORY_RESOURCE" \
  --task-timeout 30m \
  --max-retries 0 \
  --command /opt/venv/bin/python3 \
  --args /app/benchmark.py \
  --set-env-vars "GBW_BENCH_PROJECT=${GBW_GCP_PROJECT},GBW_BENCH_BUCKET=${GBW_BUCKET},GBW_BENCH_INPUT_PATH=${INPUT_PATH},GBW_BENCH_STRATEGY=${STRATEGY},GBW_BENCH_MEMORY_GIB=${MEMORY_GIB},GBW_BENCH_VCPU_PRICE_PER_SECOND=${VCPU_PRICE},GBW_BENCH_MEMORY_PRICE_PER_GIB_SECOND=${MEMORY_PRICE},GBW_BENCH_BUNDLE_PREFIX=${BUNDLE_PREFIX},GBW_BENCH_IMAGE_IDENTITY=${GBW_PINNED_IMAGE},GBW_BENCH_PRICING_BASIS=${PRICING_BASIS},GBW_BENCH_WARM_PROBE=${WARM_PROBE},GBW_MODEL_REPO=/opt/demucs/models" \
  --clear-volumes \
  --clear-volume-mounts \
  --quiet

ACTUAL_IMAGE="$(gcloud run jobs describe "$BENCH_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --format='value(spec.template.spec.template.spec.containers[0].image)')"
test "$ACTUAL_IMAGE" = "$GBW_PINNED_IMAGE"

STARTED="$(date +%s)"
EXECUTION="$(gcloud run jobs execute "$BENCH_JOB" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --task-timeout 30m \
  --wait \
  --format='value(metadata.name)')"
ENDED="$(date +%s)"
test -n "$EXECUTION"

mkdir -p "$ARTIFACT_DIR/stems" "$ARTIFACT_DIR/prepared"
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/SHA256SUMS" "$ARTIFACT_DIR/SHA256SUMS" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/benchmark.json" "$ARTIFACT_DIR/benchmark.json" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/recombined.wav" "$ARTIFACT_DIR/recombined.wav" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/stems/*.wav" "$ARTIFACT_DIR/stems/" >/dev/null
gcloud storage cp "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/prepared/*.wav" "$ARTIFACT_DIR/prepared/" >/dev/null
(
  cd "$ARTIFACT_DIR"
  sha256sum -c SHA256SUMS
)

python3 - "$ARTIFACT_DIR/benchmark.json" "$GBW_PINNED_IMAGE" "$EXPECTED_ENGINE_STRATEGY" "$WARM_PROBE" <<'PY'
import json, sys
path, image, expected_engine_strategy, warm_required = sys.argv[1:]
data=json.load(open(path, encoding="utf-8"))
assert data["engine"]=="demucs-pytorch"
assert data["model"]=="htdemucs_6s"
assert data["modelSha256"]=="34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd"
assert data["strategy"]==expected_engine_strategy
assert data["device"]=="cpu" and data["shifts"]==1 and abs(float(data["overlap"])-0.5)<1e-12
assert data.get("imageIdentity")==image
assert not [row for row in data["qualityFindings"] if row.get("severity")=="reject"]
assert set(data["stemMetrics"])=={"drums","bass","other","vocals","guitar","piano"}
assert set(data["preparedSha256"])=={"backing","guitar"}
assert int(data["totalMs"])<=900_000, f"W4 runtime exceeds 15-minute gate: {data['totalMs']} ms"
if warm_required=="1":
    warm=data.get("warmProbe")
    assert warm is not None
    assert not [row for row in warm["qualityFindings"] if row.get("severity")=="reject"]
print(json.dumps({
    "totalMs":data["totalMs"],
    "wallMsIncludingWarmProbe":data["wallMsIncludingWarmProbe"],
    "inferenceMs":data["inferenceMs"],
    "warmInferenceMs":None if data.get("warmProbe") is None else data["warmProbe"]["inferenceMs"],
    "renderMs":data["renderMs"],
    "maxRssKiB":data["maxRssKiB"],
    "estimatedCostUsd":data["estimatedCostUsd"],
    "otherEnergyShare":data["qualitySummary"]["energyShareByStem"]["other"],
    "qualityFindings":data["qualityFindings"],
}, sort_keys=True))
PY

cat > "$ARTIFACT_DIR/W4_IDENTITY.txt" <<EOF
source_sha=${GITHUB_SHA:-local}
image=${GBW_PINNED_IMAGE}
execution=${EXECUTION}
fixture_sha256=${FIXTURE_SHA}
fixture_duration_seconds=180
variant=${VARIANT}
strategy=${STRATEGY}
engine_strategy=${EXPECTED_ENGINE_STRATEGY}
vcpu=${VCPU}
memory_gib=${MEMORY_GIB}
cloud_wall_seconds=$((ENDED-STARTED))
model_probe_wall_seconds=${PROBE_WALL_SECONDS}
warm_probe=${WARM_PROBE}
pricing_basis=${PRICING_BASIS}
owner_listening=REQUIRED_NOT_YET_PASSED
EOF

printf 'w4_shadow_benchmark=PASS variant=%s execution=%s artifact_dir=%s\n' "$VARIANT" "$EXECUTION" "$ARTIFACT_DIR"
