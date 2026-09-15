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
- `.github/workflows/android-ci.yml` remains manual-only. Maintenance/documentation commits use `[skip ci]`.

## Evidence boundary
CI #620 remains the authoritative DIGITAL PASS through H11/H11a/H11b. Physical Review IV (H12–H15) is newer application/test/materializer work and is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until a new user-dispatched canonical workflow passes.

CI #618 and #619 remain diagnostic history only. #620 superseded both and passed software, API36 full connected regression, tablet geometry and signed homologation on exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`.

## Physical Review IV — H12–H15 — IMPLEMENTED / PRE-GATE
The #620 APK physical review exposed four polish gaps. They are now implemented as ordered source parts after H11b.

### H12 — global assisted level analysis
- `Níveis` is available from the Comparação side of the Comparação/Timeline practice bar.
- A dedicated `Níveis das pistas` modal presents every project track, current gain, audible-clip count and per-track analysis state.
- `Analisar todas` analyzes every track that has audible material while preserving the same effective-gain semantics already validated for single-track analysis.
- Each track can still be analyzed/reanalyzed and applied independently.
- `Aplicar sugestões (N)` applies only actionable recommendations and persists all selected gain deltas in **one project mutation / one Undo step**.
- Source/track snapshot validation prevents stale analysis from being applied after the project changes.
- Busy state is explicit per track and global application is blocked while analysis is running.

Automated acceptance added: `AllTracksLevelDialogInstrumentedTest` plus shared engine snapshot validation.

### H13 — Trim timing on the fixed timeline time ruler
- The old `Início`/`Fim` bubbles were removed from directly over the waveform/handles.
- T1/T2 are projected onto the fixed time ruler immediately above the tracks and below the playhead/section area.
- Each marker has a short yellow tick at the exact proportional frame position and a compact floating label with precise time.
- Marker labels use elevated z-order so they remain readable when coincident with the playhead; nearby T1/T2 labels separate vertically.
- Existing draggable Trim handles remain independently accessible and unchanged in function.

Automated acceptance extends `PhysicalEditingHardeningInstrumentedTest` with `timeline-time-ruler`, `trim-ruler-start` and `trim-ruler-end` contracts.

### H14 — Mixer overflow with fixed MASTER
- Track strips are now inside a horizontally scrollable `LazyRow`.
- The MASTER strip remains a separate sibling anchored at the right edge and does not move with track scrolling.
- The behavior scales to projects with more strips than the tablet width can display.

Automated acceptance extends `MixerDockInstrumentedTest`: a 10-track Mixer is swiped to the last strip while the MASTER left/right bounds must remain unchanged.

### H15 — resident Studio return without double-load flash
- `StudioViewModel.load(projectId)` is now idempotent when the same project is already resident in the Activity-scoped ViewModel.
- Returning Home/Options → the same Studio no longer clears state, publishes an unnecessary `loading=true`, reloads the repository, rebuilds waveform/history state, or visually renders a transient second Studio state.
- Transport/recording safety is still normalized appropriately before the early return.
- Undo history and the resident project object are preserved across the same-project return.
- The in-app `Ajuda` is synchronized with H12 global levels, H13 timeline T1/T2 presentation and H14 Mixer horizontal scrolling.

Automated acceptance extends `GuitarLabLifecycleInstrumentedTest` and verifies same-object residency plus retained Undo capability.

## Source representation and validation
Physical Review IV materialization order:
1. `.source-parts/H12LevelEngine.patch`
2. `.source-parts/H12LevelUi.patch`
3. `.source-parts/H13TrimRuler.patch`
4. `.source-parts/H14MixerHorizontalScroll.patch`
5. `.source-parts/H15ResidentStudioReturn.patch` (includes in-app guide synchronization)

The five patches were serially dry-run/applied against the exact materialized #620 source artifact. `git diff --check` passed and the final materialized tree matched the independently developed H12→H15 tree byte-for-byte for every changed/new source/test/help file.

Patch SHA-256 evidence:
- H12 engine: `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- H12 UI: `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- H13: `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- H14: `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- H15: `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: #620 is the last digitally homologated baseline; H12–H15 are active PRE-GATE refinements from physical review.
- M8: H0–H11 DIGITAL PASS; H12–H15 IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.

## Next authoritative gate
After H12–H15 source parts, materializer and documentation are consolidated on `main`, the user must manually dispatch one new `GuitarLab Android CI` with `signed_homologation=true` on that exact `main` SHA.

Required PASS:
1. full unit/core/audio/DSP/persistence/migration regression;
2. Android Lint and debug/release assembly;
3. API36 full instrumentation, including new H12/H13/H14/H15 contracts and retained H11 Trim regression;
4. isolated 1920×1200 geometry;
5. signed homologation;
6. exact package/version/source/signer/checksum provenance.

No assistant-triggered workflow or rerun is permitted.

## Residual physical gate after digital PASS
Use the newly signed H12–H15 candidate only after the exact-source digital gate passes. Physical checks should focus on visual/touch facts automation cannot establish: modal ergonomics, Trim marker readability over a real timeline/playhead, natural Mixer swipe feel with MASTER fixed, absence of visible Studio return flash, plus retained MK300 routing/REC/meters/synchronization/listening smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single final manual checklist.
