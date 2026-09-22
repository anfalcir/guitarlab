#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
GBW_REGION="${GBW_REGION:-us-central1}"
GBW_REPOSITORY="${GBW_REPOSITORY:-gbw}"
GBW_JOB_NAME="${GBW_JOB_NAME:-gbw-demucs}"
GBW_IMAGE="${GBW_REGION}-docker.pkg.dev/${GBW_GCP_PROJECT}/${GBW_REPOSITORY}/remote-worker"
GBW_WORKER_SA="gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"

GBW_TAG="$(git rev-parse --short=12 HEAD)"
BUILD_ID="$(gcloud builds submit cloud/remote-separation/worker \
  --project "$GBW_GCP_PROJECT" \
  --tag "${GBW_IMAGE}:${GBW_TAG}" \
  --async \
  --format='value(id)')"
test -n "$BUILD_ID"
echo "Cloud Build submitted: $BUILD_ID"

while true; do
  BUILD_STATUS="$(gcloud builds describe "$BUILD_ID" \
    --project "$GBW_GCP_PROJECT" \
    --format='value(status)')"
  case "$BUILD_STATUS" in
    SUCCESS)
      break
      ;;
    QUEUED|PENDING|WORKING)
      sleep 10
      ;;
    *)
      echo "Cloud Build $BUILD_ID finished with status: $BUILD_STATUS" >&2
      exit 1
      ;;
  esac
done

GBW_DIGEST="$(gcloud artifacts docker images describe "${GBW_IMAGE}:${GBW_TAG}" --project "$GBW_GCP_PROJECT" --format='value(image_summary.digest)')"
GBW_PINNED_IMAGE="${GBW_IMAGE}@${GBW_DIGEST}"

RUN_DEPLOY=(gcloud run jobs deploy)
DELAY_ARGS=()
if gcloud beta run jobs deploy --help 2>&1 | grep -Fq -- 'delay-execution'; then
  RUN_DEPLOY=(gcloud beta run jobs deploy)
  DELAY_ARGS=(--no-delay-execution)
  echo "Using beta Cloud Run deploy with explicit --no-delay-execution."
else
  echo "Cloud SDK does not expose delay-execution on beta deploy; using stable deploy and relying on post-deploy contract verification."
fi

"${RUN_DEPLOY[@]}" "$GBW_JOB_NAME" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --image "$GBW_PINNED_IMAGE" \
  --service-account "$GBW_WORKER_SA" \
  --tasks 1 \
  --parallelism 1 \
  --cpu 8 \
  --memory 16Gi \
  --task-timeout 30m \
  --max-retries 0 \
  "${DELAY_ARGS[@]}" \
  --set-env-vars "GBW_BUCKET=${GBW_BUCKET},GBW_VCPU=8,GBW_DEMUCS_BINARY=/usr/local/bin/demucs_mt.cpp.main,GBW_DEMUCS_MT_THREADS=4,OPENBLAS_NUM_THREADS=2,OMP_NUM_THREADS=2" \
  --add-volume "name=model,type=cloud-storage,bucket=${GBW_BUCKET}" \
  --add-volume-mount "volume=model,mount-path=/model" \
  --quiet

JOB_JSON="$(gcloud run jobs describe "$GBW_JOB_NAME" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --format=json)"
python3 -c 'import json,sys; d=json.load(sys.stdin); delay=str(d["spec"].get("delayExecution", "false")).lower(); assert delay not in ("true","1"), f"delayExecution unexpectedly enabled: {delay}"; c=d["spec"]["template"]["spec"]["template"]["spec"]["containers"][0]; env={row["name"]:row.get("value","") for row in c.get("env",[])}; expected={"GBW_VCPU":"8","GBW_DEMUCS_BINARY":"/usr/local/bin/demucs_mt.cpp.main","GBW_DEMUCS_MT_THREADS":"4","OPENBLAS_NUM_THREADS":"2","OMP_NUM_THREADS":"2"}; missing={k:(env.get(k),v) for k,v in expected.items() if env.get(k)!=v}; assert not missing, f"unexpected production inference contract: {missing}"' <<<"$JOB_JSON"

printf '%s\n' "$GBW_PINNED_IMAGE"
