# Documentation Audit — Physical Review III / H11

Updated: 2026-09-15

## Scope
This audit records the complete H11 refinement and its diagnostic/fix loop through the final canonical CI #620 pass. It does not invalidate prior #617 evidence; #620 supersedes it as the active application candidate.

## Evidence boundary
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: authoritative H0–H10 DIGITAL PASS baseline.
- CI #618 / run `34922529980` / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software PASS, API36 FAIL, signing skipped.
- CI #619 / run `34923582119` / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`: software PASS, API36 FAIL, signing skipped.
- **CI #620 / run `34924500870` / source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`: H11/H11a/H11b DIGITAL PASS.**

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

## Diagnostic history
### CI #618 → H11a
The only failed instrumentation was `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`, timing out after one `Cortar` click while waiting for independent Trim handles.

H11a removed a genuine inherited race: the H8 menu flow deferred `beginTrim()` through `pendingTrimClipId` + `LaunchedEffect`. It now invokes `onBeginTrim(clip.id)` synchronously in the same click callback. The test remained unchanged.

### CI #619 → H11b
#619 again passed the complete software gate and H11 feature-specific coverage, but the same single Trim instrumentation remained red.

The final blocker was the H11 selection semantics topology:
- waveform ancestor carried `Modifier.clickable`;
- clip content also carried click semantics;
- Compose merged the descendant semantics tree;
- the visually rendered Trim handles therefore stopped existing as independent accessibility/test nodes.

H11b corrected the architecture:
- lane selection moved to a sibling background hit target;
- clip-level click semantics are absent during Trim;
- independent Trim handles remain exposed;
- waveform/clip/live-waveform track selection is retained.

No timeout was increased, no unmerged-tree bypass was used and no assertion was weakened.

## H11b source validation before CI
- patch SHA-256: `315bbd8d247d25edf8fc4d19adb67fbafae2705f618863d2de98af0d003f570a`;
- base `StudioPlaceholderScreen.kt` blob: `55c7f098416c9d69f6eca7e11c323220fa84173f`;
- final blob: `b1ef57568057d1665153d385de00afa720aebb0e`;
- forward application: PASS;
- reverse dry-run after application: PASS.

## CI #620 closure evidence
Run #620 executed exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` and passed every mandatory gate:
- Unit tests + reproducible performance evidence: PASS;
- Android Lint: PASS;
- debug/release assembly: PASS;
- unsigned release provenance: PASS;
- API 36 full connected instrumentation: PASS;
- unchanged independent Trim-handle regression: PASS;
- H11 waveform-selection/segmented-bar/persistence coverage: PASS;
- isolated 1920×1200 tablet geometry: PASS;
- signed homologation: PASS.

Signed identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3`, versionCode `23`;
- unsigned APK SHA-256 `14c4862371871cf6db85548bd6abc3405cdfcd278d726a5a9f097d533183bafd`;
- signed APK SHA-256 `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- signature scheme v2 verified;
- one RSA-4096 signer;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID `10380000533`.

The downloaded APK checksum was independently recomputed and matched `SHA256SUMS.txt`.

## Status
**H11 / H11a / H11b: DIGITAL PASS / CLOSED AS A DIGITAL GATE.**

Only final real-device/human validation remains for this candidate: visual/touch quality of the Mixer bar, persistent toggle feel, waveform selection ergonomics, physical Trim handles, live MK300 Peak/RMS plausibility and retained routing/synchronization/listening/export smoke.
