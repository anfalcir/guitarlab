#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bo.py"
PATCH = ROOT / ".source-parts/U12bpAndroidCompileAndAdbHardening.patch"
PATCH_BLOB = "c02a9508586d61a2e90487ffa735690a5c51c16e"
TARGETS = {
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt": (
        "77f77f3a62a0c7015583f3ea6e1a68805c899579",
        "f13441dec13ca4a09271fe0fd4df0a75e3a4e54d",
    ),
    "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt": (
        "30e51cb0500e867d0765c5209c4033ab69478219",
        "2365960949f75f039ec6f8b5ee858469390847b2",
    ),
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt": (
        "dda46082508bd4f1991290c78f6379c0422793f6",
        "cbd645d5dbeb67df82290a2395bc8d6d3721f81b",
    ),
    ".github/workflows/android-ci.yml": (
        "b0fe700a3f9515510701fafc2353f1da323b1cfe",
        "9f4d6144da5b36b40a5d4dfa5a6a859007c4d7c4",
    ),
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
    return all((ROOT / rel).is_file() and blob(ROOT / rel) == before for rel, (before, _) in TARGETS.items())

def verify() -> None:
    run("git", "diff", "--check")
    bundle = (ROOT / "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticBundleExporter.kt").read_text()
    screen = (ROOT / "app/src/main/java/studio/guitarlab/app/diagnostics/DiagnosticsScreen.kt").read_text()
    prepare = (ROOT / "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt").read_text()
    workflow = (ROOT / ".github/workflows/android-ci.yml").read_text()
    required = [
        (bundle, "as? JSONObject"),
        (bundle, "var result: String = value"),
        (prepare, "val provenance = asset.provenance"),
        (workflow, "Reset ADB server before API 36 regression"),
        (workflow, "adb wait-for-device"),
        (workflow, "guitarlab-avd-v2-api36-default-x86_64-pixel7"),
    ]
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"U12bp guard failed: {token}")
    if "import androidx.compose.foundation.layout.weight" in screen:
        raise SystemExit("U12bp stale internal weight import remains")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bp patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bp")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bo materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bp baseline blob mismatch")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bp terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bp")
