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
GBW_TAGGED_IMAGE="${GBW_IMAGE}:${GBW_TAG}"
REGISTRY_HOST="${GBW_REGION}-docker.pkg.dev"

echo "Configuring Docker authentication for ${REGISTRY_HOST}"
gcloud auth configure-docker "${REGISTRY_HOST}" --quiet >/dev/null

echo "Building RC20 worker locally on the authenticated GitHub runner"
docker build --pull --no-cache \
  --tag "${GBW_TAGGED_IMAGE}" \
  cloud/remote-separation/worker

echo "Pushing RC20 worker to Artifact Registry"
docker push "${GBW_TAGGED_IMAGE}"

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
  --set-env-vars "GBW_BUCKET=${GBW_BUCKET},GBW_VCPU=8,GBW_MODEL_REPO=/opt/demucs/models,GBW_DEMUCS_DEVICE=cpu,GBW_DEMUCS_SHIFTS=1,GBW_DEMUCS_OVERLAP=0.5,GBW_DEMUCS_CPU_THREADS=8,OMP_NUM_THREADS=8,MKL_NUM_THREADS=8,OPENBLAS_NUM_THREADS=8,NUMEXPR_NUM_THREADS=8" \
  --clear-volumes \
  --clear-volume-mounts \
  --quiet

JOB_JSON="$(gcloud run jobs describe "$GBW_JOB_NAME" \
  --project "$GBW_GCP_PROJECT" \
  --region "$GBW_REGION" \
  --format=json)"
python3 -c 'import json,sys; d=json.load(sys.stdin); delay=str(d["spec"].get("delayExecution", "false")).lower(); assert delay not in ("true","1"), f"delayExecution unexpectedly enabled: {delay}"; c=d["spec"]["template"]["spec"]["template"]["spec"]["containers"][0]; env={row["name"]:row.get("value","") for row in c.get("env",[])}; expected={"GBW_VCPU":"8","GBW_MODEL_REPO":"/opt/demucs/models","GBW_DEMUCS_DEVICE":"cpu","GBW_DEMUCS_SHIFTS":"1","GBW_DEMUCS_OVERLAP":"0.5","GBW_DEMUCS_CPU_THREADS":"8","OMP_NUM_THREADS":"8","MKL_NUM_THREADS":"8","OPENBLAS_NUM_THREADS":"8","NUMEXPR_NUM_THREADS":"8"}; missing={k:(env.get(k),v) for k,v in expected.items() if env.get(k)!=v}; assert not missing, f"unexpected official Demucs inference contract: {missing}"; assert not d["spec"]["template"]["spec"]["template"]["spec"].get("volumes"), "worker must not depend on runtime model volumes"' <<<"$JOB_JSON"

printf '%s\n' "$GBW_PINNED_IMAGE"
