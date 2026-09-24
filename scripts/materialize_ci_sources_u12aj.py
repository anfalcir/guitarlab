#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ai.py"
PATCH = ROOT / ".source-parts/U12ajRemoteSameGenerationRecovery.patch"
PATCH_BLOB = "4c9b0366cba392a8de4e9913f9076be840c09e87"
AFTER = {
    "app/build.gradle.kts": "e311e4197526f28ec0c1197f7574f200eb29b8bf",
    "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt": "dffd7e4835506294ecef8d8b249ca4191e5ed43c",
    "app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt": "4fce7dc86c97acd4ea78370825b20a44f44f72cb",
    "cloud/remote-separation/functions/src/index.ts": "4d279551ae78728354624e93a2c5c64891d572d4",
    "cloud/remote-separation/functions/src/policy.ts": "d1140ef30fbaba7f111e7bf5f69161a0c35d93d1",
    "cloud/remote-separation/functions/src/test/policy.test.ts": "8b344471ea30051da63fb73039b775be459c4f03",
    "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt": "c72c45865fed557e0f9e59446875905e0e7066ee",
    "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt": "dfa24747a089f0e213e0994faea8e132a5a8241c",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt": "55fc39ec468fd6bfd4f43ebb8fb00c88a39b5c93",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteBackend.kt": "645dbf0a747ecd8d08cea92bc28b52a7fe5008a3",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt": "245a354c7217f9c854fae96e6902fd00d64581e0",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt": "f3feecd36e452999a196efe498234af402409558",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicy.kt": "b15c30f01800b4736b9747fb7775a0dc4e50f078",
    "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifierTest.kt": "bcd19499510382f0bcd1d2369f05aa4268cd8442",
    "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteRecoveryPolicyTest.kt": "8177e10913dc5e9fea2e1fa350e3a9f94998bc97",
    "scripts/u4_cloud_integration_smoke.sh": "358785d3c2c97b94c9373ecd20bf987120d501ec"
}


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))


def ready() -> bool:
    return all((ROOT / path).is_file() and blob(ROOT / path) == expected for path, expected in AFTER.items())


def verify_semantics() -> None:
    run("git", "diff", "--check")
    checks = (
        ("app/build.gradle.kts", 'versionName = "0.5.0-rc19"'),
        ("app/build.gradle.kts", "versionCode = 39"),
        ("cloud/remote-separation/functions/src/index.ts", "findRecoverableRemoteSeparation"),
        ("cloud/remote-separation/functions/src/index.ts", "EXISTING_SAME_GENERATION"),
        ("cloud/remote-separation/functions/src/index.ts", "cleanupUnregisteredUpload"),
        ("cloud/remote-separation/functions/src/policy.ts", "ACTIVE_JOB_CONFLICT"),
        ("cloud/remote-separation/functions/src/policy.ts", "MONTHLY_QUOTA_REACHED"),
        ("core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt", "RemoteEnqueueResult"),
        ("core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt", "RECOVERY_MARKER"),
        ("platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt", "override fun adopt"),
        ("platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt", "RemoteRecoveryProbeWorker"),
        ("app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt", "O limite mensal de separações em nuvem foi atingido."),
        ("scripts/u4_cloud_integration_smoke.sh", "U4 same-generation recovery PASS"),
    )
    for rel, needle in checks:
        if needle not in (ROOT / rel).read_text(encoding="utf-8"):
            raise SystemExit(f"U12aj semantic guard failed: {rel}: {needle}")


if not PATCH.is_file():
    raise SystemExit("Missing U12aj remote recovery patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12aj patch blob mismatch")

if ready():
    verify_semantics()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12aj remote same-generation recovery")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12ai materializer")
run("python3", str(PREVIOUS))
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
run("git", "diff", "--check")
if not ready():
    raise SystemExit("U12aj terminal blob mismatch")
verify_semantics()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12aj remote same-generation recovery")
