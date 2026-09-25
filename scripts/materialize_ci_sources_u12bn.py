#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bm.py"
PATCH = ROOT / ".source-parts/U12bnRc20AndroidCompletion.patch"
PATCH_BLOB = "f5da6f8e24f2ebdc7160c1c1d19ce40f302cd166"
TARGETS = {
    "app/build.gradle.kts": ("e311e4197526f28ec0c1197f7574f200eb29b8bf", "2e89ebca930e7b7f960f746eb36adf24fb2bddda"),
    "app/src/androidTest/java/studio/guitarlab/app/DiagnosticSupportInstrumentedTest.kt": (None, "18b77a083a20bbb53fc23fcb30ca2134ed32d7c0"),
    "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt": ("be267ff6c2839f4a713a604187667681553774ca", "f355326a2f8d71313f77d36afd5a5873f7d4ef75"),
    "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityStore.kt": ("96ff4ae7db7f256cced701a54b9ca3c5df772b6a", "599017a95a3f0a1fcab0ca9e80c1d02385b7daea"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt": (None, "77f77f3a62a0c7015583f3ea6e1a68805c899579"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticJournal.kt": (None, "5ff477fce53a31b82bc85164f8ccc0781608ac73"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt": (None, "30e51cb0500e867d0765c5209c4033ab69478219"),
    "app/src/main/java/studio/guitarlab/app/ui/AppScreen.kt": ("f377e80b93fd7316f66418e23d8c8b134739b6ce", "187fd60b0ce5cbe988d2d5f8dba5f17fc1adf812"),
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt": ("8155b33ae2f45718ec68365469ea9e63b2a33859", "e3b1ba664172989e43d90f739159f665029d1da8"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": ("f5e59582e75ff917d9f920f51932ccc19414ddc4", "43a366919ddd5046324f1fd385c962fb3c8dd38b"),
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt": ("1b5f27f5bafc1f5a3b1a38400a71b716834b6787", "62221f7fdffe2fea3467056e6dec6da15fe6a21d"),
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt": ("cf0e61914d147c418e6aa198aa2b64bcbde367f9", "dda46082508bd4f1991290c78f6379c0422793f6"),
    "app/src/test/java/studio/guitarlab/app/ui/AppRouteCodecTest.kt": ("939e7ad62c70e57e295ba3b165e355e2351c11cb", "a9d8ff97295da271f1b8c3b0014bfc2aa417df80"),
    "core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt": ("7b026fa974024e658f00e8e4ee161ddb9095e829", "8218ade3391a7705c7b9a8390f1ca0a3a2ded863"),
    "core/separation/src/test/kotlin/studio/guitarlab/core/separation/RemoteSeparationTest.kt": ("0a7294bd3553806509d72e3e247ed118e5e485e9", "db5c29a58673a35af7d8afabdf7d1b16a0aea881"),
    "core/source/src/main/kotlin/studio/guitarlab/core/source/SourceSearch.kt": ("1bf289578cd9e1f16e78024b79e8255e2080d9b9", "cbc89a79b14a098dc5e8e982dcf132419e9a5499"),
    "core/source/src/test/kotlin/studio/guitarlab/core/source/SourceSearchRulesTest.kt": ("13c879a13d81a68b36c99fe54b3c607a4b47f2c4", "e22c063703cac33db9f3ed9f99f42d0579be0d42"),
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/AcceptedRemoteManifestStore.kt": (None, "52fe29477d55fbc69082ab8a833acb12bbb2d0c0"),
    "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt": ("245a354c7217f9c854fae96e6902fd00d64581e0", "4ea8243b52de42f92a5de3a250a72cf911a20a32"),
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceDiscovery.kt": ("8d41e7381f9d7ece0b8e806aca64355659265f32", "7fa717665b36e8004e273c700272c1717e2980b0"),
    "platform/source-android/src/test/kotlin/studio/guitarlab/platform/source/android/SourceDiscoveryTest.kt": ("995c908fd191e7ea881b4f38b21a6a3101828b58", "4f00bbf231cd9092be9c83860991691070e96fde"),
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
        ((before is None and not (ROOT / rel).exists()) or
         (before is not None and (ROOT / rel).is_file() and blob(ROOT / rel) == before))
        for rel, (before, _) in TARGETS.items()
    )


def verify() -> None:
    run("git", "diff", "--check")
    workflow_tokens = [
        ("app/build.gradle.kts", 'versionCode = 40'),
        ("app/build.gradle.kts", 'versionName = "0.5.0-rc20"'),
        ("app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt", "SourceSearchTerminalState"),
        ("app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt", "repairPreparedReferenceBindings"),
        ("app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticJournal.kt", "MAX_BYTES = 8 * 1024 * 1024"),
        ("app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt", "integrity/SHA256SUMS.txt"),
        ("app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt", "Exportar pacote de diagnóstico"),
        ("core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt", "persistAcceptedManifest"),
        ("platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/AcceptedRemoteManifestStore.kt", "_acceptedAtEpochMs"),
    ]
    for rel, token in workflow_tokens:
        if token not in (ROOT / rel).read_text(encoding="utf-8"):
            raise SystemExit(f"U12bn semantic guard failed: {rel}: {token}")
    run("bash", "-n", str(ROOT / "scripts/materialize_ci_sources.sh"))


if not PATCH.is_file():
    raise SystemExit("Missing U12bn RC20 Android completion patch")
if blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bn patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bn RC20 Android completion")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bm materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bn baseline blob mismatch after U12bm")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bn terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bn RC20 Android completion")
