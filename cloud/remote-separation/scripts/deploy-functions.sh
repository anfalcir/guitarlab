#!/usr/bin/env bash
set -euo pipefail
: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_BUCKET:?Set GBW_BUCKET}"
: "${GBW_ALLOWED_UIDS:?Set GBW_ALLOWED_UIDS}"
GBW_REGION="${GBW_REGION:-us-central1}"
GBW_BACKEND_REVISION="${GBW_BACKEND_REVISION:-$(git rev-parse --short=12 HEAD)}"
GBW_ENV_FILE="cloud/remote-separation/functions/.env.${GBW_GCP_PROJECT}"
umask 077
trap 'rm -f "$GBW_ENV_FILE"' EXIT
{
  printf 'GBW_BUCKET=%s\n' "$GBW_BUCKET"
  printf 'GBW_REGION=%s\n' "$GBW_REGION"
  printf 'GBW_ALLOWED_UIDS=%s\n' "$GBW_ALLOWED_UIDS"
  printf 'GBW_REQUIRE_APP_CHECK=false\n'
  printf 'GBW_ORCHESTRATOR_SA=gbw-orchestrator@%s.iam.gserviceaccount.com\n' "$GBW_GCP_PROJECT"
  printf 'GBW_BACKEND_REVISION=%s\n' "$GBW_BACKEND_REVISION"
} > "$GBW_ENV_FILE"

npm ci --prefix cloud/remote-separation/functions
npm run build --prefix cloud/remote-separation/functions
test -f cloud/remote-separation/functions/lib/index.js

firebase deploy --config cloud/remote-separation/firebase.json --project "$GBW_GCP_PROJECT" --only auth,firestore:rules,firestore:indexes,storage,functions \
  --force --non-interactive
printf 'Firebase Authentication (anonymous), rules and Functions deployed; App Check enforcement remains pending physical validation.\n'
