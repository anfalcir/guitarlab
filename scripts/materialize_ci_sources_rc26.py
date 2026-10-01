#!/usr/bin/env python3
"""Hash-locked RC26 Studio Mixer/levels presentation refinement; prior payloads immutable."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc25f.py"
PATCHES = [
    (".source-parts/RC26MixerLevelsCode.patch", "618f9d248344b44cfcf379a8e784998d3df839f5"),
    (".source-parts/RC26MixerLevelsTests.patch", "b4432aeda2d73fe40a07c308b7878bee8f1b3056"),
    (".source-parts/RC26MixerLevelsDocs.patch", "944295c3acc0621f3c7626fb02a5963555474006"),
]
TARGETS = {
    "app/build.gradle.kts": ("95d00255dbda77d9c72d36a50e71a38108400023", "e8196fa208c6f1df0502a384faf011dc4eec6eac"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt": ("fabdef6dc0931cae81f7ccc73dad0a447ec0276c", "5f82d000943b60d60ec51e76d0a41bb51f08eed8"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ("37aa90d2bbeeadfefff94ea5ad67a6260da40aba", "47547dace1aa90012f26cf1852cd1b89f66605f2"),
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ("1ef6e8e5660fdfc95d6f73eebf5402050cba3f84", "96d714d5f4c62d96bb5b6d22b58e105cbf92d7a2"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt": ("604a5201761a57e99eadd49b759c386e91445cec", "f4f95a243b29f97e5ef71e83bab5e83d3cf2f1cc"),
    "app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt": ("f51ea548bfeacade9b25fea73d3fdbeb515e8060", "07115d0502e3e8badabfb35fb427fff9bbc36c90"),
    "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt": ("29308b7e6cb511acee73db937d7a0028e54d5362", "05064922d003a51be983a20b1e013362c895a5b3"),
    "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": ("97a3992482d1fa6b4cc410fa62f383d29d881635", "72902c74310dbf1b5169a00ff6535d2b03b3a46b"),
    "docs/STUDIO_WORKSPACE_GUIDELINES.md": ("8dc1a5df71474007f4c1a54bce55f6fcf084e697", "155a51e1971db7546bae5fa79affab546b190c51"),
    "docs/STUDIO_OPTIONS_AND_MIXER.md": ("05ee36af584460d3ac0f7d6260ec67739e732cf8", "216f05f591be959fc2c096e0971b84df7d22ccb9"),
    "docs/UI_VISUAL_SYSTEM.md": ("015f2a5fe9a910112bb59cea8b52261e25351555", "7f4ee8b4625efbfa0bd04f6123d1f075ada928d1"),
    "docs/DECISIONS.md": ("e8b651b760be4e6b5f0d45e5ab4523cf80472977", "77ac676852785b4c2d4c0070da4c1aa687c7dc9b"),
    "docs/CURRENT_STATE.md": ("7d7eab286548bd8bf9b9017787c2ea1d71fcf3d9", "65168515b58da57c0020255a3b5e2077b836d44a"),
    "README.md": ("ad135caf1f58d247fd83945b5b22cae5d59b8465", "80682db1015e5ad1e2a9e2b0af5dc17730f47f89"),
}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items())

def verify_patch_payloads() -> None:
    for rel, expected in PATCHES:
        patch = ROOT / rel
        if not patch.is_file() or blob(patch) != expected:
            raise SystemExit(f"RC26 patch blob mismatch: {rel}")

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    checks = {
        "app/build.gradle.kts": ['versionCode = 46', 'versionName = "0.5.0-rc26"'],
        "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt": [
            "Icons.Default.Speed",
            'testTag("studio-action-levels")',
            "Modifier.width(152.dp)",
        ],
        "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": [
            "levelsEnabled = structuralControlsEnabled",
            "onOpenLevelAnalysis = { allLevelsDialogVisible = true }",
        ],
        "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": [
            "MasterVolumeSlider(",
            'testTag("mixer-master-volume-slider")',
            'testTag("mixer-master-volume-readout")',
            'testTag("mixer-master-meters")',
            "Arrangement.SpaceBetween",
        ],
        "app/src/androidTest/java/studio/guitarlab/app/StudioWorkspaceBarInstrumentedTest.kt": [
            "studio-action-levels",
            "Níveis must open directly instead of reusing a panel",
        ],
        "app/src/androidTest/java/studio/guitarlab/app/StudioMixerTabletInstrumentedTest.kt": ["studio-action-levels"],
        "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": [
            "Track PK/RMS must use the strip full inner width",
            "Master volume control must span the useful card width",
        ],
        "docs/DECISIONS.md": ["D-102 — Níveis is a third navbar action"],
    }
    for rel, tokens in checks.items():
        content = (ROOT / rel).read_text()
        for token in tokens:
            if token not in content:
                raise SystemExit(f"RC26 semantic guard failed: {rel}: {token}")
    mixer = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt").read_text()
    if 'Text("Níveis")' in mixer or "onOpenLevelAnalysis" in mixer:
        raise SystemExit("RC26 Master must not own the Níveis action")
    bar = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/StudioWorkspaceBar.kt").read_text()
    if bar.count('testTag("studio-action-levels")') != 1:
        raise SystemExit("RC26 must expose exactly one navbar Níveis trigger")
    if bar.count("Modifier.width(152.dp)") != 2:
        raise SystemExit("RC26 navbar must retain equal 152dp left/right reservations")
    left = bar.index("Row(Modifier.width(152.dp)")
    levels = bar.index('testTag("studio-action-levels")')
    transport = bar.index("Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { transportContent() }")
    if not left < levels < transport:
        raise SystemExit("RC26 Níveis trigger must live inside the left navbar group before transport")

verify_patch_payloads()
if ready():
    verify()
    for rel, _ in PATCHES:
        run("git", "apply", "--check", "--reverse", str(ROOT / rel))
    print("Source chain already materialized through RC26 Mixer/levels refinement")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC26 baseline blob mismatch after RC25")
for rel, _ in PATCHES:
    run("git", "apply", "--check", str(ROOT / rel))
    run("git", "apply", str(ROOT / rel))
if not ready():
    raise SystemExit("RC26 terminal blob mismatch")
verify()
for rel, _ in PATCHES:
    run("git", "apply", "--check", "--reverse", str(ROOT / rel))
print("Source chain materialized through RC26 Mixer/levels refinement")
