#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PREVIOUS=ROOT/"scripts/materialize_ci_sources_u12ay.py"
PATCH=ROOT/".source-parts/U12azModelLoadProbe.patch"
PATCH_BLOB="7c7f4c830d956e849f8faca34c65943eb8905dce"
TARGETS={
    "cloud/remote-separation/worker/Dockerfile": [
        "72739f555ed4359d10b356af9b633b9ccb772f0c",
        "a10e90ac830f7f0b624a1e88a7b4e688c1fdb6e6"
    ],
    "cloud/remote-separation/worker/model_probe.py": [
        None,
        "c4b32b1158aaad064e3cca8f803e00c546726843"
    ]
}
CHECKS=[
    [
        "cloud/remote-separation/worker/Dockerfile",
        "model_probe.py"
    ],
    [
        "cloud/remote-separation/worker/model_probe.py",
        "GBW_PROBE_OUTPUT_PATH"
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
            raise SystemExit(f"u12az guard failed: {rel} missing {token}")

if not PATCH.is_file(): raise SystemExit("missing u12az patch")
if blob(PATCH)!=PATCH_BLOB: raise SystemExit("u12az patch blob mismatch")
if ready():
    verify()
    run("git","apply","--check","--reverse",str(PATCH))
    print("u12az already materialized")
    raise SystemExit(0)
if not PREVIOUS.is_file(): raise SystemExit("missing previous materializer")
run("python3",str(PREVIOUS))
if not baseline_ready(): raise SystemExit("u12az baseline blob mismatch")
run("git","apply","--check",str(PATCH))
run("git","apply",str(PATCH))
if not ready(): raise SystemExit("u12az terminal blob mismatch")
verify()
run("git","apply","--check","--reverse",str(PATCH))
print("u12az materialized")
