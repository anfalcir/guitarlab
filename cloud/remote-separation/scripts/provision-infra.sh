#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT to the billed Firebase project}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
GBW_REGION="${GBW_REGION:-us-central1}"
gcloud config set project "$GBW_GCP_PROJECT"
gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com \
  firestore.googleapis.com firebasestorage.googleapis.com cloudfunctions.googleapis.com \
  identitytoolkit.googleapis.com firebaseappcheck.googleapis.com eventarc.googleapis.com pubsub.googleapis.com \
  cloudbilling.googleapis.com firebaseextensions.googleapis.com storage.googleapis.com
gcloud artifacts repositories describe gbw --location "$GBW_REGION" >/dev/null 2>&1 || \
  gcloud artifacts repositories create gbw --repository-format=docker --location "$GBW_REGION" --description "GBW immutable remote worker images"
gcloud storage buckets describe "gs://${GBW_BUCKET}" >/dev/null 2>&1 || \
  gcloud storage buckets create "gs://${GBW_BUCKET}" --project "$GBW_GCP_PROJECT" --location "$GBW_REGION" --uniform-bucket-level-access
gcloud iam service-accounts describe "gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" >/dev/null 2>&1 || \
  gcloud iam service-accounts create gbw-worker --display-name "GBW Demucs worker"
gcloud iam service-accounts describe "gbw-orchestrator@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" >/dev/null 2>&1 || \
  gcloud iam service-accounts create gbw-orchestrator --display-name "GBW remote orchestrator"
gcloud projects add-iam-policy-binding "$GBW_GCP_PROJECT" --member "serviceAccount:gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --role roles/datastore.user --condition=None >/dev/null
gcloud storage buckets add-iam-policy-binding "gs://${GBW_BUCKET}" --member "serviceAccount:gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --role roles/storage.objectAdmin >/dev/null
gcloud projects add-iam-policy-binding "$GBW_GCP_PROJECT" --member "serviceAccount:gbw-orchestrator@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --role roles/run.developer --condition=None >/dev/null
gcloud projects add-iam-policy-binding "$GBW_GCP_PROJECT" --member "serviceAccount:gbw-orchestrator@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --role roles/datastore.user --condition=None >/dev/null
gcloud storage buckets add-iam-policy-binding "gs://${GBW_BUCKET}" --member "serviceAccount:gbw-orchestrator@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --role roles/storage.objectAdmin >/dev/null
gcloud iam service-accounts add-iam-policy-binding "gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --member "serviceAccount:gbw-orchestrator@${GBW_GCP_PROJECT}.iam.gserviceaccount.com" --role roles/iam.serviceAccountUser >/dev/null
cat > /tmp/gbw-lifecycle.json <<'JSON'
{"rule":[{"action":{"type":"Delete"},"condition":{"age":1,"matchesPrefix":["remote/v1/users/"]}}]}
JSON
gcloud storage buckets update "gs://${GBW_BUCKET}" --lifecycle-file=/tmp/gbw-lifecycle.json
rm -f /tmp/gbw-lifecycle.json
echo "Infrastructure base provisioned without service-account keys."
