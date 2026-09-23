#!/usr/bin/env bash
set -euo pipefail

: "${GBW_GCP_PROJECT:?Set GBW_GCP_PROJECT}"
: "${GBW_ALLOWED_UIDS:?Set GBW_ALLOWED_UIDS}"

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUT="${1:-}"
FUNCTIONS_DIR="$ROOT/cloud/remote-separation/functions"

test -d "$FUNCTIONS_DIR/node_modules/firebase-admin" || {
  echo "firebase-admin dependencies are not installed; run npm ci first." >&2
  exit 1
}

(
  cd "$FUNCTIONS_DIR"
  node <<'NODE'
const { applicationDefault, initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");

(async () => {
  const projectId = process.env.GBW_GCP_PROJECT;
  const uids = (process.env.GBW_ALLOWED_UIDS || "")
    .split(",")
    .map(value => value.trim())
    .filter(Boolean);

  if (uids.length === 0) throw new Error("GBW_ALLOWED_UIDS is empty");

  const app = initializeApp({ credential: applicationDefault(), projectId }, "u7-email-auth-check");
  const auth = getAuth(app);

  for (const uid of uids) {
    const user = await auth.getUser(uid);
    if (user.disabled) throw new Error("An allowlisted Firebase user is disabled");
    if (!user.email) throw new Error("An allowlisted Firebase user has no email");
    const providers = new Set(user.providerData.map(row => row.providerId));
    if (!providers.has("password")) {
      throw new Error("An allowlisted Firebase user is not backed by the password provider");
    }
  }

  console.log("allowlisted_password_users=PASS");
  console.log(`allowlisted_user_count=${uids.length}`);
})().catch(error => {
  console.error("allowlisted_password_users=FAIL");
  console.error(String(error && error.message ? error.message : error));
  process.exit(1);
});
NODE
) | if [[ -n "$OUT" ]]; then tee "$OUT"; else cat; fi
