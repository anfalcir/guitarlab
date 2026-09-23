#!/usr/bin/env python3
from __future__ import annotations

import base64
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12af.py"
PATCH_B64 = ROOT / ".source-parts/U12agU7ShadowDependencies.patch.b64"
PATCH_B64_BLOB = "e468b6abaa2620cc04534dfa029f205ea1ab7529"
PATCH_BLOB = "e47b034c781e89144fa0b5ea01739fcb1f0d78b4"
FILES = {".github/workflows/u7-cloud-backend.yml": "4e9f1883c98df3da86ef88288443dfceba6cb697"}
CHECKS = (
    ("Set up Node.js for cloud smoke and production Functions", True),
    ("Install Firebase Admin dependencies for cloud smoke", True),
    ("npm ci --prefix cloud/remote-separation/functions", True),
    ("Set up Node.js for production Functions", False),
)

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def ready() -> bool:
    return all((ROOT / rel).is_file() and out("git", "hash-object", str(ROOT / rel)) == expected for rel, expected in FILES.items())

def verify_semantics() -> None:
    run("git", "diff", "--check")
    text = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(errors="replace")
    for needle, present in CHECKS:
        if (needle in text) != present:
            raise SystemExit(f"U12ag semantic guard failed: {needle} :: expected={present}")

if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ag U7 shadow-dependencies payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ag base64 payload blob mismatch")
try:
    patch = base64.b64decode(b"".join(PATCH_B64.read_bytes().split()), validate=True)
except Exception as exc:
    raise SystemExit(f"U12ag base64 payload is invalid: {exc}") from exc
with tempfile.NamedTemporaryFile(prefix="u12ag-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ag decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ag U7 shadow dependencies")
    else:
        if not PREVIOUS.is_file():
            raise SystemExit("Missing U12af materializer")
        run("python3", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready():
            raise SystemExit("U12ag final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ag U7 shadow dependencies")
finally:
    temp.unlink(missing_ok=True)
