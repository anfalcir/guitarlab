#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PREVIOUS=ROOT/"scripts/materialize_ci_sources_u12ba.py"
PATCH=ROOT/".source-parts/U12bbW4ParameterizedRunner.patch"
PATCH_BLOB="88fad9d496c98b29d9ad1984364336d0b9d8a211"
TARGETS={
    "scripts/u12_rc20_shadow_benchmark.sh": [
        "ac4f8ed08734b9f23c669fbaf0375686652bd3a7",
        "cdffef35b3d691e406450a50a372ca67a15af1d7"
    ]
}
CHECKS=[
    [
        "scripts/u12_rc20_shadow_benchmark.sh",
        "GBW_W4_MODEL_PROBE"
    ],
    [
        "scripts/u12_rc20_shadow_benchmark.sh",
        "model-probe.json"
    ],
    [
        "scripts/u12_rc20_shadow_benchmark.sh",
        "GBW_W4_VARIANT"
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
            raise SystemExit(f"u12bb guard failed: {rel} missing {token}")
    run("bash","-n","scripts/u12_rc20_shadow_benchmark.sh")

if not PATCH.is_file(): raise SystemExit("missing u12bb patch")
if blob(PATCH)!=PATCH_BLOB: raise SystemExit("u12bb patch blob mismatch")
if ready():
    verify()
    run("git","apply","--check","--reverse",str(PATCH))
    print("u12bb already materialized")
    raise SystemExit(0)
if not PREVIOUS.is_file(): raise SystemExit("missing previous materializer")
run("python3",str(PREVIOUS))
if not baseline_ready(): raise SystemExit("u12bb baseline blob mismatch")
run("git","apply","--check",str(PATCH))
run("git","apply",str(PATCH))
if not ready(): raise SystemExit("u12bb terminal blob mismatch")
verify()
run("git","apply","--check","--reverse",str(PATCH))
print("u12bb materialized")
