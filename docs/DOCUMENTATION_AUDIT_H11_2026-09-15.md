# Documentation Audit — Physical Review III / H11

Updated: 2026-09-15

## Scope
This audit records the post-CI-#617 interaction/metering refinement and the CI #618 failure/fix loop. It does not invalidate #617 evidence through H10.

## Evidence boundary
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: authoritative H0–H10 DIGITAL PASS.
- H11/H11a: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.
- CI #618 / run ID `34922529980` / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software gate PASS, API 36 integration FAIL, signed homologation correctly skipped.

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
- `scripts/materialize_ci_sources.sh` applies H11 then H11a after H7–H10.

## H11 automated contracts
- recording Peak/RMS projection updates only the recording target and preserves unrelated meter entries;
- two docked practice segments have equal geometry;
- waveform/audio area tap selects the corresponding track;
- Mixer visibility persists across preference-store recreation;
- existing Mixer behavior tests are adapted to removal of Pin/X/title ownership.

## CI #618 diagnosis
All software-side gates passed. The only failed instrumentation was `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`, which timed out waiting for `trim-start-handle`/`trim-end-handle` after one `Cortar` click.

Root cause: the H8 first-tap implementation still dispatched Trim indirectly through `pendingTrimClipId` plus `LaunchedEffect` after DropdownMenu dismissal. That local Compose state handoff is race-prone and can miss the effective transition under instrumentation.

## H11a correction
H11a removes the deferred state/effect bridge and invokes `onBeginTrim(clip.id)` synchronously in the same `Cortar` click callback after setting `clipMenuExpanded = false`.

The failing CI test is intentionally kept unchanged as the regression gate. No timeout was increased and no assertion was weakened.

## Validation before next CI
- H11 software/unit/Lint/build gate already PASS in #618;
- H11a patch is minimal and limited to the Trim menu dispatch path;
- patch applies cleanly against the H11 materialized `StudioPlaceholderScreen.kt`;
- `git diff --check` PASS locally;
- the existing first-tap instrumentation remains the authoritative acceptance test for this correction.

## Closure criteria
H11/H11a moves to DIGITAL PASS only after a new manually dispatched workflow on the corrected final `main` SHA passes software, API 36 integration/geometry and signed homologation with matching package/version/source/signer/checksum.
