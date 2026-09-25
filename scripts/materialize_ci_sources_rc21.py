#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bx.py"
PATCH = ROOT / ".source-parts/RC21IntegrityFeedbackBackup.patch"
PATCH_BLOB = "cd9702e544701e5ab721bb53b02397bd6ddffb14"
TARGETS = {
    "app/build.gradle.kts": ("2e89ebca930e7b7f960f746eb36adf24fb2bddda", "076c5e76779c30a2a78df6edbbf5f849950134f8"),
    "app/src/androidTest/java/studio/guitarlab/app/BackupCatalogCacheInstrumentedTest.kt": (None, "6355ef50077dcf0d6eac51f82339f71b6dabcd38"),
    "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt": ("67b200b6be911830628158f73d9475f150689951", "e80c6a95107bfa6ab5705269369a33170304de50"),
    "app/src/androidTest/java/studio/guitarlab/app/TransientFeedbackHostInstrumentedTest.kt": (None, "5a229cc263868a846d8e94f839b4cf173473aefb"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupCatalogCacheStore.kt": (None, "7de3a96919b5ad73dede826f5e1fa57de3631d8b"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt": ("7c9f128b42f1546cc84db277f594eef767da17d6", "0857ecbd7f12e4827b48d03b1eaf6197aa1bea3a"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt": ("600ca22ceab33c8377ee7abad642e5a481f269da", "3afcc48f5c91dee4284e0873a6113a64129c3315"),
    "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt": ("5ac06fad4bac40074c39d7051232ca41481619ba", "699ac96c4067a7f0c6facec2cd6d2e02f4f6c7e2"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt": ("2365960949f75f039ec6f8b5ee858469390847b2", "a0bb842418119c781c7f8832219d037d94e68490"),
    "app/src/main/java/studio/guitarlab/app/ui/AppTransientFeedbackHost.kt": (None, "525ed160fb078d09ab22a4bdcbc25cee18a0deaa"),
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt": ("d17cf271f3d88b6b516e159d3c27fc187b83c27d", "6c19689e5c9a15c88c2784ea01bc82d7be9908ea"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt": ("fb798328dd823f0f540856d6fcf9275e569aa8af", "7a2e9135705807c71c3781116c2be450a1873af8"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": ("a7982d01296f6c61c05a860798e239279e9c5d80", "74cece9a4a00035b3b76810b3fab7935efe8f13a"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ("6a9ab339ea86e4ccbe5f44b682ea9d5c2e551d17", "6cd72c76501aef0b3cabc5b5c9f6f3c6dfb1aa3e"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt": ("02c53da6409543e57e77be4522b2ea3e6a856fb3", "1c9e472debdf654de1b4b377ce3611c152812886"),
    "app/src/test/java/studio/guitarlab/app/backup/BackupCatalogFreshnessPolicyTest.kt": (None, "73fec5f6d48afa46a7558c2de448207452c792fc"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/LegacyRecordingTakeRecoveryPolicy.kt": (None, "2ff6f0c579e931e3f368bf6b2cda51df6d4118c5"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectCodec.kt": ("b5800e0f98b019cf89082c1ccf02a6a2aa9878d6", "b7d7a31b94f95a70f0856fc28383204aae30dda4"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/StereoSeparationProjectPolicy.kt": (None, "68ba6691e66aed52c910d9333a7f2794a84d003d"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveProductionStorage.kt": ("1baf6a31cdcedadcd148658bd307785754e5b18b", "fe9024752d5944baba29d0c927f8a92f2ed9fac3"),
    "core/project/src/test/kotlin/studio/guitarlab/core/project/LegacyRecordingStereoMaintenanceTest.kt": (None, "46590131058f86981b196d8cab95e2f12d4e347f"),
    "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveProductionStorageTest.kt": ("41f958e91e13857bc7e3b7a4ec7eaef9371da450", "34d2df5ea6c7535c177c5484942b78d6ac00514a"),
    "scripts/ci_run_api36_regression_groups.sh": ("6cf04756a6eee8d23097e059a32eaf60be9874ff", "9fd83e5a8124b42fa2febe5376748c3bd09cc5ea"),
}


def out(*args: str) -> str:
    return subprocess.check_output(list(args), cwd=ROOT, text=True).strip()


def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)


def blob(path: Path) -> str:
    return out("git", "hash-object", str(path))


def ready() -> bool:
    return all(
        (ROOT / rel).is_file() and blob(ROOT / rel) == after
        for rel, (_, after) in TARGETS.items()
    )


def baseline_ready() -> bool:
    return all(
        (not (ROOT / rel).exists())
        if before is None
        else ((ROOT / rel).is_file() and blob(ROOT / rel) == before)
        for rel, (before, _) in TARGETS.items()
    )


def verify() -> None:
    run("git", "diff", "--check")

    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    codec = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectCodec.kt").read_text(encoding="utf-8")
    recovery = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/LegacyRecordingTakeRecoveryPolicy.kt").read_text(encoding="utf-8")
    stereo_policy = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/StereoSeparationProjectPolicy.kt").read_text(encoding="utf-8")
    drive_restore = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveProductionStorage.kt").read_text(encoding="utf-8")
    studio = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt").read_text(encoding="utf-8")
    app = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt").read_text(encoding="utf-8")
    home = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt").read_text(encoding="utf-8")
    feedback = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/AppTransientFeedbackHost.kt").read_text(encoding="utf-8")
    diagnostics = (ROOT / "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt").read_text(encoding="utf-8")
    backup_vm = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt").read_text(encoding="utf-8")
    backup_screen = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt").read_text(encoding="utf-8")
    backup_service = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt").read_text(encoding="utf-8")
    backup_cache = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/BackupCatalogCacheStore.kt").read_text(encoding="utf-8")
    legacy_test = (ROOT / "core/project/src/test/kotlin/studio/guitarlab/core/project/LegacyRecordingStereoMaintenanceTest.kt").read_text(encoding="utf-8")
    drive_test = (ROOT / "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveProductionStorageTest.kt").read_text(encoding="utf-8")
    feedback_test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/TransientFeedbackHostInstrumentedTest.kt").read_text(encoding="utf-8")
    cache_test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/BackupCatalogCacheInstrumentedTest.kt").read_text(encoding="utf-8")
    api36_groups = (ROOT / "scripts/ci_run_api36_regression_groups.sh").read_text(encoding="utf-8")

    required = (
        (build, 'versionName = "0.5.0-rc21"'),
        (build, "versionCode = 41"),
        (codec, "fun decodePersistedState(serialized: String): GuitarProject"),
        (codec, "LegacyRecordingTakeRecoveryPolicy.recover(decodePersistedState(serialized))"),
        (recovery, 'Regex("""^(.+)-take-(\\d{10,})\\.wav$"""'),
        (recovery, 'if (clip.sourceUri != "managed://$path") return null'),
        (stereo_policy, "editingTotalFrames = splitTotalFrames"),
        (stereo_policy, "Separe canais apenas antes de dividir temporalmente uma take."),
        (studio, "val split = StereoWavChannelSplitter.split(editingFile, leftTemp, rightTemp)"),
        (studio, "splitTotalFrames = split.totalFrames"),
        (studio, "mediaStore.discardUncommitted(project.id, it)"),
        (drive_restore, "val persistedProject = codec.decodePersistedState(serialized)"),
        (drive_restore, "UnifiedProjectRevision.sha256(persistedProject)"),
        (app, "AppTransientFeedbackHost("),
        (feedback, "onConsumed()"),
        (feedback, "TransientFeedbackKind.ERROR"),
        (feedback, "SnackbarDuration.Long"),
        (diagnostics, "catch (cancelled: CancellationException)"),
        (diagnostics, "Pacote exportado com"),
        (backup_vm, "fun onScreenEntered()"),
        (backup_vm, "refreshInternal(forceRemote = true)"),
        (backup_vm, "knownManifestCreatedAtEpochMs = cached?.manifestCreatedAtEpochMs.orEmpty()"),
        (backup_vm, "messageKind = TransientFeedbackKind.OPERATIONAL_STATUS"),
        (backup_screen, 'testTag("backup-catalog-loading")'),
        (backup_screen, "Carregando histórico do Google Drive…"),
        (backup_screen, 'testTag("backup-catalog-background-refresh")'),
        (backup_service, "val heads = remote.listAllHeads()"),
        (backup_service, "knownManifestCreatedAtEpochMs: Map<String, Long> = emptyMap()"),
        (backup_service, "val persistedProject = codec.decodePersistedState(serialized)"),
        (backup_cache, "const val SCHEMA = 3"),
        (backup_cache, "currentLocalRevisions: Map<String, String>? = null"),
        (backup_cache, "takeIf(::isValidSnapshot)"),
        (legacy_test, "8_790_012L"),
        (legacy_test, "legacyRecoveryIsIdempotent"),
        (legacy_test, "recoveredLegacyTakeSupportsReversiblePerTakeSynchronization"),
        (drive_test, "legacyRecordingRestoreVerifiesPersistedDigestBeforeTakeRecovery"),
        (feedback_test, "asyncCompletionIsConsumedImmediatelyAndShownOnce"),
        (cache_test, "corruptedLocalCacheIsDiscardedInsteadOfBecomingCatalogTruth"),
        (cache_test, "org.junit.Assert.assertEquals"),
        (cache_test, "org.junit.Assert.assertNull"),
        (api36_groups, "BackupCatalogCacheInstrumentedTest"),
        (api36_groups, "TransientFeedbackHostInstrumentedTest"),
    )
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"RC21 semantic guard failed: {token}")

    forbidden = (
        (studio, "editingTotalFrames = sourceClip.editingTotalFrames ?: sourceClip.lengthFrames"),
        (home, "showSnackbar("),
        (home, "SnackbarHostState"),
        (diagnostics, "runCatching {\n                withContext(Dispatchers.IO)"),
    )
    for text_value, token in forbidden:
        if token in text_value:
            raise SystemExit(f"RC21 stale implementation remains: {token}")


if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("RC21 patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through RC21 maintenance")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bx materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC21 baseline blob mismatch after U12bx")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("RC21 terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through RC21 maintenance")
