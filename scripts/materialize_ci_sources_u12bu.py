#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bt.py"
PATCH = ROOT / ".source-parts/U12buActivityCancellationAndSettingsCleanup.patch"
PATCH_BLOB = "b8c7db311729b1b09d1a6f580342709e3ce24ff3"
TARGETS = {
    "app/src/androidTest/java/studio/guitarlab/app/SettingsVisualHierarchyInstrumentedTest.kt": ("df12a5bc6a7c4dc1718bceae439d68dcdbd0e363", "79ca916734328b65be5ec8adfc7f2174c10380a6"),
    "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt": ("a6a13fe9d70029c50c82042eb81f6d7e7b7106fd", "674947c23c3f7bd657ab31e0b4030cd962aff75e"),
    "app/src/main/java/studio/guitarlab/app/activity/ActivityCancellationCoordinator.kt": (None, "4b88b1815ed8ddfbf7b28e161e296fde44a4c727"),
    "app/src/main/java/studio/guitarlab/app/activity/ActivityCancellationRegistry.kt": (None, "1e5c187487e2a6008ad69e57281196ea961dbe5c"),
    "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt": ("04fe188258c88204e6bbe04cb74d43e88a77bbe6", "3c70e609f4037b7fa3c797b4d0a2ed7bb4725931"),
    "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityStore.kt": ("599017a95a3f0a1fcab0ca9e80c1d02385b7daea", "67483c4610c1c3d5bf8ff8897c40269886802d4c"),
    "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityViewModel.kt": ("7ef8f6a082a883d1ef54a2204fa56cb2f09d90bd", "afb692702c8a7963f5431808ffd9cb3fb7a6a57c"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupScheduler.kt": ("65c9b7d5c18d09b3baa6d354c62ec1432343e4ab", "41970084a3ae1dd08922af860c8d7dc1fd3db43e"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt": ("9766eb90fa65180581876d0111ab377c783cba67", "600ca22ceab33c8377ee7abad642e5a481f269da"),
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt": ("e3b1ba664172989e43d90f739159f665029d1da8", "ea852856665dea56da2107d21e6cd8a3aa4d5065"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": ("43a366919ddd5046324f1fd385c962fb3c8dd38b", "2445bba607b4b730dade4a796dd1c4d2abb2173c"),
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt": ("62221f7fdffe2fea3467056e6dec6da15fe6a21d", "e4c982b4039d00e412901eb0ed75a58b94b298fa"),
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt": ("4ea8243b52de42f92a5de3a250a72cf911a20a32", "515fe99cb844b0bdc32393cc7ebc6bfdf014091d"),
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceAcquisitionClient.kt": ("ddda8ea9f0b38a298f97f862214449ffc7262aab", "21690c5511c48cc50a9845ad1e4872523c734730"),
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
    return all(
        (not (ROOT / rel).exists()) if before is None
        else ((ROOT / rel).is_file() and blob(ROOT / rel) == before)
        for rel, (before, _) in TARGETS.items()
    )

def verify() -> None:
    run("git", "diff", "--check")
    settings = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt").read_text(encoding="utf-8")
    app = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt").read_text(encoding="utf-8")
    activity = (ROOT / "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt").read_text(encoding="utf-8")
    coordinator = (ROOT / "app/src/main/java/studio/guitarlab/app/activity/ActivityCancellationCoordinator.kt").read_text(encoding="utf-8")
    remote = (ROOT / "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt").read_text(encoding="utf-8")
    source = (ROOT / "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceAcquisitionClient.kt").read_text(encoding="utf-8")
    home = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt").read_text(encoding="utf-8")

    forbidden_settings = ('title = "Projeto e Studio"', 'title = "Importação"', 'title = "Ver atividade"', "settings-open-activity")
    for token in forbidden_settings:
        if token in settings:
            raise SystemExit(f"U12bu stale Settings surface: {token}")
    required = (
        (app, 'onActivity = { navigate(AppScreen.Activity()) }'),
        (activity, 'testTag("activity-cancel-${record.operationId}")'),
        (coordinator, "cancelFromActivity(projectId, record.operationId)"),
        (remote, "RemoteActivityOrphanCancelWorker"),
        (remote, 'getHttpsCallable("cancelRemoteSeparation")'),
        (source, "fun cancel(projectId: String, operationId: String): Boolean"),
        (home, "RemoteJobState.CANCEL_REQUESTED || current.state == RemoteJobState.CANCELLED"),
    )
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"U12bu semantic guard failed: {token}")
    options_start = app.index("is AppScreen.Options -> SettingsScreen(")
    options_end = app.index("is AppScreen.Diagnostics", options_start)
    if "onActivity" in app[options_start:options_end]:
        raise SystemExit("U12bu redundant Settings Activity callback remains")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bu patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bu")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bt materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bu baseline blob mismatch after U12bt")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bu terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bu")
