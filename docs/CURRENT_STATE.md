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

CI #618 / run ID `34922529980` / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2` and CI #619 / run ID `34923582119` / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a` both passed the complete software gate and failed only API 36 integration. Signed homologation was correctly skipped in both runs.

In #619 the complete H11 feature-specific coverage passed, including waveform track selection, equal segmented practice controls, Mixer visibility persistence and live-recording meter policy. The sole remaining failure was again:
- `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`
- timeout waiting for the independent `trim-start-handle` / `trim-end-handle` semantics nodes.

## H11a — genuine race removed, but not sufficient
#618 exposed a real inherited H8 race: `Cortar` deferred `beginTrim()` through `pendingTrimClipId` + `LaunchedEffect` after DropdownMenu dismissal. H11a removed that protocol and now dispatches `onBeginTrim(clip.id)` synchronously in the same click callback.

#619 proved that this was a valid hardening fix but not the final blocker: the same instrumentation still could not observe the independent Trim handles.

## CI #619 root cause and correction — H11b
H11 had added track selection with `Modifier.clickable` on the ancestor waveform container and another clickable on the clip card. In Compose, clickable semantics merge descendants. During Trim, those semantics caused the two visible Trim handles to disappear as independent nodes from the merged accessibility/test tree. This is an accessibility/interaction architecture defect, not a timeout problem.

H11b fixes the structure instead of weakening the test:
1. waveform-lane selection is provided by a sibling background target behind clip content, not by an ancestor clickable;
2. the clip installs click semantics only while it is not being trimmed;
3. during Trim no clip-level clickable semantics can merge the two handle descendants;
4. the existing `trim-start-handle` / `trim-end-handle` contract remains unchanged;
5. waveform/clip/live-waveform track selection remains supported.

Source representation:
- `.source-parts/H11MixerWaveformMetering.patch.gz`
- `.source-parts/H11TrimEntryRaceFix.patch`
- `.source-parts/H11bWaveformSelectionSemantics.patch`
- `scripts/materialize_ci_sources.sh` applies H11 → H11a → H11b after H7–H10.

H11b exact source validation:
- base `StudioPlaceholderScreen.kt` blob: `55c7f098416c9d69f6eca7e11c323220fa84173f`;
- final H11b blob: `b1ef57568057d1665153d385de00afa720aebb0e`;
- patch SHA-256: `315bbd8d247d25edf8fc4d19adb67fbafae2705f618863d2de98af0d003f570a`;
- forward application and reverse dry-run: PASS against exact #619 materialized source;
- no timeout increase or assertion weakening.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: digital baseline through H10 PASS; closure remains open because H11/H11a/H11b changes active Studio behavior.
- M8: H0–H10 DIGITAL PASS in CI #617; H11 + H11a + H11b IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.

## Physical Review III — H11 retained behavior
### Segmented Mixer practice bar
- redundant dock title `Mixer`, Pin and X removed;
- top-bar Mixer action is the persistent visibility toggle;
- Mixer-open practice bar contains two equal horizontal segments: **Comparação** and **Timeline**;
- restrained near-square corner radii replace pill-like buttons.

### Waveform selects the track
- tapping waveform/audio lane selects the corresponding track;
- clip cards and live-recording waveform use the same selected-track state;
- H11b preserves independent Trim-handle accessibility while retaining this behavior.

### Live recording Peak/RMS
- raw capture Peak/RMS is projected only to the active recording track;
- Mixer PK/RMS updates through the existing meter path;
- live waveform shows a compact PK/RMS overlay;
- meter state resets across recording lifecycle boundaries.

## Next authoritative gate
One new manually dispatched `GuitarLab Android CI` run is required on the final H11b `main` HEAD. Required PASS:
1. software/unit/Lint/build/provenance gate;
2. API 36 full instrumentation including the unchanged independent Trim-handle regression;
3. H11 selection/segmented-bar/persistence regressions;
4. isolated 1920×1200 geometry;
5. signed homologation;
6. package/version/source/signer/checksum agreement.

Do not rerun #618 or #619 as evidence for the corrected source; use one new workflow on the new final `main` SHA.

## Residual physical gate after digital PASS
Keep manual validation focused on real-device facts:
- visual harmony and touch ergonomics of the segmented Mixer bar;
- persistent top-bar Mixer toggle;
- track selection by tapping waveform/clip/live waveform;
- Trim handles remain independently usable after waveform-selection hardening;
- real MK-300 REC Peak/RMS responsiveness/plausibility;
- retained level-analysis, Stop-during-REC, long-waveform, routing/isolation, synchronization and listening/export smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single final physical checklist after the next exact-source PASS.
