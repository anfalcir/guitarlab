#!/usr/bin/env python3
from __future__ import annotations

import base64
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ad.py"
PATCH_B64 = ROOT / ".source-parts/U12aeDisabledAuthCopy.patch.b64"
PATCH_B64_BLOB = "823d958199c8fa17efe2fab3bc80d94c6fddf695"
PATCH_BLOB = "2dcd17c06690183909f5d91f00450f510cc7b19e"
FILES = {
    "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt":
        "fd1c599fab8200ceaeb560cc4b0e0a3cf1547685",
}
EXPECTED_COPY = (
    "A autenticação da conta de separação está desativada no serviço. "
    "A fonte foi preservada; tente novamente após a configuração do serviço."
)
REJECTED_COPY = "O login da conta de separação está desativado no serviço."

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def ready() -> bool:
    return all(
        (ROOT / rel).is_file()
        and out("git", "hash-object", str(ROOT / rel)) == expected
        for rel, expected in FILES.items()
    )

def verify_semantics() -> None:
    run("git", "diff", "--check")
    text = (ROOT / next(iter(FILES))).read_text(errors="replace")
    if EXPECTED_COPY not in text:
        raise SystemExit("U12ae semantic guard failed: actionable disabled-auth copy missing")
    if REJECTED_COPY in text:
        raise SystemExit("U12ae semantic guard failed: obsolete login copy remains")

if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ae disabled-auth copy payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ae base64 payload blob mismatch")
try:
    encoded_patch = b"".join(PATCH_B64.read_bytes().split())
    patch = base64.b64decode(encoded_patch, validate=True)
except Exception as exc:
    raise SystemExit(f"U12ae base64 payload is invalid: {exc}") from exc
with tempfile.NamedTemporaryFile(prefix="u12ae-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ae decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ae disabled-auth copy")
    else:
        if not PREVIOUS.is_file():
            raise SystemExit("Missing U12ad materializer")
        run("python3", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready():
            raise SystemExit("U12ae final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ae disabled-auth copy")
finally:
    temp.unlink(missing_ok=True)
