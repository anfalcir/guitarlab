#!/usr/bin/env python3
"""RC29: bounded CUE physical-route settlement before independent clock qualification."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc28a.py"
PATCHES = [
    (".source-parts/RC29CueRouteSettlementRuntime.patch", "388992c289dcdc9be0427a9dba15d3c6fe4d384c"),
    (".source-parts/RC29CueRouteSettlementDocs.patch", "27217152dbef7cf2279cf9f91ff67c546d63f4b1"),
]
TARGETS = {
    "app/build.gradle.kts": ("055047c598ba87ed7b01c7801c5bf35cadbdaae4", "9a7b09fb97418550f98ecd2f290e98b266c316a0"),
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/CueStartupProbe.kt": ("b62cf9ddff383bb1775de53c77723503e808c4a7", "20992e4adb1cb3a4e7c8e6d702111b9e0c82a7b6"),
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/CueStartupProbeTest.kt": ("0fb8b718dde2b19ea6a1be653f746dd17fc3ca9a", "85fc7859644b84d156a0e38f89d0af44acf24d25"),
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt": ("5be3d82b236065916722f3a4c1775d5066568083", "fc12d8854193d44c4786390d95385f497a3a85d4"),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt": ("74c17436f3f7b7fc168abcf27679cdffebd334e9", "2eca21deb42937d955a45c0d1ed3476d728be60d"),
    "README.md": ("6c5896ee3565ecd309cd679f44d7d37f76baaa97", "2e3d10861626a8cdc0c129e49f7dfca77a1e4e42"),
    "docs/CURRENT_STATE.md": ("7abad7cf2e71243386d37c6e2ecc03626d24453b", "7c2139dc759789dee28d0cf7cd26a5e5bd14c86d"),
    "docs/CI_PIPELINE.md": ("da6e59c1a0d17dc253c8c2c343c40e53a0069972", "2fc4a2efdeadf49859c6a538483e0f759a53d5eb"),
    "docs/DECISIONS.md": ("f03e89c98a9ff4d6b434809da5eafe6211fcd84f", "8c4780b445281596a95a8a1367235a274ef81373"),
}
NEW_TARGETS = {
    "docs/history/RC29_CUE_ROUTE_SETTLEMENT_2026-10-03.md": "2a8e97252571721d610c89928372805eb8bc2349",
}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items()) and all(
        (ROOT / p).is_file() and blob(ROOT / p) == after for p, after in NEW_TARGETS.items()
    )

def baseline_ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items()) and all(
        not (ROOT / p).exists() for p in NEW_TARGETS
    )

def verify_patch_payloads() -> None:
    for rel, expected in PATCHES:
        patch = ROOT / rel
        if not patch.is_file() or blob(patch) != expected:
            raise SystemExit(f"RC29 patch blob mismatch: {rel}")

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    build = (ROOT / "app/build.gradle.kts").read_text()
    probe = (ROOT / "core/audio/src/main/kotlin/studio/guitarlab/core/audio/CueStartupProbe.kt").read_text()
    probe_tests = (ROOT / "core/audio/src/test/kotlin/studio/guitarlab/core/audio/CueStartupProbeTest.kt").read_text()
    verifier = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt").read_text()
    diagnostics = (ROOT / "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt").read_text()
    decisions = (ROOT / "docs/DECISIONS.md").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    for token in ['versionCode = 49', 'versionName = "0.5.0-rc29"']:
        if token not in build:
            raise SystemExit(f"RC29 identity guard failed: {token}")
    for token in [
        "ROUTE_SETTLE_TIMEOUT_NS = 5_000_000_000L",
        "CLOCK_QUALIFICATION_TIMEOUT_NS = 2_000_000_000L",
        "ROUTE_STABLE_POLLS = 4",
        "routeQualifiedAtNs",
    ]:
        if token not in probe:
            raise SystemExit(f"RC29 route-settlement guard failed: {token}")
    if "routeAvailableAfterNs = 2_200_000_000L" not in probe_tests:
        raise SystemExit("RC29 delayed-route regression guard failed")
    for token in [
        "PERFORMANCE_MODE_LOW_LATENCY",
        "setPreferredDevice(expectedMain)",
        "setPreferredDevice(expectedCue)",
        "lastPreflightDiagnostic",
    ]:
        if token not in verifier:
            raise SystemExit(f"RC29 Android preflight guard failed: {token}")
    if 'put("lastCuePreflight", AndroidCueRouteVerifier.lastPreflightDiagnostic())' not in diagnostics:
        raise SystemExit("RC29 diagnostic export guard failed")
    if "D-107 — CUE route settlement and clock qualification use separate bounded phases" not in decisions:
        raise SystemExit("RC29 durable decision guard failed")
    if "RC29 CUE ROUTE SETTLEMENT" not in current or "142418.mp4" not in current:
        raise SystemExit("RC29 current-state owner-evidence guard failed")

verify_patch_payloads()
if ready():
    verify()
    for rel, _ in PATCHES:
        run("git", "apply", "--check", "--reverse", str(ROOT / rel))
    print("Source chain already materialized through RC29 CUE route settlement")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC29 baseline blob mismatch after RC28a")
for rel, _ in PATCHES:
    patch = ROOT / rel
    run("git", "apply", "--check", str(patch))
    run("git", "apply", str(patch))
if not ready():
    raise SystemExit("RC29 terminal blob mismatch")
verify()
for rel, _ in PATCHES:
    run("git", "apply", "--check", "--reverse", str(ROOT / rel))
print("Source chain materialized through RC29 CUE route settlement")
