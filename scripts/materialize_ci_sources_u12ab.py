#!/usr/bin/env python3
from __future__ import annotations
import base64, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12z.sh"
PATCH_B64 = ROOT / ".source-parts/U12abRemotePreparedReferencesV2Rc13.patch.b64"
PATCH_B64_BLOB = '30a638cee2b22307e7c39c796d00acaeb9a390f2'
PATCH_BLOB = '5ec83381b0e1c7650eceb39795a41ee0c530a0f6'
FILES = {
  ".github/workflows/u4-cloud-integration-smoke.yml": "982e6cbcd29b596c384e73e6cb6800adabaeab58",
  ".github/workflows/u7-cloud-backend.yml": "033810ecea1079946cb3ddf53f1c37326042b209",
  "app/build.gradle.kts": "5acfc63873b2b06c5768b594633b804e9ac4f26a",
  "app/src/androidTest/java/studio/guitarlab/app/HomeProjectLibraryInstrumentedTest.kt": "85d2ec1aa64668f1c18c74f16d08a6b2fd9add52",
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt": "4c2853a83cda7d1452059c93fc4d416cff2554e4",
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": "5fc2ac0874a7ad87a16b547d5019341ecd13294b",
  "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt": "f5e0aad8eef5ee514d30cef233cb5a3e7b564380",
  "app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt": "c1ff0e57be5a6ba5af6d477a9b9ed9b7075bcddb",
  "cloud/remote-separation/functions/src/index.ts": "f1b545dd27506aa802db06cd297c49213253cf11",
  "cloud/remote-separation/functions/src/policy.ts": "3d07560092a2a545cba2d3fc250ee56216b9428d",
  "cloud/remote-separation/functions/src/test/policy.test.ts": "0f5de3c062e757fd7a409f557fee525755f920ba",
  "cloud/remote-separation/schemas/remote-job.schema.json": "e7d90a212b9a14b3085dd8aaf0fc143f2c6075b4",
  "cloud/remote-separation/schemas/result-manifest.schema.json": "eedf7689ca8e7ae8ec58710446b1192d61800115",
  "cloud/remote-separation/scripts/bootstrap-github-actions.sh": "0c009abe03550d01ebae7bc383db6780171fba40",
  "cloud/remote-separation/scripts/deploy-functions.sh": "c1f00994e04775b7d1c6f28e13901ba0dc5bb40f",
  "cloud/remote-separation/scripts/ensure-remote-storage-lifecycle.sh": "b89b9f16e81d1a7aa29d6fdd616dcc0482abe7f0",
  "cloud/remote-separation/scripts/provision-infra.sh": "9e7deee997fdb4021fd3bcc55efb464db9d2f58d",
  "cloud/remote-separation/worker/gbw_worker.py": "53542911d0142ff95dfe8bfa459ea48a6da4bff6",
  "cloud/remote-separation/worker/tests/test_worker.py": "df932196ed7fca54e666a23366e034b0a879d576",
  "core/project/src/main/kotlin/studio/guitarlab/core/project/PreparedReferenceProjectPublisher.kt": "21fa980675b3a86810368541c125c680dde05910",
  "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectLifecyclePolicy.kt": "219dbdc6fdd4119ed86048b6e4508c6b0ed37a1a",
  "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectManagedMediaStore.kt": "b90489d26f3c173e5cbe166e70278c50b1b9ee46",
  "core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferenceProjectPublisherTest.kt": "91bef314b8f3d2a1b57a766bba35fc27acb773e3",
  "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectLifecyclePolicyTest.kt": "10c3f917692b9448318073a1f03a6a777c804ec1",
  "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt": "53ccfb3378fce778cf10a7e82ffef2089d856f00",
  "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt": "9f23c50704f5e4069faae35ff45b4d8f11a2df6c",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt": "60a9e44bd334b3c6d4b61c6825b2633f5aa87386",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/ManagedStemSetPublisher.kt": "edfa390a297b337da10e3608a0589ae063732a61",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodec.kt": "24a8e53570c6897cf45541535497758c548537dc",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteSeparationNotifier.kt": "7bd2628a67284b5862790bd5cc3166d3664a22db",
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteStemStaging.kt": "5bcba388b6f025122385dba33c7af9d439c928ca",
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodecTest.kt": "28345a2678e83d79838a0c33932bdb8da65d229e",
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteSeparationNotificationPolicyTest.kt": "0fb1ec1a5f2c943e5009c8771b007bdaf9f67d67",
  "platform/separation/src/test/kotlin/studio/guitarlab/platform/separation/RemoteStemStagingTest.kt": "b3328071ae39929b74b0d1db93fb61c2aa6aa14f",
  "scripts/bootstrap_u4_github_wif.sh": "3734fa45f98e35b89b74b1beaae566b6f49511a9",
  "scripts/u4_cloud_integration_smoke.sh": "c855f2ef8660050125185a0841ecd717c6c1f1a9"
}
CHECKS = [['app/build.gradle.kts', 'versionName = "0.5.0-rc13"', True], ['app/build.gradle.kts', 'versionCode = 33', True], ['cloud/remote-separation/worker/gbw_worker.py', 'REFERENCE_DELIVERABLES = ("backing", "guitar")', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "SEPARATING"', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "PREPARING_REFERENCES"', True], ['cloud/remote-separation/worker/gbw_worker.py', '"phase": "PUBLISHING_RESULTS"', True], ['core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt', 'expectedResultUid', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteManifestCodec.kt', 'uid = root["uid"]', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteStemStaging.kt', 'cachedManifest', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt', 'staging.cachedManifest', True], ['platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt', 'deleteTree(storage.reference.child(prefix(identity)))', False], ['scripts/u4_cloud_integration_smoke.sh', 'acknowledgeRemoteImport', True], ['scripts/u4_cloud_integration_smoke.sh', 'Firestore IMPORTED/PURGED PASS', True], ['scripts/bootstrap_u4_github_wif.sh', 'roles/iam.serviceAccountTokenCreator', True], ['cloud/remote-separation/scripts/ensure-remote-storage-lifecycle.sh', 'remote_storage_lifecycle=PASS age_days=3 prefix=remote/v1/users/', True]]


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
            raise SystemExit(f"U12ab semantic guard failed: {rel} :: {needle} :: expected={present}")


if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ab rc13 payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ab base64 payload blob mismatch")
patch = base64.b64decode(PATCH_B64.read_bytes(), validate=False)
with tempfile.NamedTemporaryFile(prefix="u12ab-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ab decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ab rc13 prepared references v2")
    else:
        if not PREVIOUS.is_file(): raise SystemExit("Missing U12z materializer")
        run("bash", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready(): raise SystemExit("U12ab final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ab rc13 prepared references v2")
finally:
    temp.unlink(missing_ok=True)
