#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
GBW_REGION="${GBW_REGION:-us-central1}"
gcloud run jobs execute gbw-demucs --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --wait
gcloud run jobs executions list --job gbw-demucs --project "$GBW_GCP_PROJECT" --region "$GBW_REGION" --limit 1
