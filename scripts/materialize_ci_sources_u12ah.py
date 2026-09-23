#!/usr/bin/env python3
from __future__ import annotations

import base64
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12ag.py"
PATCH_B64 = ROOT / ".source-parts/U12ahRc14Corrections.patch.b64"
PATCH_B64_BLOB = "867b534c0ab2791a9188dd851fddf9b6767e2d05"
PATCH_BLOB = "3eb5c43e3811c5d6592a08d169590b1985cc6925"
FILES = {
    "README.md": "ea4b8090432f529b79bb070f3365b19e10391c35",
    "app/build.gradle.kts": "fec3e67d4314d9a983b36c8d19ab8417eccaa4e8",
    "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt": "bc29a07f014aa8e2a4fe5a4dc9d98d2ce598b46c",
    "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt": "9e299aae9e80cb7e4d15c09688aea578a1ea8269",
    "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt": "7c9f128b42f1546cc84db277f594eef767da17d6",
    "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt": "9766eb90fa65180581876d0111ab377c783cba67",
    "app/src/main/java/studio/guitarlab/app/backup/DeletedProjectBackupStore.kt": "5bcc5c488f08522af98ffb5ccba5cd67b3d2ca85",
    "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt": "5ac06fad4bac40074c39d7051232ca41481619ba",
    "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt": "936b0b5069c5cc2eeb41f6993ea28cf344a9e5e3",
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": "f5e59582e75ff917d9f920f51932ccc19414ddc4",
    "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt": "ac2990aed12e000e9976735d6641d986f9c77b92",
    "app/src/main/res/drawable/ic_notification_guitarlab.xml": "3a30e17f6fbe3cd61d6c8d2ccaa11b7a1c4ec972",
    "app/src/test/java/studio/guitarlab/app/backup/DeletedProjectRetentionPolicyTest.kt": "243798301083f7e9ea181beba578189b16c76730",
    "docs/ARCHITECTURE.md": "edc63e40bc5c8459890bbef1da1bd8435188ba82",
    "docs/CI_PIPELINE.md": "f7629c99cc07c9021f5661e15b73c623e5b6c968",
    "docs/CURRENT_STATE.md": "ccfad22c75df91e80ace4ef519397c15a54eae7a",
    "docs/DOCUMENTATION_MAP.md": "7eb392ae293246a915a91adf9288cac57cbda1e3",
    "docs/IMPLEMENTATION_ROADMAP.md": "2f0fc85b6593b710e993e0ef02fa4eafc5808005",
    "docs/RC14_FINAL_PHYSICAL_HOMOLOGATION.md": "07d280ef6df2c389492eafe81b59a30ac027a9eb",
    "docs/RELEASE_NOTES_0.5.0-rc14.md": "e008cc38f9e7181ce1778b43b48259483815865d",
    "docs/TEST_AND_HOMOLOGATION_PLAN.md": "b95d7d2a47ebd5ba0bd70486d3de34ef46edfa13",
    "docs/U12_REMOTE_REFERENCE_V2_HARDENING_PLAN.md": "4f32994954e04860c07c69d1126c66f09adaa061",
    "platform/separation/src/main/AndroidManifest.xml": "e6aedf9b22fdc8c75df1bd2bf3a57734702a1e4a",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt": "e502c4101260a3a79ebd47e2cb30a7ad157157b9",
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/RemoteSeparationNotifier.kt": "a2be66bb3540636035da44d118cf268f9229fb4a",
    "platform/separation/src/main/res/drawable/ic_notification_guitarlab.xml": "3a30e17f6fbe3cd61d6c8d2ccaa11b7a1c4ec972",
}

def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def ready() -> bool:
    return all((ROOT / rel).is_file() and out("git", "hash-object", str(ROOT / rel)) == expected for rel, expected in FILES.items())

def verify_semantics() -> None:
    run("git", "diff", "--check")
    checks = {
        "app/build.gradle.kts": ('versionName = "0.5.0-rc14"', "versionCode = 34"),
        "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt": ("setForeground(", "notifier.foregroundInfo("),
        "platform/separation/src/main/AndroidManifest.xml": ('android:foregroundServiceType="dataSync"', "FOREGROUND_SERVICE_DATA_SYNC"),
        "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt": ("Gerenciar takes e sincronização…",),
        "app/src/main/java/studio/guitarlab/app/backup/DeletedProjectBackupStore.kt": ("RETENTION_DAYS = 10L", "Mere absence"),
        "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt": ("suspend fun deleteProject", "ownedHashes - protectedHashes"),
    }
    for rel, needles in checks.items():
        text = (ROOT / rel).read_text(errors="replace")
        for needle in needles:
            if needle not in text:
                raise SystemExit(f"U12ah semantic guard failed: {rel} :: {needle}")

if not PATCH_B64.is_file():
    raise SystemExit("Missing U12ah RC14 payload")
if out("git", "hash-object", str(PATCH_B64)) != PATCH_B64_BLOB:
    raise SystemExit("U12ah base64 payload blob mismatch")
try:
    patch = base64.b64decode(b"".join(PATCH_B64.read_bytes().split()), validate=True)
except Exception as exc:
    raise SystemExit(f"U12ah base64 payload is invalid: {exc}") from exc
with tempfile.NamedTemporaryFile(prefix="u12ah-check-", suffix=".patch", delete=False) as handle:
    handle.write(patch)
    temp = Path(handle.name)
try:
    if out("git", "hash-object", str(temp)) != PATCH_BLOB:
        raise SystemExit("U12ah decoded patch blob mismatch")
    if ready():
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain already materialized through U12ah RC14 corrections")
    else:
        if not PREVIOUS.is_file():
            raise SystemExit("Missing U12ag materializer")
        run("python3", str(PREVIOUS))
        run("git", "apply", "--check", str(temp))
        run("git", "apply", str(temp))
        run("git", "diff", "--check")
        if not ready():
            raise SystemExit("U12ah final blob mismatch")
        verify_semantics()
        run("git", "apply", "--check", "--reverse", str(temp))
        print("Source patch chain materialized through U12ah RC14 corrections")
finally:
    temp.unlink(missing_ok=True)
