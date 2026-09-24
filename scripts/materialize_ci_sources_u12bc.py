#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PREVIOUS=ROOT/"scripts/materialize_ci_sources_u12bb.py"
PATCH=ROOT/".source-parts/U12bcCpu4MatrixWorkflow.patch"
PATCH_BLOB="233a9376bcd9878c69ef1c1b2d693c143b138183"
TARGETS={
    ".github/workflows/u7-cloud-backend.yml": [
        "12193d34846906b16aa4657196f11c7130017edf",
        "6a09b304930cbe7175d4da3463d7a8b22e692142"
    ]
}
CHECKS=[
    [
        ".github/workflows/u7-cloud-backend.yml",
        "Run W4 CPU4 matrix cell"
    ],
    [
        ".github/workflows/u7-cloud-backend.yml",
        "[run u7 matrix]"
    ],
    [
        ".github/workflows/u7-cloud-backend.yml",
        "timeout-minutes: 90"
    ]
]

def out(*args:str)->str:
    return subprocess.check_output(list(args),cwd=ROOT,text=True).strip()
def run(*args:str)->None:
    subprocess.run(list(args),cwd=ROOT,check=True)
def blob(path:Path)->str:
    return out("git","hash-object",str(path))
def ready()->bool:
    return all((ROOT/rel).is_file() and blob(ROOT/rel)==after for rel,(_,after) in TARGETS.items())
def baseline_ready()->bool:
    for rel,(before,_) in TARGETS.items():
        p=ROOT/rel
        if before is None:
            if p.exists(): return False
        elif not p.is_file() or blob(p)!=before:
            return False
    return True
def verify()->None:
    run("git","diff","--check")
    for rel,token in CHECKS:
        if token not in (ROOT/rel).read_text(encoding="utf-8"):
            raise SystemExit(f"u12bc guard failed: {rel} missing {token}")

if not PATCH.is_file(): raise SystemExit("missing u12bc patch")
if blob(PATCH)!=PATCH_BLOB: raise SystemExit("u12bc patch blob mismatch")
if ready():
    verify()
    run("git","apply","--check","--reverse",str(PATCH))
    print("u12bc already materialized")
    raise SystemExit(0)
if not PREVIOUS.is_file(): raise SystemExit("missing previous materializer")
run("python3",str(PREVIOUS))
if not baseline_ready(): raise SystemExit("u12bc baseline blob mismatch")
run("git","apply","--check",str(PATCH))
run("git","apply",str(PATCH))
if not ready(): raise SystemExit("u12bc terminal blob mismatch")
verify()
run("git","apply","--check","--reverse",str(PATCH))
print("u12bc materialized")
