#!/usr/bin/env bash
set -euo pipefail

: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
: "${GBW_PINNED_IMAGE:?Set GBW_PINNED_IMAGE}"
GBW_REGION="${GBW_REGION:-us-central1}"
RUN_TOKEN="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
PREFIX="diagnostics/u12-rc20/w4/${RUN_TOKEN}/parity"
INPUT_PATH="${PREFIX}/input.wav"
CLOUD_BUNDLE_PREFIX="${PREFIX}/cloud"
PARITY_JOB="${GBW_PARITY_JOB_NAME:-guitarlab-demucs-w3-${GITHUB_RUN_ID:-local}}"
PARITY_JOB="$(printf '%s' "$PARITY_JOB" | tr '[:upper:]_' '[:lower:]-' | cut -c1-63)"
WORKER_SA="gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"
ARTIFACT_DIR="artifacts/u12-rc20-w3/${RUN_TOKEN}"

TMP="$(mktemp -d)"
mkdir -p "$ARTIFACT_DIR"
chmod 0777 "$TMP"
cleanup() {
  gcloud run jobs delete "$PARITY_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --quiet >/dev/null 2>&1 || true
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
  sudo rm -rf "$TMP"
}
trap cleanup EXIT

python3 scripts/u12_generate_benchmark_fixture.py "$TMP/input.wav" --seconds 12
gcloud storage cp "$TMP/input.wav" "gs://${GBW_BUCKET}/${INPUT_PATH}" --content-type=audio/wav >/dev/null

docker pull "$GBW_PINNED_IMAGE" >/dev/null
mkdir -p "$TMP/local"
chmod 0777 "$TMP/local"
docker run --rm \
  --user 65532:65532 \
  --volume "$TMP:/work" \
  --volume "$PWD/scripts/u12_rc20_local_parity.py:/parity.py:ro" \
  --entrypoint python3 \
  "$GBW_PINNED_IMAGE" \
  /parity.py /work/input.wav /work/local --image-identity "$GBW_PINNED_IMAGE"

gcloud run jobs deploy "$PARITY_JOB" \
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
  --command python3 \
  --args /app/benchmark.py \
  --set-env-vars "GBW_BENCH_PROJECT=${GBW_GCP_PROJECT},GBW_BENCH_BUCKET=${GBW_BUCKET},GBW_BENCH_INPUT_PATH=${INPUT_PATH},GBW_BENCH_STRATEGY=cpu8_s1_o05,GBW_BENCH_MEMORY_GIB=16,GBW_BENCH_BUNDLE_PREFIX=${CLOUD_BUNDLE_PREFIX},GBW_BENCH_IMAGE_IDENTITY=${GBW_PINNED_IMAGE},GBW_MODEL_REPO=/opt/demucs/models" \
  --clear-volumes \
  --clear-volume-mounts \
  --quiet

ACTUAL_IMAGE="$(gcloud run jobs describe "$PARITY_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --format='value(spec.template.spec.template.spec.containers[0].image)')"
test "$ACTUAL_IMAGE" = "$GBW_PINNED_IMAGE"
EXECUTION="$(gcloud run jobs execute "$PARITY_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --task-timeout 30m --wait --format='value(metadata.name)')"
test -n "$EXECUTION"

gcloud storage cp "gs://${GBW_BUCKET}/${CLOUD_BUNDLE_PREFIX}/benchmark.json" "$TMP/cloud.json" >/dev/null
cp "$TMP/local/parity.json" "$ARTIFACT_DIR/local.json"
cp "$TMP/cloud.json" "$ARTIFACT_DIR/cloud.json"

python3 - "$ARTIFACT_DIR/local.json" "$ARTIFACT_DIR/cloud.json" "$ARTIFACT_DIR/parity.json" <<'PY'
import json
import math
import sys

local_path, cloud_path, out_path = sys.argv[1:]
local = json.load(open(local_path, encoding="utf-8"))
cloud = json.load(open(cloud_path, encoding="utf-8"))

for key in (
    "engine", "engineRevision", "demucsVersion", "pytorchVersion", "model",
    "modelSha256", "modelBytes", "strategy", "device", "shifts", "overlap",
    "cpuThreads", "imageIdentity",
):
    assert local[key] == cloud[key], (key, local[key], cloud[key])
assert local["sourceContract"] == cloud["sourceContract"]
assert not [x for x in local["qualityFindings"] if x.get("severity") == "reject"]
assert not [x for x in cloud["qualityFindings"] if x.get("severity") == "reject"]

REL_TOL = 0.01
ABS_TOL = 5e-6
GAIN_ABS_TOL_DB = 0.05
ENERGY_SHARE_ABS_TOL = 0.01
max_relative_delta = 0.0
comparisons = 0

def compare_metrics(label, a, b):
    global max_relative_delta, comparisons
    for key in ("frames", "finiteSamples", "nonFiniteSamples"):
        assert a[key] == b[key], (label, key, a[key], b[key])
    for key in ("peakPerChannel", "rmsPerChannel", "meanDcPerChannel"):
        for av, bv in zip(a[key], b[key]):
            if av is None or bv is None:
                assert av is bv
                continue
            assert math.isclose(float(av), float(bv), rel_tol=REL_TOL, abs_tol=ABS_TOL), (label, key, av, bv)
            denom = max(abs(float(av)), abs(float(bv)), ABS_TOL)
            max_relative_delta = max(max_relative_delta, abs(float(av) - float(bv)) / denom)
            comparisons += 1
    av = float(a["energy"])
    bv = float(b["energy"])
    assert math.isclose(av, bv, rel_tol=REL_TOL, abs_tol=ABS_TOL), (label, "energy", av, bv)
    denom = max(abs(av), abs(bv), ABS_TOL)
    max_relative_delta = max(max_relative_delta, abs(av - bv) / denom)
    comparisons += 1

for stem in ("drums", "bass", "other", "vocals", "guitar", "piano"):
    compare_metrics(f"stem:{stem}", local["stemMetrics"][stem], cloud["stemMetrics"][stem])

for stage in ("backingRaw", "guitarRaw", "fullRaw", "backingFinal", "guitarFinal", "recombinedFinal"):
    compare_metrics(f"prepared:{stage}", local["preparedMetrics"][stage], cloud["preparedMetrics"][stage])

assert math.isclose(
    float(local["sharedGainDb"]),
    float(cloud["sharedGainDb"]),
    rel_tol=0.0,
    abs_tol=GAIN_ABS_TOL_DB,
), ("sharedGainDb", local["sharedGainDb"], cloud["sharedGainDb"])

for stem in ("drums", "bass", "other", "vocals", "guitar", "piano"):
    a=float(local["qualitySummary"]["energyShareByStem"][stem])
    b=float(cloud["qualitySummary"]["energyShareByStem"][stem])
    assert abs(a-b) <= ENERGY_SHARE_ABS_TOL, ("energyShare", stem, a, b)

result = {
    "status": "PASS",
    "metricRelativeTolerance": REL_TOL,
    "metricAbsoluteTolerance": ABS_TOL,
    "sharedGainAbsoluteToleranceDb": GAIN_ABS_TOL_DB,
    "energyShareAbsoluteTolerance": ENERGY_SHARE_ABS_TOL,
    "metricComparisons": comparisons,
    "maxRelativeDelta": max_relative_delta,
    "stemHashEquality": {
        stem: local["stemSha256"][stem] == cloud["stemSha256"][stem]
        for stem in ("drums", "bass", "other", "vocals", "guitar", "piano")
    },
    "preparedHashEquality": {
        name: local["preparedSha256"][name] == cloud["preparedSha256"][name]
        for name in ("backing", "guitar")
    },
    "localInferenceMs": local["inferenceMs"],
    "cloudInferenceMs": cloud["inferenceMs"],
}
with open(out_path, "w", encoding="utf-8") as handle:
    json.dump(result, handle, sort_keys=True, indent=2)
    handle.write("\n")
print(json.dumps(result, sort_keys=True))
PY

printf 'w3_parity=PASS execution=%s artifact_dir=%s\n' "$EXECUTION" "$ARTIFACT_DIR"
