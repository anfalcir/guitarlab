#!/usr/bin/env bash
set -euo pipefail
PROJECT="${GBW_GCP_PROJECT:-gbwapp-ef048}"
POOL="${GBW_WIF_POOL:-github-gbw}"
PROVIDER="${GBW_WIF_PROVIDER:-github-actions}"
SA="${GBW_DEPLOYER_SA:-gbw-github-deployer@${PROJECT}.iam.gserviceaccount.com}"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT" --format='value(projectNumber)')"
PROVIDER_NAME="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/providers/${PROVIDER}"
COND="assertion.repository_owner_id=='216095257' && ((assertion.repository_id=='1374753325' && assertion.ref=='refs/heads/dev/android-6.0' && assertion.workflow_ref=='anfalcir/gbw/.github/workflows/rc5-cloud-deploy.yml@refs/heads/dev/android-6.0') || (assertion.repository_id=='1361533070' && assertion.ref=='refs/heads/main' && assertion.workflow_ref=='anfalcir/guitarlab/.github/workflows/u4-cloud-integration-smoke.yml@refs/heads/main'))"
gcloud iam workload-identity-pools providers update-oidc "$PROVIDER_NAME"   --project "$PROJECT"   --issuer-uri="https://token.actions.githubusercontent.com"   --attribute-mapping="google.subject=assertion.sub,attribute.repository_id=assertion.repository_id,attribute.repository_owner_id=assertion.repository_owner_id,attribute.ref=assertion.ref,attribute.workflow_ref=assertion.workflow_ref"   --attribute-condition="$COND"
PRINCIPAL="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository_id/1361533070"
gcloud iam service-accounts add-iam-policy-binding "$SA" --project "$PROJECT"   --role=roles/iam.workloadIdentityUser --member="$PRINCIPAL"
# Narrow object access required only for the isolated U4 real-cloud smoke namespace.
gcloud storage buckets add-iam-policy-binding "gs://${PROJECT}.firebasestorage.app" \
  --member="serviceAccount:${SA}" --role="roles/storage.objectUser" >/dev/null
echo "GuitarLab U4 WIF trust enabled without weakening GBW trust."
