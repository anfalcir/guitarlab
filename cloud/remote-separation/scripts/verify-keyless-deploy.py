#!/usr/bin/env python3
"""Static security gate for the RC5 keyless deployment path."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
workflow = (ROOT / ".github/workflows/rc5-cloud-deploy.yml").read_text()
bootstrap = (ROOT / "cloud/remote-separation/scripts/bootstrap-github-actions.sh").read_text()
functions = (ROOT / "cloud/remote-separation/scripts/deploy-functions.sh").read_text()

required_workflow = (
    "workflow_dispatch:",
    "id-token: write",
    "contents: read",
    "github.repository_id == '1374753325'",
    "github.repository_owner_id == '216095257'",
    "github.ref == 'refs/heads/dev/android-6.0'",
    "projects/119299736855/locations/global/workloadIdentityPools/github-gbw/providers/github-actions",
    "gbw-github-deployer@gbwapp-ef048.iam.gserviceaccount.com",
    "secrets.GBW_ALLOWED_UIDS",
)
required_bootstrap = (
    "assertion.repository_id=='${GBW_GITHUB_REPOSITORY_ID}'",
    "assertion.repository_owner_id=='${GBW_GITHUB_OWNER_ID}'",
    "assertion.ref=='${GBW_GITHUB_REF}'",
    "assertion.workflow_ref=='${GBW_GITHUB_WORKFLOW_REF}'",
    "roles/iam.workloadIdentityUser",
)

for marker in required_workflow:
    assert marker in workflow, f"missing workflow security marker: {marker}"
for marker in required_bootstrap:
    assert marker in bootstrap, f"missing WIF bootstrap security marker: {marker}"
assert "push:" not in workflow and "pull_request:" not in workflow
assert "service_account_key" not in workflow.lower()
assert "credentials_json" not in workflow.lower()
assert "GBW_ALLOWED_UIDS=%s" in functions
assert "trap 'rm -f \"$GBW_ENV_FILE\"' EXIT" in functions
print("RC5_KEYLESS_DEPLOY_CONTRACT_OK")
