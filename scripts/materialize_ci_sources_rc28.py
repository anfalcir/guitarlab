#!/usr/bin/env python3
"""RC28: canonical physical-route identity for CUE admission and API36 runtime safety."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc27d.py"
PATCHES = [
    (".source-parts/RC28CuePhysicalRouteRuntime.patch", "e87f0da514c7fc21a18d1f4c828d6889fba6d964"),
    (".source-parts/RC28CuePhysicalRouteOperationalDocs.patch", "246b19b0badd90d606d0068031370d13eced9399"),
    (".source-parts/RC28CuePhysicalRouteDomainDocs.patch", "8017a287b19b54d1603156d1d85c6795cfe1d7dc"),
]
TARGETS = {
    "app/build.gradle.kts": ("95c1c2da9bcdf2d653dbc676ec69aa2f5007a60b", "055047c598ba87ed7b01c7801c5bf35cadbdaae4"),
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt": ("c07015dca2ed98369a1dafb5c849f948e964901b", "5be3d82b236065916722f3a4c1775d5066568083"),
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt": ("e431b2db9bf6021080b0baf9cce102770ce33566", "0188b1f6c74dfa8cd5e408689b9ad14b82dc699a"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt": ("85adc30e98acc9834ca1f0e60be33ba4a8535288", "e5d3ac69c01746e9765483b383b3033a5b8931e7"),
    "README.md": ("9793e5fe41cae27621fb57d9afbbeea6b5b8a4f6", "c483a5e8d158b4d8ba1470aae7b7a40eeb49b067"),
    "docs/CURRENT_STATE.md": ("68afa473e1f9855a834c2ba736b9af88714026eb", "aaa3c351059a717e6774c57af20b95a18d9354c0"),
    "docs/CI_PIPELINE.md": ("75a0bec445a5f6cf8d8371b781d43454e3d8218c", "3b11d0358a225ec0cc62e83f4a31febe52417e7a"),
    "docs/DOCUMENTATION_MAP.md": ("13a0adef35b1c21350c1d72c4f7a7df58da2232e", "50b6310b1c6aae7169bb6882144d9d41e79a3613"),
    "docs/STUDIO_OPTIONS_AND_MIXER.md": ("f1296d9c7d2dcf0b07d7fd85746e082dcc20b496", "96cf674eb27fcb45a60fbea27d0f2ffb4b28987d"),
    "docs/TEST_AND_HOMOLOGATION_POLICY.md": ("198fb4238dd0aba579cf007a0e27dbd56c23ddf9", "853c78dab952fddf66977004cd46590d3db606b5"),
    "docs/DECISIONS.md": ("53720dc1bd6b46ffdb520d2ae5f12bab10a190a2", "8ef04e04e9cd2d4bda6896e65d855547b0ad1a71"),
}
NEW_TARGETS = {
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidOutputRouteIdentity.kt": "45705408e577b6a679aa32f2babd1f859a6783ef",
    "platform/audio-android/src/test/kotlin/studio/guitarlab/platform/audio/android/AndroidOutputRouteIdentityTest.kt": "1a687d1c4d9517b3df4d8332e84bbdaa746eeab5",
    "docs/history/RC28_CUE_PHYSICAL_ROUTE_IDENTITY_2026-10-03.md": "12dcd63524baa02f3777b61d9ebc7da4240d0a11",
}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return (
        all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items())
        and all((ROOT / p).is_file() and blob(ROOT / p) == after for p, after in NEW_TARGETS.items())
    )

def baseline_ready() -> bool:
    return (
        all((ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items())
        and all(not (ROOT / p).exists() for p in NEW_TARGETS)
    )

def verify_patch_payloads() -> None:
    for rel, expected in PATCHES:
        patch = ROOT / rel
        if not patch.is_file() or blob(patch) != expected:
            raise SystemExit(f"RC28 patch blob mismatch: {rel}")

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    build = (ROOT / "app/build.gradle.kts").read_text()
    identity = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidOutputRouteIdentity.kt").read_text()
    verifier = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidCueRouteVerifier.kt").read_text()
    playback = (ROOT / "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt").read_text()
    routing = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioAudioRoutingStore.kt").read_text()
    tests = (ROOT / "platform/audio-android/src/test/kotlin/studio/guitarlab/platform/audio/android/AndroidOutputRouteIdentityTest.kt").read_text()
    decisions = (ROOT / "docs/DECISIONS.md").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    for token in ['versionCode = 48', 'versionName = "0.5.0-rc28"']:
        if token not in build:
            raise SystemExit(f"RC28 identity guard failed: {token}")
    for token in [
        "track.routedDevices",
        "routesOnlyToExpected",
        "pairRemainsDistinct",
        "usbPhysicalAddress",
        '"wired-jack|$product|${normalize(address)}"',
    ]:
        if token not in identity:
            raise SystemExit(f"RC28 physical-route guard failed: {token}")
    if "canonicalRoutedDeviceId(track, expected)" not in verifier:
        raise SystemExit("RC28 selection-time verifier guard failed")
    if "AndroidOutputRouteIdentity.pairRemainsDistinct" not in playback:
        raise SystemExit("RC28 runtime verifier guard failed")
    if "track.routedDevices" not in routing or "routedIds.all { it in candidateIds }" not in routing:
        raise SystemExit("RC28 output-candidate probe guard failed")
    for token in [
        "usbLogicalEndpointsOnSameCardShareOnePhysicalIdentity",
        "headsetAndHeadphonesModesShareTheSamePhysicalJack",
        "routeSetMustContainOnlyTheExpectedPhysicalDestination",
    ]:
        if token not in tests:
            raise SystemExit(f"RC28 regression guard failed: {token}")
    if "D-106 — CUE safety is based on canonical physical routes" not in decisions:
        raise SystemExit("RC28 durable-decision guard failed")
    if "RC28 CUE PHYSICAL-ROUTE IDENTITY" not in current:
        raise SystemExit("RC28 current-state guard failed")

verify_patch_payloads()
if ready():
    verify()
    for rel, _ in PATCHES:
        run("git", "apply", "--check", "--reverse", str(ROOT / rel))
    print("Source chain already materialized through RC28 CUE physical-route identity")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC28 baseline blob mismatch after RC27d")
for rel, _ in PATCHES:
    patch = ROOT / rel
    run("git", "apply", "--check", str(patch))
    run("git", "apply", str(patch))
if not ready():
    raise SystemExit("RC28 terminal blob mismatch")
verify()
for rel, _ in PATCHES:
    run("git", "apply", "--check", "--reverse", str(ROOT / rel))
print("Source chain materialized through RC28 CUE physical-route identity")
