#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bx.py"
PATCH = ROOT / ".source-parts/RC21IntegrityFeedbackBackup.patch"
PATCH_BLOB = "098bb5f820f2000d8aec7f6ee15c3199cacc1354"
TARGETS = {
    "app/build.gradle.kts": ("2e89ebca930e7b7f960f746eb36adf24fb2bddda", "076c5e76779c30a2a78df6edbbf5f849950134f8"),
    "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt": ("67b200b6be911830628158f73d9475f150689951", "2f9c4fd53196ecfa1f9853c2eaae510c91136139"),
    "app/src/androidTest/java/studio/guitarlab/app/TransientFeedbackHostInstrumentedTest.kt": (None, "5a229cc263868a846d8e94f839b4cf173473aefb"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupCatalogCacheStore.kt": (None, "34183e1e7137994b37a6c1c5fc113679d1e43650"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt": ("7c9f128b42f1546cc84db277f594eef767da17d6", "5a29e0ef2c322ba9bdcd89c1d8d5158b73f150d3"),
    "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt": ("600ca22ceab33c8377ee7abad642e5a481f269da", "5b0a8537a24d1e710a28306df3819d8c262b870f"),
    "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt": ("5ac06fad4bac40074c39d7051232ca41481619ba", "0adf3272a98463a5fc3501f511f17d7873bbe423"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt": ("2365960949f75f039ec6f8b5ee858469390847b2", "58fb7f2dfa88e030e4e7adcc0e291a9ec25ce730"),
    "app/src/main/java/studio/guitarlab/app/ui/AppTransientFeedbackHost.kt": (None, "f6cea97e8de9e4221f50d81f3d205434d160e865"),
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt": ("d17cf271f3d88b6b516e159d3c27fc187b83c27d", "6c19689e5c9a15c88c2784ea01bc82d7be9908ea"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt": ("fb798328dd823f0f540856d6fcf9275e569aa8af", "7a2e9135705807c71c3781116c2be450a1873af8"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": ("a7982d01296f6c61c05a860798e239279e9c5d80", "58008009eb84e0f9930e7e9830a0ce8a55af7e36"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ("6a9ab339ea86e4ccbe5f44b682ea9d5c2e551d17", "6cd72c76501aef0b3cabc5b5c9f6f3c6dfb1aa3e"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt": ("02c53da6409543e57e77be4522b2ea3e6a856fb3", "47536fb89bd282826cd3791b7c730b5e9cf127bc"),
    "app/src/test/java/studio/guitarlab/app/backup/BackupCatalogFreshnessPolicyTest.kt": (None, "2e12fa6c3bd4fa1cb77885e992c4d312a54ce749"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/LegacyRecordingTakeRecoveryPolicy.kt": (None, "2ff6f0c579e931e3f368bf6b2cda51df6d4118c5"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectCodec.kt": ("b5800e0f98b019cf89082c1ccf02a6a2aa9878d6", "90f2e3e434c18e5936dfca8050f50987169210d6"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/StereoSeparationProjectPolicy.kt": (None, "68ba6691e66aed52c910d9333a7f2794a84d003d"),
    "core/project/src/test/kotlin/studio/guitarlab/core/project/LegacyRecordingStereoMaintenanceTest.kt": (None, "05da30137470a05d11b0044b59c66c8232d06c86"),
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

    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    codec = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectCodec.kt").read_text(encoding="utf-8")
    recovery = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/LegacyRecordingTakeRecoveryPolicy.kt").read_text(encoding="utf-8")
    stereo_policy = (ROOT / "core/project/src/main/kotlin/studio/guitarlab/core/project/StereoSeparationProjectPolicy.kt").read_text(encoding="utf-8")
    studio = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt").read_text(encoding="utf-8")
    app = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt").read_text(encoding="utf-8")
    home = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt").read_text(encoding="utf-8")
    feedback = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/AppTransientFeedbackHost.kt").read_text(encoding="utf-8")
    diagnostics = (ROOT / "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt").read_text(encoding="utf-8")
    backup_vm = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt").read_text(encoding="utf-8")
    backup_service = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt").read_text(encoding="utf-8")
    backup_cache = (ROOT / "app/src/main/java/studio/guitarlab/app/backup/BackupCatalogCacheStore.kt").read_text(encoding="utf-8")
    regression = (ROOT / "core/project/src/test/kotlin/studio/guitarlab/core/project/LegacyRecordingStereoMaintenanceTest.kt").read_text(encoding="utf-8")
    feedback_test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/TransientFeedbackHostInstrumentedTest.kt").read_text(encoding="utf-8")

    required = (
        (build, 'versionName = "0.5.0-rc21"'),
        (build, "versionCode = 41"),
        (codec, "LegacyRecordingTakeRecoveryPolicy.recover(upgraded)"),
        (recovery, 'Regex("""^(.+)-take-(\\d{10,})\\.wav$"""'),
        (recovery, 'if (clip.sourceUri != "managed://$path") return null'),
        (stereo_policy, "editingTotalFrames = splitTotalFrames"),
        (stereo_policy, "Separe canais apenas antes de dividir temporalmente uma take."),
        (studio, "val split = StereoWavChannelSplitter.split(editingFile, leftTemp, rightTemp)"),
        (studio, "splitTotalFrames = split.totalFrames"),
        (app, "AppTransientFeedbackHost("),
        (feedback, "onConsumed()"),
        (feedback, "AppTransientFeedbackPolicy.shouldShowSnackbar(kind)"),
        (diagnostics, "Pacote exportado com"),
        (diagnostics, "TransientFeedbackKind.ASYNC_COMPLETION"),
        (backup_vm, "refreshInternal(forceRemote = false)"),
        (backup_vm, "knownVersions = cached?.versions.orEmpty()"),
        (backup_service, "remote.listAllHeads()"),
        (backup_service, "knownVersions: List<BackupVersionDescriptor> = emptyList()"),
        (backup_cache, "DEFAULT_MAX_AGE_MS: Long = 2L * 60L * 1_000L"),
        (regression, "8_790_012L"),
        (regression, "trimmedLegacyStereoRecordingSeparatesWithoutTrimBoundsAndKeepsValidTakeLineage"),
        (feedback_test, "asyncCompletionIsConsumedImmediatelyAndShownOnce"),
    )
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"RC21 semantic guard failed: {token}")

    forbidden = (
        (studio, "editingTotalFrames = sourceClip.editingTotalFrames ?: sourceClip.lengthFrames"),
        (home, "showSnackbar("),
        (home, "SnackbarHostState"),
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
