# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last digitally homologated application/source SHA: `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`.
- Last canonical PASS: CI #620 / run ID `34924500870`.
- #620 signed APK SHA-256: `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`.
- #620 unsigned release SHA-256: `14c4862371871cf6db85548bd6abc3405cdfcd278d726a5a9f097d533183bafd`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only. Ordinary commits use `[skip ci]`.

## Evidence boundary
CI #620 remains the authoritative DIGITAL PASS through H11/H11a/H11b. Physical Review IV (H12–H16) is newer application/source/test/materializer work and therefore remains **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until one new exact-source manual workflow passes.

CI #618 and #619 remain diagnostic evidence only. They do not supersede the successful #620 candidate.

## CI #620 — retained canonical baseline
Manual workflow #620 ran on exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` and passed software/unit/performance/Lint/build/provenance, API 36 full instrumentation, isolated tablet geometry and signed homologation.

Signed identity:
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- source: `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`;
- unsigned APK SHA-256: `14c4862371871cf6db85548bd6abc3405cdfcd278d726a5a9f097d533183bafd`;
- signed APK SHA-256: `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- APK Signature Scheme v2: verified;
- number of signers: 1;
- key: RSA 4096;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID: `10380000533`.

## Physical Review IV — H12–H16 — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
This review is based on the real-device screenshot/video feedback received after #620. It deliberately preserves #620 as the rollback/evidence baseline while advancing the active source in isolated, serial stages.

### H12 — project-wide level analysis
- A compact **Níveis** action is integrated into the **Comparação** segment of the practice bar.
- It opens a dedicated `Níveis das pistas` modal rather than overloading the single-track menu.
- Each eligible track has independent Analyze/Reanalyze/Apply controls and visible analysis state.
- `Analisar todas` processes eligible tracks serially with project/session/clip stale guards; no creative state is mutated merely by analysis.
- `Aplicar sugestões` validates every analyzed target and commits all accepted gain changes in one project mutation/history entry, making the global operation one Undo step.
- Busy state is explicit per track and mutating actions are locked while their analysis is in flight.

### H13 — Trim T1/T2 on the fixed timeline time ruler
- The old floating time bubbles over the waveform are removed.
- During Trim, the fixed time ruler below the playhead rail expands only enough to host precise T1/T2 feedback.
- T1/T2 use mustard/yellow ticks at the exact frame position plus floating time labels above the ticks.
- The Trim overlay receives explicit z-priority so it remains readable if a Trim boundary crosses the playhead or section geometry.
- Existing independent Trim handles remain the actual drag controls; the ruler indicators are passive feedback only.

### H14 — Mixer track overflow with fixed Master
- Track strips now use a keyed horizontal `LazyRow` viewport intended specifically for touch swiping.
- The MASTER strip is not inside the scrolling collection; it remains a fixed sibling anchored at the right edge.
- Instrumentation explicitly swipes the track viewport and verifies that a track moves while the MASTER x-position remains unchanged.

### H15 — resident Studio re-entry without blank/loading flash
- Re-entering the same already resident project from Home/Config no longer resets the whole UI to `StudioUiState(loading=true)`.
- Same-project re-entry stops transient playback state and performs a silent background refresh while retaining the current Studio tree.
- If the persisted project really changed externally, the refresh updates it only if the original resident revision/session is still current; it cannot overwrite an edit made while refresh is running.
- Background refresh failure leaves the resident project usable instead of blanking the Studio.

### H16 — integrated state/accessibility/guide regression
- Level-analysis busy state is cleared wherever level results are invalidated.
- Single-track stale analysis uses the same audible-clip filter as measurement.
- User Guide documents project-wide Níveis, ruler T1/T2 and Mixer swipe/Master anchoring.
- Added/extended API 36 contracts for the batch-level modal, Trim ruler, Mixer overflow and resident Studio re-entry.

## Source representation and validation
Materialization order after H11b:
`H12 level batch → H13 Trim ruler → H14 Mixer overflow → H15 resident Studio re-entry → H16 integrated polish`.

Source parts:
- `.source-parts/H12TrackLevelBatch.patch.gz`
- `.source-parts/H13TrimTimelineRuler.patch.gz`
- `.source-parts/H14MixerOverflow.patch.gz`
- `.source-parts/H15StudioReentry.patch.gz`
- `.source-parts/H16IntegratedPolish.patch.gz`

Local exact-#620-source validation completed before publication:
- all five patches applied serially to a clean #620 materialized source snapshot;
- reproduced the expected final tree for all 11 touched application/test files;
- `git diff --check` passed;
- each stage reverse dry-run matched the expected state;
- no test timeout/assertion weakening was used.

This is source validation, not a substitute for the Android software/API36/signing gates.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: #620 is the last DIGITAL PASS; final physical closure remains open because H12–H16 alter active Studio behavior.
- M8: H0–H11 DIGITAL PASS in #620; H12–H16 IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.

## Next authoritative gate
After H12–H16 are consolidated into `main`, one new manually dispatched `GuitarLab Android CI` run is required with `signed_homologation=true` on the exact final application/source HEAD. Required PASS:
1. full JVM/unit/performance regression;
2. Android Lint + debug/release assembly + unsigned provenance;
3. API 36 full connected regression, including H12–H16 contracts and all retained Trim/H11 regressions;
4. isolated 1920×1200 geometry;
5. signed homologation;
6. exact package/version/source/signer/checksum agreement.

Do not rerun #620 as evidence for H12–H16 and do not dispatch CI automatically.

## Residual physical gate after the next digital PASS
Manual validation should then focus on the real-device facts automation cannot establish:
- usability/clarity of the all-tracks level-analysis modal;
- T1/T2 readability on the real tablet while crossing playhead/section positions;
- natural horizontal touch swipe through many Mixer strips while MASTER remains fixed;
- absence of the visible Studio re-entry flash on Home/Config round-trips;
- retained MK-300 routing, REC Peak/RMS plausibility, synchronization, listening and export smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single final manual checklist and must target the next exact signed candidate, not #620.
