#!/usr/bin/env python3
from __future__ import annotations
import base64, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ab.py"
PATCH_B64 = ROOT / ".source-parts/U12acRc13QualificationCorrectives.patch.b64"
PATCH_B64_BLOB = '024fd08a353e9313593a4ae1326061fcbae06b03'
PATCH_BLOB = 'e1789b9eb235d676362a494b121d1dcf0816c3a4'
FILES = {
  "cloud/remote-separation/scripts/bootstrap-github-actions.sh": "7c5e25a5135e2fdd2bd8718bd4d45a10ff2df70e",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/ManagedStemSetPublisher.kt": "01aea18eecef89928e8874b709fb932e76c6cedf",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt": "dc6f4472ac68ba2de8be10a4051448b0abe18a29"
}
CHECKS = [['app/build.gradle.kts', 'versionName = "0.5.0-rc13"', True], ['app/build.gradle.kts', 'versionCode = 33', True], ['cloud/remote-separation/worker/gbw_worker.py', 'REFERENCE_DELIVERABLES = ("backing", "guitar")', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "SEPARATING"', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "PREPARING_REFERENCES"', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "PUBLISHING_RESULTS"', True], ['core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt', 'expectedResultUid', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodec.kt', 'uid = root["uid"]', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteStemStaging.kt', 'cachedManifest', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt', 'staging.cachedManifest', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt', 'deleteTree(storage.reference.child(prefix(identity)))', False], ['scripts/u4_cloud_integration_smoke.sh', 'acknowledgeRemoteImport', True], ['scripts/u4_cloud_integration_smoke.sh', 'Firestore IMPORTED/PURGED PASS', True], ['scripts/bootstrap_u4_github_wif.sh', 'roles/iam.serviceAccountTokenCreator', True], ['cloud/remote-separation/scripts/ensure-remote-storage-lifecycle.sh', 'remote_storage_lifecycle=PASS age_days=3 prefix=remote/v1/users/', True], ['cloud/remote-separation/scripts/bootstrap-github-actions.sh', 'roles/cloudscheduler.admin', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt', 'stage == RemotePipelineStage.DOWNLOADING_RESULTS', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/ManagedStemSetPublisher.kt', 'skipFully(input, dataBytes)', True]]


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def ready() -> bool:
    for rel, expected in FILES.items():
        path = ROOT / rel
        if expected == "__MISSING__":
            if path.exists(): return False
        elif not path.is_file() or out("git", "hash-object", str(path)) != expected:
            return False
    return True


def verify_semantics() -> None:
    run("git", "diff", "--check")
    for rel, needle, present in CHECKS:
        text = (ROOT / rel).read_text(errors="replace")
        if (needle in text) != present:
            raise SystemExit(f"U12ac semantic guard failed: {rel} :: {needle} :: expected={present}")


if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ac rc13 payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ac base64 payload blob mismatch")
patch = base64.b64decode(PATCH_B64.read_bytes(), validate=False)
with tempfile.NamedTemporaryFile(prefix="u12ac-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ac decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ac rc13 prepared references v2")
    else:
        if not PREVIOUS.is_file(): raise SystemExit("Missing U12z materializer")
        run("python3", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready(): raise SystemExit("U12ac final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ac rc13 prepared references v2")
finally:
    temp.unlink(missing_ok=True)
