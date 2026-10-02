#!/usr/bin/env python3
"""RC27b: compact clipping warning face without changing its 48dp action semantics."""
from __future__ import annotations
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_rc27a.py"
PATCHES = [
    (".source-parts/RC27bCompactClipBadgeRuntime.patch", "a972a7a42c399598fef1cd6dca6f6b8430841b57"),
    (".source-parts/RC27bCompactClipBadgeDocs.patch", "1628ec2591070f5a8fe0c20f3f8c4c4cf5b1521f"),
]
TARGETS = {
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt": ("1107a3ab024b17edb0b9b4882c3a9342195ea86d", "da134f013f7fced214081f31f32b402a1b2076f2"),
    "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt": ("14e9c5be5c70c4fd68969acede554bceebbbc7aa", "49175dca73570f76c719e2ba5e8ab8d154c6e414"),
    "docs/UI_VISUAL_SYSTEM.md": ("87251987da41bcf0a78e471f4bf4ca854b1dbca6", "07335d18a3600bf5788fcedf3f96b574517405a2"),
    "docs/STUDIO_OPTIONS_AND_MIXER.md": ("eed3c23836f11f9f7fadadfe709bc65528fdad6c", "984b1197451883d0b35431c8ad067cf00633c3c3"),
    "docs/DECISIONS.md": ("de01662919d00de8160d0f2dde19e7a4e9df01be", "7dbd7a43456b48e264f914dd319719a1cf5e0b5c"),
    "docs/TEST_AND_HOMOLOGATION_POLICY.md": ("8374c0682f27f5e7a8aa1bbd48adc1126f81182c", "81ec78f4127964e5748cdcc670632c44ddc40c25"),
    "docs/CURRENT_STATE.md": ("087886912096b1019f82aa3142f8aba91f185c6f", "65720a944e0b88e0facc38f56d2028df4f342150"),
    "docs/history/RC27_MIXER_CARD_HIERARCHY_2026-10-02.md": ("3bf18775d8d8cb625a7bbfcc97a4d6b2c7d26c39", "7be552a8e568acde649034dd690a1b1c2938b4a3"),
    "docs/CI_PIPELINE.md": ("2e095be65346e4c7442ab1f7d3b3e155f811dd30", "b14353b64694c84054336c8c57e2384f1beab8a2"),
    "README.md": ("f1334adcc2bde915c23955ea04377300bca88120", "409b246c40864a96ba6d84aaa55dbc5109d442c2"),
}

def run(*args: str) -> None:
    subprocess.run(list(args), cwd=ROOT, check=True)

def blob(path: Path) -> str:
    return subprocess.check_output(["git", "hash-object", str(path)], cwd=ROOT, text=True).strip()

def ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == after for p, (_, after) in TARGETS.items())

def baseline_ready() -> bool:
    return all((ROOT / p).is_file() and blob(ROOT / p) == before for p, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check", "--", ".", ":(exclude).source-parts/*.patch")
    mixer = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt").read_text()
    test = (ROOT / "app/src/androidTest/java/studio/guitarlab/app/MixerDockInstrumentedTest.kt").read_text()
    decisions = (ROOT / "docs/DECISIONS.md").read_text()
    current = (ROOT / "docs/CURRENT_STATE.md").read_text()
    for token in [
        "Icons.Default.Warning",
        'Modifier.size(width = 28.dp, height = 24.dp).testTag(indicatorTag)',
        'indicatorTag = "mixer-master-clip-indicator"',
        'indicatorTag = "mixer-track-clip-indicator-${track.id}"',
    ]:
        if token not in mixer:
            raise SystemExit(f"RC27b runtime guard failed: {token}")
    for token in [
        "Track CLIP indicator must not cover the centered title",
        "Master CLIP indicator must not cover MASTER",
        "rc27b-mixer-segmented-narrow-clipping",
        "rc27b-mixer-segmented-large-font",
    ]:
        if token not in test:
            raise SystemExit(f"RC27b regression guard failed: {token}")
    if "D-104 — Clipping keeps a 48 dp header target" not in decisions:
        raise SystemExit("RC27b decision guard failed")
    if "scripts/materialize_ci_sources_rc27b.py" not in current:
        raise SystemExit("RC27b current-state tail guard failed")

for rel, expected in PATCHES:
    patch = ROOT / rel
    if not patch.is_file() or blob(patch) != expected:
        raise SystemExit(f"RC27b patch blob mismatch: {rel}")

if ready():
    verify()
    for rel, _ in PATCHES:
        run("git", "apply", "--check", "--reverse", str(ROOT / rel))
    print("Source chain already materialized through RC27b compact CLIP badge")
    raise SystemExit(0)

run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("RC27b baseline blob mismatch after RC27a")
for rel, _ in PATCHES:
    run("git", "apply", "--check", str(ROOT / rel))
    run("git", "apply", str(ROOT / rel))
if not ready():
    raise SystemExit("RC27b terminal blob mismatch")
verify()
for rel, _ in PATCHES:
    run("git", "apply", "--check", "--reverse", str(ROOT / rel))
print("Source chain materialized through RC27b compact CLIP badge")
