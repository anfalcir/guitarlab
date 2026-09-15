# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last digitally homologated application/source SHA: `abc0e2a9f8708dd141735915898b508ce0948f48`.
- Last canonical PASS: CI #617 / run ID `34918430241`.
- #617 signed APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only. Ordinary commits use `[skip ci]`.

## Evidence boundary
CI #617 remains the authoritative DIGITAL PASS through H10. Physical Review III / H11 is newer and remains PRE-GATE.

CI #618 / run ID `34922529980` executed H11 on source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2` and **FAILED only the API 36 integration gate**. The software gate passed unit tests, performance evidence, Android Lint, debug/release assembly and unsigned provenance. Signed homologation was correctly skipped because integration did not pass.

The sole failing instrumentation was:
- `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`
- timeout at the first-valid-tap Trim-entry assertion while waiting for `trim-start-handle` and `trim-end-handle`.

## CI #618 root cause and correction — H11a
The failing path still inherited an H8 asynchronous handoff: tapping `Cortar` stored `pendingTrimClipId`, closed the DropdownMenu and relied on a `LaunchedEffect` observing that local-state transition before calling `beginTrim()`. #618 proved that handoff is race-prone under real Compose instrumentation.

H11a removes that deferred local-state protocol. The `Cortar` action now:
1. closes the menu state;
2. invokes `onBeginTrim(clip.id)` synchronously in the same click callback.

The existing failing instrumentation remains the regression contract; the fix does not relax or bypass the assertion.

Source representation:
- `.source-parts/H11MixerWaveformMetering.patch.gz`
- `.source-parts/H11TrimEntryRaceFix.patch`
- `scripts/materialize_ci_sources.sh` applies H11a immediately after H11.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: digital baseline through H10 PASS; closure remains open because H11/H11a changes active Studio behavior.
- M8: H0–H10 DIGITAL PASS in CI #617; H11 + H11a IMPLEMENTED / PRE-GATE.

## Physical Review III — H11
### Segmented Mixer practice bar
- redundant dock title `Mixer`, Pin and X removed;
- top-bar Mixer action is the persistent visibility toggle;
- Mixer-open practice bar contains two equal horizontal segments: **Comparação** and **Timeline**;
- restrained near-square corner radii replace pill-like buttons.

### Waveform selects the track
- tapping waveform/audio lane selects the corresponding track;
- clip cards and live-recording waveform use the same selected-track state.

### Live recording Peak/RMS
- raw capture Peak/RMS is projected only to the active recording track;
- Mixer PK/RMS updates through the existing meter path;
- live waveform shows a compact PK/RMS overlay;
- meter state resets across recording lifecycle boundaries.

### H11a — Trim entry race closure
- removes `pendingTrimClipId` + `LaunchedEffect` deferred dispatch;
- `Cortar` now dispatches `beginTrim` synchronously on the first valid click;
- CI #618's exact failing test is retained as the acceptance regression.

## Next authoritative gate
One new manually dispatched `GuitarLab Android CI` run is required on the final H11a `main` HEAD. Required PASS:
1. software/unit/Lint/build/provenance gate;
2. API 36 full instrumentation including the previously failing first-tap Trim regression;
3. isolated 1920×1200 geometry;
4. signed homologation;
5. package/version/source/signer/checksum agreement.

Do not rerun #618 itself as evidence for the corrected source; use one new workflow on the new final `main` SHA.

## Residual physical gate after digital PASS
Keep manual validation focused on real-device facts:
- visual harmony and touch ergonomics of the segmented Mixer bar;
- persistent top-bar Mixer toggle;
- track selection by tapping waveform/clip/live waveform;
- real MK-300 REC Peak/RMS responsiveness/plausibility;
- retained trim, level-analysis, Stop-during-REC, long-waveform, routing/isolation, synchronization and listening/export smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single final physical checklist after the next exact-source PASS.
