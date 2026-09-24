#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PREVIOUS=ROOT/"scripts/materialize_ci_sources_u12bc.py"
PATCH=ROOT/".source-parts/U12bdCrossHostParityEnvelope.patch"
PATCH_BLOB="02797b46918d7c08d93c594ad0b1549d163a431f"
TARGETS={
    "scripts/u12_rc20_parity.sh": ("80649650422f3be64972ebd9dc1189fc7ddeb06f", "437793c147e46708b1d52c565850ef9925e4cdf2"),
}
def out(*args:str)->str:
    return subprocess.check_output(list(args),cwd=ROOT,text=True).strip()
def run(*args:str)->None:
    subprocess.run(list(args),cwd=ROOT,check=True)
def blob(path:Path)->str:
    return out("git","hash-object",str(path))
def ready()->bool:
    return all((ROOT/rel).is_file() and blob(ROOT/rel)==after for rel,(_,after) in TARGETS.items())
def baseline_ready()->bool:
    return all((ROOT/rel).is_file() and blob(ROOT/rel)==before for rel,(before,_) in TARGETS.items())
def verify()->None:
    run("git","diff","--check")
    text=(ROOT/"scripts/u12_rc20_parity.sh").read_text(encoding="utf-8")
    for token in ("REL_TOL = 0.01","GAIN_ABS_TOL_DB = 0.05","prepared:","ENERGY_SHARE_ABS_TOL = 0.01","sudo rm -rf"):
        if token not in text:
            raise SystemExit(f"U12bd parity guard failed: {token}")
    run("bash","-n","scripts/u12_rc20_parity.sh")
if not PATCH.is_file(): raise SystemExit("missing U12bd patch")
if blob(PATCH)!=PATCH_BLOB: raise SystemExit("U12bd patch blob mismatch")
if ready():
    verify()
    run("git","apply","--check","--reverse",str(PATCH))
    print("U12bd already materialized")
    raise SystemExit(0)
if not PREVIOUS.is_file(): raise SystemExit("missing U12bc materializer")
run("python3",str(PREVIOUS))
if not baseline_ready(): raise SystemExit("U12bd baseline blob mismatch")
run("git","apply","--check",str(PATCH))
run("git","apply",str(PATCH))
if not ready(): raise SystemExit("U12bd terminal blob mismatch")
verify()
run("git","apply","--check","--reverse",str(PATCH))
print("U12bd materialized")
