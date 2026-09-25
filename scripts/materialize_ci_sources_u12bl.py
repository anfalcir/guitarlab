#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bk.py"
PATCH = ROOT / ".source-parts/U12blSingleInferenceAndU4Identity.patch"
PATCH_BLOB = "4659d5617970244ddaa9e4f8e88c0cc81924eba4"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": (
        "b1674e09b690173dda49d412872cb2c0b1bf2528",
        "0c7337422142517f55756d83247aa6fea5e64b32",
    ),
    "scripts/u12_rc20_shadow_benchmark.sh": (
        "59d3b857fd9422cb921d4608d7daef7922ff0f85",
        "41c595acfa36910c0341eb8a0ce015b67ccabac7",
    ),
    ".github/workflows/u4-cloud-integration-smoke.yml": (
        "982e6cbcd29b596c384e73e6cb6800adabaeab58",
        "9dbd02c0bbc30e839d932b348bbe65818ad0cb71",
    ),
}


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))


def ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == after for rel, (_, after) in TARGETS.items())


def baseline_ready() -> bool:
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == before for rel, (before, _) in TARGETS.items())


def verify() -> None:
    run("git", "diff", "--check")
    u7 = (ROOT / ".github/workflows/u7-cloud-backend.yml").read_text(encoding="utf-8")
    bench = (ROOT / "scripts/u12_rc20_shadow_benchmark.sh").read_text(encoding="utf-8")
    u4 = (ROOT / ".github/workflows/u4-cloud-integration-smoke.yml").read_text(encoding="utf-8")

    if u7.count("GBW_W4_WARM_PROBE: '0'") != 2:
        raise SystemExit("U12bl requires single-inference baseline and CPU4 cells")
    if "GBW_W4_WARM_PROBE: '1'" in u7:
        raise SystemExit("U12bl stale warm-probe workflow path remains")
    if 'WARM_PROBE="${GBW_W4_WARM_PROBE:-0}"' not in bench:
        raise SystemExit("U12bl benchmark default must be single inference")
    if 'MODEL_PROBE="${GBW_W4_MODEL_PROBE:-0}"' not in bench:
        raise SystemExit("U12bl benchmark default model probe must be disabled")
    if "owner_listening=WAIVED_D092" not in bench:
        raise SystemExit("U12bl W5 waiver identity guard failed")
    official = "34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd"
    if f"MODEL_SHA256: {official}" not in u4:
        raise SystemExit("U12bl standalone U4 model identity mismatch")
    if "09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856" in u4:
        raise SystemExit("U12bl standalone U4 retains legacy model identity")
    if "Run consolidated W3/W4 shadow qualification" not in u7:
        raise SystemExit("U12bl consolidated W3/W4 step missing")
    if "w5_listening:" in u7 or "[run u7 w5]" in u7:
        raise SystemExit("U12bl retired W5 workflow path returned")


if not PATCH.is_file():
    raise SystemExit("Missing U12bl single-inference patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bl patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bl single-inference/U4 identity")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bk materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bl baseline blob mismatch after U12bk")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bl terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bl single-inference/U4 identity")
