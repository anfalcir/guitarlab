# Documentation Audit — Physical Review IV / H12–H16

Updated: 2026-09-15

## Scope
This audit records the post-CI-#620 physical-feedback implementation covering project-wide level analysis, Trim feedback placement, Mixer horizontal overflow and Studio re-entry polish. CI #620 remains the last digitally homologated binary until a new exact-source workflow passes.

## User-observed physical findings
1. Single-track level analysis works, but project-wide analysis/application is missing.
2. Trim T1/T2 time labels over the clip waveform can be obscured by the draggable handle and are difficult to compare against song sections/time.
3. With more Mixer strips than fit on screen, horizontal dragging does not provide reliable access to hidden tracks; MASTER must remain fixed at right.
4. Returning from Home/Config to an already open Studio project visibly flashes as if the Studio loads twice.

## H12 — batch level analysis
Implementation contracts:
- **Níveis** is placed inside the existing Comparação segment; it does not create a third practice-bar segment.
- Dedicated `Níveis das pistas` modal exposes project-wide status without hiding per-track control.
- Analyze All is non-mutating and sequential, avoiding uncontrolled decode fanout.
- Every async result is guarded by project session, project identity and the analyzed track/clip snapshot before publication.
- Individual Apply remains available.
- Global Apply validates all actionable analyses and persists them in one project mutation, therefore one Undo entry.
- Analysis busy state is visible and prevents conflicting apply/reanalyze actions.

Automated coverage added/extended:
- `AutoSectionsSlotInstrumentedTest.levelsActionIsAvailableFromDockedComparisonSegment`
- `TrackLevelBatchDialogInstrumentedTest` for global/per-track controls, explicit apply semantics and busy-state locking.

## H13 — Trim feedback on the fixed timeline time ruler
Implementation contracts:
- T1/T2 waveform bubbles are removed.
- Trim boundary feedback is projected onto the fixed timeline time ruler directly below the playhead rail.
- Exact mustard/yellow ticks use the same frame-to-fraction timeline mapping as the rest of the timeline.
- Floating T1/T2 time labels sit above their ticks and receive explicit z-priority while Trim is active.
- When T1/T2 are near each other, labels bias in opposite directions to remain legible.
- The indicators remain passive; independent Trim handles continue to own drag interaction and accessibility identity.

Automated coverage:
- existing `PhysicalEditingHardeningInstrumentedTest` remains strict for the two independent handles and now also requires both ruler markers.

## H14 — Mixer horizontal overflow with anchored MASTER
Implementation contracts:
- track strips use a keyed horizontal `LazyRow` with dedicated `LazyListState`;
- horizontal gesture ownership belongs to the track viewport;
- MASTER is a sibling outside that viewport, not an item within the lazy list;
- selected-track behavior and all strip controls remain unchanged.

Automated coverage:
- `MixerDockInstrumentedTest.trackStripsScrollHorizontallyWhileMasterRemainsAnchored` creates overflow, performs a real swipe, verifies strip displacement and checks MASTER x-position invariance.

## H15 — resident Studio re-entry without loading flash
Root cause in the previous source: `StudioViewModel.load(projectId)` recognized a resident same-project case but continued into a full `StudioUiState(loading=true)` replacement, temporarily destroying the visible workspace before loading it again.

Implementation contracts:
- same resident project + idle recording state returns through a silent refresh path;
- resident UI is never cleared merely because the same route re-entered;
- playback is normalized to stopped without discarding the project;
- refresh publication is guarded by session generation, project ID and resident revision so it cannot overwrite a newer edit;
- a truly changed persisted project can be absorbed, including waveform/channel refresh;
- refresh failure leaves the resident project usable.

Automated coverage:
- `GuitarLabLifecycleInstrumentedTest.residentStudioReloadDoesNotBlankTheAlreadyLoadedProject` verifies repeated load and Options→Studio return do not transiently clear the project/set loading.

## H16 — integrated polish
- level-analysis busy state is invalidated wherever corresponding analysis results are invalidated;
- single-track stale checking uses the same audible/non-muted clip set as measurement;
- user guide documents the new Níveis workflow, ruler feedback and Mixer swipe semantics;
- integrated regressions preserve prior H11/H11a/H11b contracts.

## Source representation
Materializer terminal order:
`H11b → H12TrackLevelBatch → H13TrimTimelineRuler → H14MixerOverflow → H15StudioReentry → H16IntegratedPolish`.

Source-part patch SHA-256 values before text-safe gzip/base64 packaging:
- H12: `053880b6bb1ed4ecb96e6bb8a0949c529426b13a9c5ceb09e5447230d66cb8ac`
- H13: `9f7d18d4a41dd2b6a1ff5a3bc64fdf617fd02656ebfab817cf6b8ce6b064feeb`
- H14: `20f26ccf08d0a11906bb78d2bc508585bac9fed236feb82a93f318e1c33b2dcd`
- H15: `698ad2e761b562678f1f862b91cf72320a62aef2ee067031041760fe04a4cac2`
- H16: `1fae8ca1f7241737c316d1957b384ac74b3c6be94e5f551855593060a1be3e26`

Text-safe files:
- `.source-parts/H12TrackLevelBatch.patch.gz`
- `.source-parts/H13TrimTimelineRuler.patch.gz`
- `.source-parts/H14MixerOverflow.patch.gz`
- `.source-parts/H15StudioReentry.patch.gz`
- `.source-parts/H16IntegratedPolish.patch.gz`

## Source validation performed before GitHub consolidation
Using the exact materialized source artifact produced by canonical CI #620:
1. H12→H16 were applied serially to a clean snapshot;
2. all 11 touched application/test files matched the independently built final workspace;
3. `git diff --check` passed;
4. each stage matched reverse dry-run expectations;
5. gzip/base64 package round-trips reproduced the raw patch bytes;
6. no existing test was disabled, no timeout was inflated and no assertion was weakened.

A full Android/Gradle build was not claimed from this source-only environment because the downloaded source artifact does not contain a reusable configured Gradle wrapper/toolchain and the execution container has no external dependency network. The canonical manual CI remains the authoritative build/API36/signing gate.

## Evidence boundary
- CI #620 / source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`: H0–H11 DIGITAL PASS.
- H12–H16: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.
- No workflow was dispatched by the assistant for H12–H16.

## Closure criteria
H12–H16 may move to DIGITAL PASS only after one new manually dispatched `GuitarLab Android CI` on the final `main` source with `signed_homologation=true` passes software, API36/full geometry and signed homologation with matching source/package/version/signer/checksum.

After that, the final physical pass should concentrate only on the real tablet/MK-300 observations that cannot be objectively established by automated gates.
