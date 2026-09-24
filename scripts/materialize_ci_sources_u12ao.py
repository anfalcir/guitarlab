#!/usr/bin/env python3
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12an.py"
PATCH = ROOT / ".source-parts/U12aoOfficialDemucsW0W2.patch"
PATCH_BLOB = "9ce8198f597d2a0fc616a47dda9537e1d40b3cf1"
TARGETS = {
    ".github/workflows/u7-cloud-backend.yml": ("0c1cd61420c42eff1ced6354d1e6cd6919aa8ee9", "e0789d3e80003b0ee45a79e2e4de8ad5c6d0e2b2"),
    "cloud/remote-separation/functions/src/index.ts": ("b0c77616fa8b6d5beaf0056e62b270af89091c88", "451f6618b4275236e86982ad37e04758eeaca33a"),
    "cloud/remote-separation/functions/src/policy.ts": ("04a7dc673c669e8fb23f716b77a8dbe233b38683", "841794b8edb2655ed4c01727fc573f7deb06999c"),
    "cloud/remote-separation/functions/src/test/policy.test.ts": ("ea9f55ae164292a6bf1a11ef961a16a4ed98c428", "7e5cce7fb930da8ae40885e1a0c9e7927e1ced8c"),
    "cloud/remote-separation/schemas/remote-job.schema.json": ("e7d90a212b9a14b3085dd8aaf0fc143f2c6075b4", "87704e7d370523f643a2b3564ee2bd85f64eda8e"),
    "cloud/remote-separation/schemas/result-manifest.schema.json": ("71de9f71f97aff5dff1ac35c008c9c4608ee412f", "8a210f27f260f9726063cc127aabf2f4d87859fa"),
    "cloud/remote-separation/scripts/deploy-worker.sh": ("b00376eefe338cb4bebe8669a14f87b344c61374", "4ec9975d7cc2727e30677491150aac73a1540fff"),
    "cloud/remote-separation/worker/Dockerfile": ("3f82eb3cc2ea867912b7fea2c9ef024597369936", "eebea762fec7cc4c0d953b7856dcd50730279d0b"),
    "cloud/remote-separation/worker/audio_quality.py": (None, "31608474b4770f936e00a0fb67a9772e780004bd"),
    "cloud/remote-separation/worker/benchmark.py": ("7bdb5c9c86cc63dc4d04572ee9331b4466580463", "b4dd364c61e292fdb55f70e8d7918dcbb34800bb"),
    "cloud/remote-separation/worker/demucs_engine.py": (None, "512a281ca87911d73a0e72382233e17360532b39"),
    "cloud/remote-separation/worker/gbw_worker.py": ("0edd2d415720239ee306dc448f16adb3b40cd674", "0899fb477e4ad4979a668c06db529c8a5299ea6b"),
    "cloud/remote-separation/worker/requirements.txt": ("70692cd218d33a78cd102d40bd9456a2c6c753bf", "489d4a5d2ef6269752a907be18d01951b8249ea4"),
    "cloud/remote-separation/worker/tests/test_audio_quality.py": (None, "5f4ddbcac9b0f5f30a90da469dba686bc12430d7"),
    "cloud/remote-separation/worker/tests/test_benchmark.py": ("c88dea56ea265d5f351346660f5fcaf21fddb63e", "f249f6dc812bd83fd63a98b47b2df7b52d61b593"),
    "cloud/remote-separation/worker/tests/test_demucs_engine.py": (None, "59ff03380c3819e8071f56a7adfd915757ee5407"),
    "cloud/remote-separation/worker/tests/test_worker.py": ("c85c8a31c364839cb12343f1173c33310259d5f2", "9d4f6abf251e79a4156dee9b9c594b84a928448b"),
    "scripts/u4_cloud_integration_smoke.sh": ("6fe143beb9e1a6aa1274afe29b65ff9831b1d5d4", "02e5e22c3c31b9facf1d40d34494ae6e40e19c46"),
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
    for rel, (before, _) in TARGETS.items():
        target = ROOT / rel
        if before is None:
            if target.exists():
                return False
        elif not target.is_file() or blob(target) != before:
            return False
    return True

def verify() -> None:
    run("git", "diff", "--check")
    engine = (ROOT / "cloud/remote-separation/worker/demucs_engine.py").read_text(encoding="utf-8")
    quality = (ROOT / "cloud/remote-separation/worker/audio_quality.py").read_text(encoding="utf-8")
    policy = (ROOT / "cloud/remote-separation/functions/src/policy.ts").read_text(encoding="utf-8")
    schema = (ROOT / "cloud/remote-separation/schemas/result-manifest.schema.json").read_text(encoding="utf-8")
    smoke = (ROOT / "scripts/u4_cloud_integration_smoke.sh").read_text(encoding="utf-8")
    if 'ENGINE_NAME = "demucs-pytorch"' not in engine:
        raise SystemExit("U12ao engine semantic guard failed")
    if "SYSTEMATIC_STEM_DC" not in quality:
        raise SystemExit("U12ao W2 quality semantic guard failed")
    if 'WORKER_CONTRACT = "official-demucs-pytorch-v1"' not in policy:
        raise SystemExit("U12ao recovery contract semantic guard failed")
    if '"demucs-pytorch"' not in schema or "34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd" not in schema:
        raise SystemExit("U12ao result manifest semantic guard failed")
    if 'm["engine"]=="demucs-pytorch"' not in smoke:
        raise SystemExit("U12ao U4 smoke semantic guard failed")

if not PATCH.is_file():
    raise SystemExit("Missing U12ao official Demucs W0-W2 patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12ao patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12ao official Demucs W0-W2")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12an materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12ao baseline blob mismatch after U12an")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12ao terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12ao official Demucs W0-W2")
