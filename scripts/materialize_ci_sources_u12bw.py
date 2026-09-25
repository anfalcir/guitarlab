#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREVIOUS = ROOT / "scripts/materialize_ci_sources_u12bv.py"
PATCH = ROOT / ".source-parts/U12bwWaveformAndReferenceRestore.patch"
PATCH_BLOB = "c9c824662bb4faca2f67f7925740ad5a04840a6c"
TARGETS = {
    "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt": ("6f60a15195e4f17b981fdafa30ac4f2399acf2ed", "b3c0fd1a3974a8a612e0af77401104826da6bc70"),
    "app/src/androidTest/java/studio/guitarlab/app/PhysicalEditingHardeningInstrumentedTest.kt": ("8d1ee2bb3965b079e106756586b8bf0ff0d08bbb", "019ba7aa969d4d37eaaa05aa28c4c44b7b633ba3"),
    "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt": ("674947c23c3f7bd657ab31e0b4030cd962aff75e", "55f9028d8a32eff6516ba20224df5d086d06a225"),
    "app/src/main/java/studio/guitarlab/app/ui/AppNavigationViewModel.kt": ("bb50fb3bfb2b13505442c19c524e7b29e0a5d213", "f71e5a55f00777e9bdd91a0a8e28174f970838c8"),
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt": ("ea852856665dea56da2107d21e6cd8a3aa4d5065", "d17cf271f3d88b6b516e159d3c27fc187b83c27d"),
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt": ("2445bba607b4b730dade4a796dd1c4d2abb2173c", "a7982d01296f6c61c05a860798e239279e9c5d80"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt": ("ac2990aed12e000e9976735d6641d986f9c77b92", "6caa17e56c450dd2dcbd519709eb194e41a05db8"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt": ("6955f7df466a3611935a47946bc99c8de481e3d9", "6a9ab339ea86e4ccbe5f44b682ea9d5c2e551d17"),
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt": ("673949b4773b005ac058597f859796b97cd875ee", "02c53da6409543e57e77be4522b2ea3e6a856fb3"),
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt": ("cbd645d5dbeb67df82290a2395bc8d6d3721f81b", "82e3640b3644d312fd66335d8eb505528b174935"),
    "app/src/main/java/studio/guitarlab/app/ui/WaveformMini.kt": ("3fb9ebc50a6dd55b4309314a8788ef604ae8a51f", "f0ab880f297374918faaf84f6450e86962253bdf"),
    "app/src/test/java/studio/guitarlab/app/ui/AppNavigationViewModelTest.kt": ("498310a72d61156567a6ddc970e9287f76aae2f3", "d38e42f6f4bec40b0b7a968bcce9aedcb33dcef7"),
    "app/src/test/java/studio/guitarlab/app/ui/WaveformRenderReducerTest.kt": (None, "d078949b88a51fa116ccea82b746296436a6d576"),
    "core/codec/src/main/kotlin/studio/guitarlab/core/codec/WaveformEnvelope.kt": ("c67dea4358e715d328c61edfe454ab9534316027", "61c43fdd6f58b93e48718ebd696ffa2154e4e46f"),
    "core/codec/src/test/kotlin/studio/guitarlab/core/codec/WaveformEnvelopeBuilderTest.kt": ("ea8126ee5492df6605e12a79f64584a72e3cf902", "92c9300471d7468b22cce68e403818ae7cc4fcdf"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/PreparedReferencePipeline.kt": ("f2304f650f2565b8aedf1a97f2289c466b112fa1", "69b97fe68c6545f7ebb73d53512b51bee9a448a9"),
    "core/project/src/main/kotlin/studio/guitarlab/core/project/WaveformCacheStore.kt": ("3665c37c7bf1d8debc79975e4c3042bf96acd00e", "1fdd125c7a27741667b1d405475fc60cbb0bf175"),
    "core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferencePipelineTest.kt": ("e3da70403d28a6054c405d407077e7fa5bcc861b", "a58e0d7650f204c78608fd45a4ba7bb989ba1a42"),
    "core/project/src/test/kotlin/studio/guitarlab/core/project/WaveformCacheStoreTest.kt": ("7805a8c77c3b32afe163b313929b504b24f1e65c", "603e4ff798972d1d8b6d74afae962e030ab2f573"),
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

    def read(rel: str) -> str:
        return (ROOT / rel).read_text(encoding="utf-8")

    studio = read("app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt")
    cache = read("core/project/src/main/kotlin/studio/guitarlab/core/project/WaveformCacheStore.kt")
    envelope = read("core/codec/src/main/kotlin/studio/guitarlab/core/codec/WaveformEnvelope.kt")
    renderer = read("app/src/main/java/studio/guitarlab/app/ui/WaveformMini.kt")
    timeline = read("app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt")
    refs = read("core/project/src/main/kotlin/studio/guitarlab/core/project/PreparedReferencePipeline.kt")
    prepare = read("app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt")
    home = read("app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt")
    nav = read("app/src/main/java/studio/guitarlab/app/ui/AppNavigationViewModel.kt")
    app = read("app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt")
    shell = read("app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt")
    nav_test = read("app/src/test/java/studio/guitarlab/app/ui/AppNavigationViewModelTest.kt")
    ref_test = read("core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferencePipelineTest.kt")

    required = (
        (studio, "const val WAVEFORM_POINTS = 4096"),
        (studio, "WaveformCacheIdentity.forClip(clip, WAVEFORM_POINTS)"),
        (studio, "startFrame = clip.sourceStartFrame"),
        (studio, "frameCount = clip.lengthFrames"),
        (studio, "if (latest == residentProject) return@withContext null"),
        (studio, "private fun loadWaveformState(project: GuitarProject)"),
        (cache, "data class WaveformCacheIdentity("),
        (cache, "const val CURRENT_ALGORITHM_VERSION = 2"),
        (cache, "if (input.readUTF() != identity.fingerprint) return null"),
        (envelope, "val bucketStart = point.toLong() * requestedFrames / pointCount"),
        (envelope, "decoder.seekToFrame(startFrame)"),
        (renderer, "object WaveformRenderReducer"),
        (renderer, "maxPerColumn(peaks: List<Float>, columns: Int)"),
        (timeline, "val clipWidth = naturalWidth.coerceAtLeast(1.dp)"),
        (refs, "enum class PreparedReferenceRestoreTarget"),
        (refs, "fun restoreSelected("),
        (refs, "clip.trackId in targetTrackIds && clip.takeId == null"),
        (prepare, 'testTag("prepare-restore-references")'),
        (prepare, 'testTag("restore-reference-dialog")'),
        (prepare, 'testTag("restore-reference-backing")'),
        (prepare, 'testTag("restore-reference-guitar")'),
        (home, "fun restorePreparedReferences("),
        (nav, "private val _navigationEntry = MutableStateFlow(0L)"),
        (nav, "_navigationEntry.value = _navigationEntry.value + 1L"),
        (app, "val navigationEntry by navigationViewModel.navigationEntry.collectAsState()"),
        (app, "navigationEntry = navigationEntry"),
        (shell, "LaunchedEffect(projectId, navigationEntry) { viewModel.load(projectId) }"),
        (nav_test, "navigation entry changes even when destination route is identical"),
        (ref_test, "assertTrue(restored.clips.contains(backingTakeClip))"),
    )
    for text_value, token in required:
        if token not in text_value:
            raise SystemExit(f"U12bw semantic guard failed: {token}")

    forbidden = (
        (studio, "const val WAVEFORM_POINTS = 320"),
        (studio, "private fun loadWaveforms("),
        (studio, "private fun loadWaveformChannels("),
        (timeline, "naturalWidth.coerceAtLeast(92.dp)"),
        (prepare, "prepare-repair-references"),
        (home, "fun repairPreparedReferenceBindings("),
    )
    for text_value, token in forbidden:
        if token in text_value:
            raise SystemExit(f"U12bw stale behavior remains: {token}")

if not PATCH.is_file() or blob(PATCH) != PATCH_BLOB:
    raise SystemExit("U12bw patch blob mismatch")

if ready():
    verify()
    run("git", "apply", "--check", "--reverse", str(PATCH))
    print("Source chain already materialized through U12bw")
    raise SystemExit(0)

if not PREVIOUS.is_file():
    raise SystemExit("Missing U12bv materializer")
run("python3", str(PREVIOUS))
if not baseline_ready():
    raise SystemExit("U12bw baseline blob mismatch after U12bv")
run("git", "apply", "--check", str(PATCH))
run("git", "apply", str(PATCH))
if not ready():
    raise SystemExit("U12bw terminal blob mismatch")
verify()
run("git", "apply", "--check", "--reverse", str(PATCH))
print("Source chain materialized through U12bw")
