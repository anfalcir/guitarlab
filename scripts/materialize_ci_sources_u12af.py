#!/usr/bin/env python3
from __future__ import annotations

import base64
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ae.py"
PATCH_B64 = ROOT / ".source-parts/U12afU4AuthContract.patch.b64"
PATCH_B64_BLOB = "1370ebe8148f251cc63ce3eb6542f1c5390c0c19"
PATCH_BLOB = "a030f6bc74d4ba130e29200fdbc68053cdd1b709"
FILES = {
    "scripts/u4_cloud_integration_smoke.sh": "71c1868bf427ad526d3e5ecdcefde9c1e3d89a9e",
    "scripts/bootstrap_u4_github_wif.sh": "91c429f8d165107d975e4db0a9f5e7e9628f7c04",
}
CHECKS = (
    ("scripts/u4_cloud_integration_smoke.sh", 'claims.get("sub") != expected_uid', True),
    ("scripts/u4_cloud_integration_smoke.sh", 'claims.get("user_id") != expected_uid', True),
    ("scripts/u4_cloud_integration_smoke.sh", 'claims.get("aud") != project_id', True),
    ("scripts/u4_cloud_integration_smoke.sh", 'claims.get("iss")', True),
    ("scripts/u4_cloud_integration_smoke.sh", '["localId"]', False),
    ("scripts/bootstrap_u4_github_wif.sh", '--member="$PRINCIPAL"', True),
)

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def ready() -> bool:
    return all(
        (ROOT / rel).is_file() and out("git", "hash-object", str(ROOT / rel)) == expected
        for rel, expected in FILES.items()
    )

def verify_semantics() -> None:
    run("git", "diff", "--check")
    run("bash", "-n", str(ROOT / "scripts/u4_cloud_integration_smoke.sh"))
    run("bash", "-n", str(ROOT / "scripts/bootstrap_u4_github_wif.sh"))
    for rel, needle, present in CHECKS:
        text = (ROOT / rel).read_text(errors="replace")
        if (needle in text) != present:
            raise SystemExit(f"U12af semantic guard failed: {rel} :: {needle} :: expected={present}")

if not PATCH_B64.is_file():
    raise SystemExit("Missing U12af U4 auth-contract payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12af base64 payload blob mismatch")
try:
    patch = base64.b64decode(b"".join(PATCH_B64.read_bytes().split()), validate=True)
except Exception as exc:
    raise SystemExit(f"U12af base64 payload is invalid: {exc}") from exc
with tempfile.NamedTemporaryFile(prefix="u12af-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12af decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12af U4 auth contract")
    else:
        if not PREVIOUS.is_file():
            raise SystemExit("Missing U12ae materializer")
        run("python3", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready():
            raise SystemExit("U12af final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12af U4 auth contract")
finally:
    temp.unlink(missing_ok=True)
