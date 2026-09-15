# Documentation Audit — Physical Review III / H11

Updated: 2026-09-15

## Scope
This audit records the post-CI-#617 interaction/metering refinement and the CI #618/#619 failure-correction loop. It does not invalidate #617 evidence through H10.

## Evidence boundary
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: authoritative H0–H10 DIGITAL PASS.
- H11/H11a/H11b: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.
- CI #618 / run `34922529980` / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software PASS, API 36 FAIL, signing skipped.
- CI #619 / run `34923582119` / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`: software PASS, API 36 FAIL, signing skipped.

## Requested H11 behavior
1. Mixer dock header becomes a visually segmented practice bar.
2. Redundant dock title `Mixer`, Pin and X are removed.
3. Top-bar Mixer action is the single persistent open/close toggle.
4. Comparação and Timeline occupy equal horizontal segments with restrained near-square button rounding.
5. Tapping waveform/audio content selects the corresponding track.
6. The actively recorded track exposes real-time Peak and RMS during REC.

## Source representation
- `.source-parts/H11MixerWaveformMetering.patch.gz`
- `.source-parts/H11TrimEntryRaceFix.patch`
- `.source-parts/H11bWaveformSelectionSemantics.patch`
- `scripts/materialize_ci_sources.sh` applies H11 → H11a → H11b after H7–H10.

## H11 automated contracts already green in #619
The #619 run proved these H11 paths under API 36 before the suite stopped on the legacy Trim regression:
- waveform/audio area tap selects its track;
- two docked practice segments have equal geometry;
- Mixer visibility persists across preference-store recreation;
- recording Peak/RMS projection policy passes software regression;
- existing Mixer behavior remains compatible with removal of Pin/X/title ownership.

## CI #618 diagnosis — H11a
The only failed instrumentation in #618 was `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`, timing out while waiting for independent Trim handles after one `Cortar` click.

H11a removed a genuine inherited race: the H8 menu flow deferred `beginTrim()` through `pendingTrimClipId` + `LaunchedEffect`. It now invokes `onBeginTrim(clip.id)` synchronously in the same click callback. The failing test remained unchanged.

## CI #619 diagnosis — final blocker
#619 passed the entire software gate again and failed the same single API 36 regression. This proved H11a was valid but not sufficient.

The actual remaining blocker was introduced by H11 waveform selection semantics:
- the waveform ancestor carried `Modifier.clickable`;
- the clip card also carried clickable semantics, disabled during Trim;
- Compose clickable semantics merge descendants in the merged accessibility tree;
- therefore the visually rendered `trim-start-handle` and `trim-end-handle` stopped existing as independent semantics nodes during Trim.

This is an accessibility/interaction architecture defect, not a flaky timeout. The test must remain strict.

## H11b correction
H11b changes the semantics topology:
- lane-level waveform selection uses a sibling background hit target behind clips, not an ancestor clickable around Trim descendants;
- clip-level `Modifier.clickable` is installed only when the clip is neither dragging nor trimming;
- no disabled clickable remains around the Trim editor;
- Trim handles retain independent accessibility/test identity;
- waveform, clip and live-waveform selection behavior is preserved outside Trim mode.

Exact validation against #619 materialized source:
- patch SHA-256 `315bbd8d247d25edf8fc4d19adb67fbafae2705f618863d2de98af0d003f570a`;
- base `StudioPlaceholderScreen.kt` blob `55c7f098416c9d69f6eca7e11c323220fa84173f`;
- final blob `b1ef57568057d1665153d385de00afa720aebb0e`;
- forward apply: PASS;
- reverse dry-run after apply: PASS;
- no test timeout increase, merged-tree bypass or assertion weakening.

## Closure criteria
H11/H11a/H11b moves to DIGITAL PASS only after a new manually dispatched canonical workflow on the corrected final `main` SHA passes software, API 36 integration/geometry and signed homologation with matching package/version/source/signer/checksum.

Do not cite #618 or #619 as successful H11 evidence. Their value is diagnostic: both prove the software gate remains green, while #619 isolates the remaining failure to Trim-handle accessibility semantics.
