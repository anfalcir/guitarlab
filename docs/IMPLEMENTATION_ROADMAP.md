# Implementation Roadmap

Updated: 2026-09-15

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 USB-audio baseline established.

## M3 — Codec/import foundation — ABSORBED
Consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit physical approval. Later timing work hardens per-session startup behavior without reopening M6.

## M7 — Production audio polish — H11 DIGITAL PASS / H12–H16 PRE-GATE
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform, Mixer/selection/metering refinements and project/master export are implemented.

CI #620 is the last authoritative DIGITAL PASS. Physical Review IV changes active Studio behavior and therefore reopens the exact-source digital gate before final physical closure.

## M8 — Release hardening
### H0–H10 — DIGITAL PASS baseline
CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed software, API 36 full instrumentation, tablet geometry and signed homologation.

### H11/H11a/H11b — DIGITAL PASS
CI #620 / run ID `34924500870` / source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` passed the complete software, API36, geometry and signed homologation pipeline. H11 includes the segmented practice bar, Mixer visibility toggle, waveform track selection, live REC Peak/RMS, synchronous Trim entry and isolated Trim-handle semantics.

### H12 — batch track-level analysis — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- adds **Níveis** to the Comparação segment;
- dedicated all-tracks analysis modal;
- per-track Analyze/Reanalyze/Apply plus global Analyze All/Apply Suggestions;
- project/session/clip stale guards;
- no silent mutation during analysis;
- global Apply commits all accepted gains as one project history mutation / one Undo.

### H13 — Trim time-ruler projection — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- removes T1/T2 time bubbles from the waveform body;
- exact mustard/yellow T1/T2 ticks live on the fixed timeline time ruler;
- time labels float above their ticks and receive explicit visual priority when crossing playhead/section geometry;
- Trim handles remain independent local drag controls.

### H14 — Mixer overflow hardening — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- scrolling track strips use a keyed horizontal `LazyRow` optimized for touch swipe;
- MASTER remains outside the scrolling collection and fixed at the right edge;
- instrumentation verifies track movement and invariant MASTER x-position after swipe.

### H15 — resident Studio re-entry — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- same-project return from Home/Config does not replace resident Studio state with a blank/loading screen;
- background refresh is revision/session guarded;
- actual external project changes can be absorbed without overwriting a concurrent Studio edit;
- refresh failure preserves the resident workspace.

### H16 — integrated polish/regression — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- analysis busy/invalidation consistency;
- audible-clip snapshot consistency for stale detection;
- User Guide synchronization;
- dedicated API36 contracts for batch levels, fixed-ruler Trim feedback, Mixer overflow and resident Studio re-entry.

## Physical Review IV source-validation evidence
H12→H16 were applied serially to a clean exact #620 source artifact and reproduced the expected final tree across all touched application/test files. `git diff --check` passed and each stage matched its reverse dry-run. This validates source composition only; the full Android/software/API36/signing gate remains mandatory.

## Next acceptance gate
After H12–H16 are consolidated into `main`, one new manually dispatched workflow with `signed_homologation=true` must pass on that exact HEAD:
1. complete JVM/unit/performance regression;
2. Android Lint + debug/release assembly;
3. unsigned candidate provenance;
4. API 36 full connected regression, including all H12–H16 and retained H11/Trim tests;
5. isolated 1920×1200 tablet geometry;
6. signed homologation;
7. exact source/package/version/signer/checksum provenance.

Only after that run may H12–H16 move to DIGITAL PASS and the final physical checklist resume on its signed APK.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. No assistant-triggered dispatch or rerun. Source-materialization order is authoritative and fail-closed; diagnostic/source validation is never relabeled as digital homologation.
