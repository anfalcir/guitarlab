#!/usr/bin/env python3
"""RC27: owner-approved Mixer card visual hierarchy with exact-source UI/docs qualification."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc26b.py"
PATCHES = [
    (".source-parts/RC27MixerCardHierarchyRuntime.patch", "f175cebf8c44f9bd54e12b6afc2515f528aad606"),
    (".source-parts/RC27MixerCardHierarchyDocs.patch", "439a22b1834ab5fbb7986acf35679e23087c2ce9"),
    (".source-parts/RC27MixerCardHierarchyOperationalDocs.patch", "a90aa18236f3a8d8c5e10ac83c78cc126033f74b"),
]
TARGETS = {
    "app/build.gradle.kts": ("e8196fa208c6f1df0502a384faf011dc4eec6eac", "95c1c2da9bcdf2d653dbc676ec69aa2f5007a60b"),
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ("17b78eef520025601073603c5493fefc1653bb58", "1107a3ab024b17edb0b9b4882c3a9342195ea86d"),
    "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": ("13e6310baff60217579b75f11c2fc70a481239c2", "14e9c5be5c70c4fd68969acede554bceebbbc7aa"),
    "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt": ("05064922d003a51be983a20b1e013362c895a5b3", "ac266d5e34c89941c7dc5f3356e6bec745061569"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt": ("f4f95a243b29f97e5ef71e83bab5e83d3cf2f1cc", "6a91bbf1e74225aee4ad0ac9e8810604539ef1cd"),
    "docs/UI_VISUAL_SYSTEM.md": ("7f4ee8b4625efbfa0bd04f6123d1f075ada928d1", "87251987da41bcf0a78e471f4bf4ca854b1dbca6"),
    "docs/STUDIO_OPTIONS_AND_MIXER.md": ("216f05f591be959fc2c096e0971b84df7d22ccb9", "eed3c23836f11f9f7fadadfe709bc65528fdad6c"),
    "docs/STUDIO_WORKSPACE_GUIDELINES.md": ("155a51e1971db7546bae5fa79affab546b190c51", "e7d498bb71c6964294aabb92f15d293f41aaa112"),
    "docs/DECISIONS.md": ("77ac676852785b4c2d4c0070da4c1aa687c7dc9b", "de01662919d00de8160d0f2dde19e7a4e9df01be"),
    "docs/PRODUCT_REQUIREMENTS.md": ("a3d3bd8480ab1629061f06f3af696b658b9a637d", "27d461542fdda343768fb6a0a652803df169f107"),
    "docs/TEST_AND_HOMOLOGATION_POLICY.md": ("45c3cf55401bdc7d544b7ae49e14d21750fc62fb", "8374c0682f27f5e7a8aa1bbd48adc1126f81182c"),
    "docs/CURRENT_STATE.md": ("830552afdcedbe32eadcd1e46562828dc6efce5f", "3c1e83b0775646bbce5361071289314f88396294"),
    "README.md": ("586627bbd21207d9fd028c38e5cccc5d613013bf", "dfa97f63b95c49c58a5ef827beba4b8df2809e09"),
    "docs/CI_PIPELINE.md": ("8ef034542fadc384a756e180bd5a76bee0ab0342", "5682df0b0c0fd570aeae77e44cf553f40668c7e1"),
    "docs/DOCUMENTATION_MAP.md": ("31411784a2e66a5604c17aeca5652f957ae6d87b", "13a0adef35b1c21350c1d72c4f7a7df58da2232e"),
}
NEW_TARGETS = {
    "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md": "4b003cc2fd22d128fe2ce2d99968666633332388",
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
            raise SystemExit(f"RC27 patch blob mismatch: {rel}")

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    build = (ROOT / "app/build.gradle.kts").read_text()
    mixer = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt").read_text()
    mixer_test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt").read_text()
    tablet_test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt").read_text()
    guide = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt").read_text()
    decisions = (ROOT / "docs/DECISIONS.md").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    ci = (ROOT / "docs/CI_PIPELINE.md").read_text()
    history = (ROOT / "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md").read_text()
    for token in ['versionCode = 47', 'versionName = "0.5.0-rc27"']:
        if token not in build:
            raise SystemExit(f"RC27 identity guard failed: {token}")
    for token in [
        'testTag("mixer-track-header-${track.id}")',
        'testTag("mixer-track-title-group-${track.id}")',
        'testTag("mixer-track-actions-${track.id}")',
        'testTag("mixer-track-meter-section-${track.id}")',
        'testTag("mixer-track-mix-${track.id}")',
        'testTag("mixer-master-header")',
        'testTag("mixer-master-meter-section")',
        'testTag("mixer-master-volume-section")',
        "FontWeight.SemiBold",
        "MixerSectionSurface(",
    ]:
        if token not in mixer:
            raise SystemExit(f"RC27 Mixer hierarchy guard failed: {token}")
    if "mixer-master-actions" in mixer:
        raise SystemExit("RC27 Master must not contain a fake action bank")
    for token in [
        "Track title group must be visually centered",
        "Every soft section must span the useful channel width",
        "Master title must be centered",
        "Master sections must span its useful width",
    ]:
        if token not in mixer_test:
            raise SystemExit(f"RC27 Mixer regression guard failed: {token}")
    for token in ["RC27 cinco pistas com áudio", "rc27-studio-five-audio-channels-complete", "rc27-studio-five-audio-channels-minimum"]:
        if token not in tablet_test:
            raise SystemExit(f"RC27 tablet evidence guard failed: {token}")
    if "cabeçalho centralizado" not in guide or "separação visual suave" not in guide:
        raise SystemExit("RC27 in-app guide guard failed")
    if "D-103 — Mixer cards use centered identity headers and soft functional segmentation" not in decisions:
        raise SystemExit("RC27 durable decision guard failed")
    if "0.5.0-rc27" not in current or "materialize_ci_sources_rc27.py" not in current:
        raise SystemExit("RC27 current-state guard failed")
    if "current candidate tail: RC27 Mixer card hierarchy" not in ci:
        raise SystemExit("RC27 CI-pipeline guard failed")
    if "Status: SOURCE PRE-GATE" not in history:
        raise SystemExit("RC27 checkpoint status guard failed")

verify_patch_payloads()
if ready():
    verify()
    for rel, _ in PATCHES:
        run("git", "apply", "--check", "--reverse", str(ROOT / rel))
    print("Source chain already materialized through RC27 Mixer card hierarchy")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC27 baseline blob mismatch after RC26b")
for rel, _ in PATCHES:
    run("git", "apply", "--check", str(ROOT / rel))
    run("git", "apply", str(ROOT / rel))
if not ready():
    raise SystemExit("RC27 terminal blob mismatch")
verify()
for rel, _ in PATCHES:
    run("git", "apply", "--check", "--reverse", str(ROOT / rel))
print("Source chain materialized through RC27 Mixer card hierarchy")
