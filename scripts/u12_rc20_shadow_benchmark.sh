#!/usr/bin/env bash
set -euo pipefail

: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
: "${GBW_PINNED_IMAGE:?Set GBW_PINNED_IMAGE to the exact digest-pinned shadow image}"
GBW_REGION="${GBW_REGION:-us-central1}"
RUN_TOKEN="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
BENCH_JOB="${GBW_BENCH_JOB_NAME:-guitarlab-demucs-w4-${GITHUB_RUN_ID:-local}}"
BENCH_JOB="$(printf '%s' "$BENCH_JOB" | tr '[:upper:]_' '[:lower:]-' | cut -c1-63)"
PREFIX="diagnostics/u12-rc20/w4/${RUN_TOKEN}"
INPUT_PATH="${PREFIX}/input.wav"
BUNDLE_PREFIX="${PREFIX}/bundle"
ARTIFACT_DIR="artifacts/u12-rc20-w4/${RUN_TOKEN}"
WORKER_SA="gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"

# Cloud Run Jobs default on-demand Tier-1 rates checked against the public
# pricing table on 2026-09-24. Override these environment variables if pricing changes.
VCPU_PRICE="${GBW_W4_VCPU_PRICE_PER_SECOND:-0.000018}"
MEMORY_PRICE="${GBW_W4_MEMORY_PRICE_PER_GIB_SECOND:-0.000002}"
PRICING_BASIS="${GBW_W4_PRICING_BASIS:-Cloud Run Jobs Tier-1 on-demand rates checked 2026-09-24}"

mkdir -p "$ARTIFACT_DIR"
TMP="$(mktemp -d)"
cleanup() {
  gcloud run jobs delete "$BENCH_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --quiet >/dev/null 2>&1 || true
  gcloud storage rm "gs://${GBW_BUCKET}/${PREFIX}/**" --recursive >/dev/null 2>&1 || true
  rm -rf "$TMP"
}
trap cleanup EXIT

python3 scripts/u12_generate_benchmark_fixture.py "$TMP/w4-fixture.wav" --seconds 180
FIXTURE_SHA="$(sha256sum "$TMP/w4-fixture.wav" | awk '{print $1}')"
gcloud storage cp "$TMP/w4-fixture.wav" "gs://${GBW_BUCKET}/${INPUT_PATH}" --content-type=audio/wav >/dev/null

gcloud run jobs deploy "$BENCH_JOB"   --project "$GBW_GCP_PROJECT"   --region "$GBW_REGION"   --image "$GBW_PINNED_IMAGE"   --service-account "$WORKER_SA"   --tasks 1   --parallelism 1   --cpu 8   --memory 16Gi   --task-timeout 30m   --max-retries 0   --command python3   --args /app/benchmark.py   --set-env-vars "GBW_BENCH_PROJECT=${GBW_GCP_PROJECT},GBW_BENCH_BUCKET=${GBW_BUCKET},GBW_BENCH_INPUT_PATH=${INPUT_PATH},GBW_BENCH_STRATEGY=cpu8_s1_o05,GBW_BENCH_MEMORY_GIB=16,GBW_BENCH_VCPU_PRICE_PER_SECOND=${VCPU_PRICE},GBW_BENCH_MEMORY_PRICE_PER_GIB_SECOND=${MEMORY_PRICE},GBW_BENCH_BUNDLE_PREFIX=${BUNDLE_PREFIX},GBW_BENCH_IMAGE_IDENTITY=${GBW_PINNED_IMAGE},GBW_BENCH_PRICING_BASIS=${PRICING_BASIS},GBW_MODEL_REPO=/opt/demucs/models"   --clear-volumes   --clear-volume-mounts   --quiet

ACTUAL_IMAGE="$(gcloud run jobs describe "$BENCH_JOB" --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --format='value(spec.template.spec.template.spec.containers[0].image)')"
test "$ACTUAL_IMAGE" = "$GBW_PINNED_IMAGE"

STARTED="$(date +%s)"
EXECUTION="$(gcloud run jobs execute "$BENCH_JOB"   --project "$GBW_GCP_PROJECT"   --region "$GBW_REGION"   --task-timeout 30m   --wait   --format='value(metadata.name)')"
ENDED="$(date +%s)"
test -n "$EXECUTION"

gcloud storage cp --recursive "gs://${GBW_BUCKET}/${BUNDLE_PREFIX}/**" "$ARTIFACT_DIR/" >/dev/null
BUNDLE_DIR="$ARTIFACT_DIR"
if [[ -d "$ARTIFACT_DIR/bundle" ]]; then
  BUNDLE_DIR="$ARTIFACT_DIR/bundle"
fi
METRICS="$(find "$ARTIFACT_DIR" -name benchmark.json -type f -print -quit)"
SUMS="$(find "$ARTIFACT_DIR" -name SHA256SUMS -type f -print -quit)"
test -n "$METRICS" && test -n "$SUMS"
(
  cd "$(dirname "$SUMS")"
  sha256sum -c "$(basename "$SUMS")"
)

python3 - "$METRICS" "$GBW_PINNED_IMAGE" <<'PY'
import json, sys
path, image = sys.argv[1:]
with open(path, encoding="utf-8") as handle:
    data = json.load(handle)
assert data["engine"] == "demucs-pytorch"
assert data["model"] == "htdemucs_6s"
assert data["modelSha256"] == "34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd"
assert data["strategy"] == "pytorch-cpu-s1-o0.5-t8"
assert data["device"] == "cpu" and data["shifts"] == 1 and abs(float(data["overlap"]) - 0.5) < 1e-12
assert data.get("imageIdentity") == image
assert not [row for row in data["qualityFindings"] if row.get("severity") == "reject"]
assert set(data["stemMetrics"]) == {"drums","bass","other","vocals","guitar","piano"}
assert set(data["preparedSha256"]) == {"backing","guitar"}
assert int(data["totalMs"]) <= 900_000, f"W4 runtime exceeds 15-minute gate: {data['totalMs']} ms"
bundle = {row["path"] for row in data["bundleFiles"]}
expected = {f"stems/{name}.wav" for name in ("drums","bass","other","vocals","guitar","piano")}
expected |= {"prepared/backing.wav","prepared/guitar.wav","recombined.wav"}
assert expected <= bundle
print(json.dumps({
    "totalMs": data["totalMs"],
    "inferenceMs": data["inferenceMs"],
    "renderMs": data["renderMs"],
    "maxRssKiB": data["maxRssKiB"],
    "estimatedCostUsd": data["estimatedCostUsd"],
    "otherEnergyShare": data["qualitySummary"]["energyShareByStem"]["other"],
    "qualityFindings": data["qualityFindings"],
}, sort_keys=True))
PY

cat > "$ARTIFACT_DIR/W4_IDENTITY.txt" <<EOF
source_sha=${GITHUB_SHA:-local}
image=${GBW_PINNED_IMAGE}
execution=${EXECUTION}
fixture_sha256=${FIXTURE_SHA}
fixture_duration_seconds=180
strategy=cpu8_s1_o05
cloud_wall_seconds=$((ENDED-STARTED))
pricing_basis=${PRICING_BASIS}
owner_listening=REQUIRED_NOT_YET_PASSED
EOF

printf 'w4_shadow_benchmark=PASS execution=%s artifact_dir=%s\n' "$EXECUTION" "$ARTIFACT_DIR"
