#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PREVIOUS=ROOT/"scripts/materialize_ci_sources_u12az.py"
PATCH=ROOT/".source-parts/U12baCpu4WarmProbe.patch"
PATCH_BLOB="a7462c8bd31ad29116bb4938119ff0c0d824a705"
TARGETS={
    "cloud/remote-separation/worker/benchmark.py": [
        "f67e079cabd19fef59e9f708252ec781271fef56",
        "1ed2c871e705a764d4168653b7b22a112c928c94"
    ],
    "cloud/remote-separation/worker/tests/test_benchmark.py": [
        "8698df5747a22b56ce2276ce4f4501eb1ef13669",
        "dd76c816381d15916a393f5d2d2690461dd01f56"
    ]
}
CHECKS=[
    [
        "cloud/remote-separation/worker/benchmark.py",
        "cpu4_s1_o05"
    ],
    [
        "cloud/remote-separation/worker/benchmark.py",
        "warmProbe"
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
            raise SystemExit(f"u12ba guard failed: {rel} missing {token}")
    run("python3","-m","unittest","discover","-s","cloud/remote-separation/worker/tests","-p","test_benchmark.py","-v")

if not PATCH.is_file(): raise SystemExit("missing u12ba patch")
if blob(PATCH)!=PATCH_BLOB: raise SystemExit("u12ba patch blob mismatch")
if ready():
    verify()
    run("git","apply","--check","--reverse",str(PATCH))
    print("u12ba already materialized")
    raise SystemExit(0)
if not PREVIOUS.is_file(): raise SystemExit("missing previous materializer")
run("python3",str(PREVIOUS))
if not baseline_ready(): raise SystemExit("u12ba baseline blob mismatch")
run("git","apply","--check",str(PATCH))
run("git","apply",str(PATCH))
if not ready(): raise SystemExit("u12ba terminal blob mismatch")
verify()
run("git","apply","--check","--reverse",str(PATCH))
print("u12ba materialized")
