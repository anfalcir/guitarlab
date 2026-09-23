#!/usr/bin/env python3
from __future__ import annotations
import base64, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ac.py"
PATCH_B64 = ROOT / ".source-parts/U12adPublisherFixtureCorrective.patch.b64"
PATCH_B64_BLOB = '5e583c6c189b749e707335670f1d209f0bfdb072'
PATCH_BLOB = '8058e859bf482fe8c7fe35f2925d4537c891fd61'
FILES = {
  "core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferenceProjectPublisherTest.kt": "1f1fbf37bb3970691f6c135633ba4fc75e7742f8"
}
CHECKS = [['app/build.gradle.kts', 'versionName = "0.5.0-rc13"', True], ['app/build.gradle.kts', 'versionCode = 33', True], ['cloud/remote-separation/worker/gbw_worker.py', 'REFERENCE_DELIVERABLES = ("backing", "guitar")', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "SEPARATING"', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "PREPARING_REFERENCES"', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "PUBLISHING_RESULTS"', True], ['core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt', 'expectedResultUid', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodec.kt', 'uid = root["uid"]', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteStemStaging.kt', 'cachedManifest', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt', 'staging.cachedManifest', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt', 'deleteTree(storage.reference.child(prefix(identity)))', False], ['scripts/u4_cloud_integration_smoke.sh', 'acknowledgeRemoteImport', True], ['scripts/u4_cloud_integration_smoke.sh', 'Firestore IMPORTED/PURGED PASS', True], ['scripts/bootstrap_u4_github_wif.sh', 'roles/iam.serviceAccountTokenCreator', True], ['cloud/remote-separation/scripts/ensure-remote-storage-lifecycle.sh', 'remote_storage_lifecycle=PASS age_days=3 prefix=remote/v1/users/', True], ['cloud/remote-separation/scripts/bootstrap-github-actions.sh', 'roles/cloudscheduler.admin', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteFirebaseFailureClassifier.kt', 'stage == RemotePipelineStage.DOWNLOADING_RESULTS', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/ManagedStemSetPublisher.kt', 'skipFully(input, dataBytes)', True], ['core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferenceProjectPublisherTest.kt', 'if (templateId == 1) "p" else "template-$templateId"', True]]


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
            raise SystemExit(f"U12ad semantic guard failed: {rel} :: {needle} :: expected={present}")


if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ad rc13 payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ad base64 payload blob mismatch")
patch = base64.b64decode(PATCH_B64.read_bytes(), validate=False)
with tempfile.NamedTemporaryFile(prefix="u12ad-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ad decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ad rc13 prepared references v2")
    else:
        if not PREVIOUS.is_file(): raise SystemExit("Missing U12z materializer")
        run("python3", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready(): raise SystemExit("U12ad final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ad rc13 prepared references v2")
finally:
    temp.unlink(missing_ok=True)
