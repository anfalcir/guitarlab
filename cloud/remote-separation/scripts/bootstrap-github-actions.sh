#!/usr/bin/env bash
set -euo pipefail

# One-time owner-operated bootstrap for keyless GitHub Actions deployment.
# Run from the repository root in an authenticated Google Cloud Shell.

GBW_GCP_PROJECT="${GBW_GCP_PROJECT:-gbwapp-ef048}"
GBW_BUCKET="${GBW_BUCKET:-gbwapp-ef048.firebasestorage.app}"
GBW_REGION="${GBW_REGION:-us-central1}"
GBW_GITHUB_REPOSITORY="${GBW_GITHUB_REPOSITORY:-anfalcir/gbw}"
GBW_GITHUB_REPOSITORY_ID="${GBW_GITHUB_REPOSITORY_ID:-1374753325}"
GBW_GITHUB_OWNER_ID="${GBW_GITHUB_OWNER_ID:-216095257}"
GBW_GITHUB_REF="${GBW_GITHUB_REF:-refs/heads/dev/android-6.0}"
GBW_GITHUB_WORKFLOW_REF="${GBW_GITHUB_WORKFLOW_REF:-anfalcir/gbw/.github/workflows/rc5-cloud-deploy.yml@refs/heads/dev/android-6.0}"
GBW_WIF_POOL="${GBW_WIF_POOL:-github-gbw}"
GBW_WIF_PROVIDER="${GBW_WIF_PROVIDER:-github-actions}"
GBW_DEPLOYER_SA_NAME="${GBW_DEPLOYER_SA_NAME:-gbw-github-deployer}"

command -v gcloud >/dev/null
ACTIVE_ACCOUNT="$(gcloud auth list --filter=status:ACTIVE --format='value(account)' | head -1)"
test -n "$ACTIVE_ACCOUNT"

gcloud config set project "$GBW_GCP_PROJECT" >/dev/null
PROJECT_NUMBER="$(gcloud projects describe "$GBW_GCP_PROJECT" --format='value(projectNumber)')"
test -n "$PROJECT_NUMBER"

gcloud services enable \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  cloudresourcemanager.googleapis.com serviceusage.googleapis.com >/dev/null

export GBW_GCP_PROJECT GBW_BUCKET GBW_REGION
bash cloud/remote-separation/scripts/provision-infra.sh

DEPLOYER_SA="${GBW_DEPLOYER_SA_NAME}@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"
ORCHESTRATOR_SA="gbw-orchestrator@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"
WORKER_SA="gbw-worker@${GBW_GCP_PROJECT}.iam.gserviceaccount.com"

gcloud iam service-accounts describe "$DEPLOYER_SA" >/dev/null 2>&1 || \
  gcloud iam service-accounts create "$GBW_DEPLOYER_SA_NAME" \
    --display-name="GBW GitHub Actions deployer"

gcloud iam workload-identity-pools describe "$GBW_WIF_POOL" \
  --location=global >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools create "$GBW_WIF_POOL" \
    --location=global --display-name="GBW GitHub Actions"

ATTRIBUTE_MAPPING="google.subject=assertion.sub,attribute.repository_id=assertion.repository_id,attribute.repository_owner_id=assertion.repository_owner_id,attribute.ref=assertion.ref,attribute.workflow_ref=assertion.workflow_ref"
ATTRIBUTE_CONDITION="assertion.repository_id=='${GBW_GITHUB_REPOSITORY_ID}' && assertion.repository_owner_id=='${GBW_GITHUB_OWNER_ID}' && assertion.ref=='${GBW_GITHUB_REF}' && assertion.workflow_ref=='${GBW_GITHUB_WORKFLOW_REF}'"

if gcloud iam workload-identity-pools providers describe "$GBW_WIF_PROVIDER" \
  --workload-identity-pool="$GBW_WIF_POOL" --location=global >/dev/null 2>&1; then
  gcloud iam workload-identity-pools providers update-oidc "$GBW_WIF_PROVIDER" \
    --workload-identity-pool="$GBW_WIF_POOL" --location=global \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="$ATTRIBUTE_MAPPING" \
    --attribute-condition="$ATTRIBUTE_CONDITION"
else
  gcloud iam workload-identity-pools providers create-oidc "$GBW_WIF_PROVIDER" \
    --workload-identity-pool="$GBW_WIF_POOL" --location=global \
    --display-name="GBW GitHub Actions provider" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="$ATTRIBUTE_MAPPING" \
    --attribute-condition="$ATTRIBUTE_CONDITION"
fi

PRINCIPAL="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${GBW_WIF_POOL}/attribute.repository_id/${GBW_GITHUB_REPOSITORY_ID}"
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER_SA" \
  --role=roles/iam.workloadIdentityUser --member="$PRINCIPAL" >/dev/null

PROJECT_ROLES=(
  roles/artifactregistry.admin
  roles/cloudbuild.builds.editor
  roles/cloudfunctions.admin
  roles/datastore.indexAdmin
  roles/firebase.viewer
  roles/firebaseauth.admin
  roles/firebaserules.admin
  roles/run.admin
  roles/serviceusage.serviceUsageConsumer
)
for role in "${PROJECT_ROLES[@]}"; do
  gcloud projects add-iam-policy-binding "$GBW_GCP_PROJECT" \
    --member="serviceAccount:${DEPLOYER_SA}" --role="$role" \
    --condition=None >/dev/null
done

for runtime_sa in "$ORCHESTRATOR_SA" "$WORKER_SA"; do
  gcloud iam service-accounts add-iam-policy-binding "$runtime_sa" \
    --member="serviceAccount:${DEPLOYER_SA}" \
    --role=roles/iam.serviceAccountUser >/dev/null
done

# Submitting a build requires iam.serviceAccounts.actAs on the exact service
# account selected by Cloud Build. Resolve it from the project rather than
# assuming the legacy or Compute Engine default identity.
BUILD_SERVICE_ACCOUNT="$(gcloud builds get-default-service-account \
  --project="$GBW_GCP_PROJECT")"
test -n "$BUILD_SERVICE_ACCOUNT"
gcloud iam service-accounts add-iam-policy-binding "$BUILD_SERVICE_ACCOUNT" \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role=roles/iam.serviceAccountUser >/dev/null

# Firebase Functions uses the App Engine default service account as its
# runtime identity unless another account is explicitly configured.
FUNCTIONS_SERVICE_ACCOUNT="${GBW_GCP_PROJECT}@appspot.gserviceaccount.com"
gcloud iam service-accounts add-iam-policy-binding "$FUNCTIONS_SERVICE_ACCOUNT" \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role=roles/iam.serviceAccountUser >/dev/null

gcloud storage buckets add-iam-policy-binding "gs://${GBW_BUCKET}" \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role=roles/storage.objectViewer >/dev/null

# Cloud Build uploads the submitted worker source to its staging bucket before
# the build service takes over. Grant write access only to that dedicated
# bucket rather than broad project-wide Storage administration.
CLOUDBUILD_BUCKET="gs://${GBW_GCP_PROJECT}_cloudbuild"
gcloud storage buckets describe "$CLOUDBUILD_BUCKET" >/dev/null 2>&1 || \
  gcloud storage buckets create "$CLOUDBUILD_BUCKET" \
    --project="$GBW_GCP_PROJECT" --location="$GBW_REGION" \
    --uniform-bucket-level-access
gcloud storage buckets add-iam-policy-binding "$CLOUDBUILD_BUCKET" \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role=roles/storage.objectAdmin >/dev/null

PROVIDER_RESOURCE="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${GBW_WIF_POOL}/providers/${GBW_WIF_PROVIDER}"
printf '%s\n' \
  "GBW GitHub Actions federation configured." \
  "Authenticated operator: ${ACTIVE_ACCOUNT}" \
  "Repository: ${GBW_GITHUB_REPOSITORY} (immutable ID ${GBW_GITHUB_REPOSITORY_ID})" \
  "Allowed ref: ${GBW_GITHUB_REF}" \
  "Allowed workflow: ${GBW_GITHUB_WORKFLOW_REF}" \
  "Workload identity provider: ${PROVIDER_RESOURCE}" \
  "Deployment service account: ${DEPLOYER_SA}" \
  "No service-account key was created."
