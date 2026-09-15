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
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Maintenance/documentation commits use `[skip ci]`.

## Evidence boundary
CI #620 remains the authoritative signed **DIGITAL PASS** through H11/H11a/H11b. Physical Review IV (H12–H15 + H14a) is newer application/test/materializer work and remains **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until a new user-dispatched canonical workflow passes.

### CI #621 — H14 test diagnostic, not product-gate completion
CI #621 / run ID `34998393778` / source `afecde0efd4d22e58115eaedadf60eea0eb3615c`:
- software/unit/performance/Lint/debug+release/provenance: **PASS**;
- API 36 connected regression: **21/22 PASS, 1 FAIL**;
- sole failure: `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`;
- signed homologation: correctly **SKIPPED** because API 36 was red.

The #621 failure did not expose an H12/H13/H15 product defect. The original H14 regression hard-coded four swipes, which is insufficient on the narrow default Pixel 7 emulator viewport even though the track region is a valid `LazyRow`. H14a keeps a real physical swipe, retries up to 20 times, stops when the final strip actually intersects the scroller viewport, and still requires invariant MASTER bounds. It does not use `scrollToItem`, timeout inflation, unmerged-tree bypass or assertion removal.

### CI #622 — materializer infrastructure failure before build/test
CI #622 / run ID `35000635666` / source `5832c6800a7523a0bed0b64b9400a00c1fa876c2` failed before compilation or testing.

Both mandatory upstream jobs failed in `Materialize split source` with:

```text
scripts/materialize_ci_sources.sh: line 167: unexpected EOF while looking for matching `"'
```

The signed homologation job was correctly skipped. Root cause was a physically truncated `scripts/materialize_ci_sources.sh`: the file ended inside the Physical Review II H7–H10 guard (`if [[ -f "$PH...`) after the H14a maintenance commit. Therefore #622 is **not** functional evidence against H12/H13/H14/H14a/H15.

The materializer repair restores the complete canonical tail, retains fail-closed guards, restores H14 and H14a as independent/bisectable source parts, and applies Physical Review IV in the exact order H12 engine → H12 UI → H13 → H14 → H14a → H15.

## Physical Review IV — H12–H15 — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
The #620 APK physical review exposed four polish gaps. They are implemented as ordered source parts after H11b.

### H12 — global assisted level analysis
- `Níveis` is available from the Comparação side of the Comparação/Timeline practice bar.
- `Níveis das pistas` presents every project track, current gain, audible-clip count and per-track analysis state.
- `Analisar todas` analyzes tracks with audible material while preserving the same effective-gain semantics as single-track analysis.
- Each track can still be analyzed/reanalyzed and applied independently.
- `Aplicar sugestões (N)` persists all actionable selected gain deltas in **one project mutation / one Undo step**.
- Source/track/clip snapshot validation prevents stale analysis from being applied after project changes.
- Busy state is explicit and application is blocked while analysis is running.

Automated acceptance: `AllTracksLevelDialogInstrumentedTest` plus engine/snapshot validation.

### H13 — Trim timing on the fixed timeline time ruler
- Large `Início`/`Fim` bubbles were removed from over the waveform/handles.
- T1/T2 are projected onto the fixed time ruler immediately above the tracks and below the playhead/section area.
- Each marker has a short yellow tick at the proportional absolute-time position and a compact floating label with precise time.
- Nearby T1/T2 labels separate vertically; elevated z-order keeps labels readable.
- Existing draggable Trim handles remain independently accessible and unchanged in function.

Automated acceptance extends `PhysicalEditingHardeningInstrumentedTest` with `timeline-time-ruler`, `trim-ruler-start` and `trim-ruler-end` contracts.

### H14 / H14a — Mixer overflow with fixed MASTER
- Track strips are inside a horizontally scrollable `LazyRow`.
- MASTER is a separate sibling anchored at the right edge and does not move with track scrolling.
- H14a makes the regression viewport-independent while preserving real swipe interaction and exact MASTER bound assertions.

Automated acceptance: `MixerDockInstrumentedTest` with a 10-track Mixer, bounded physical swipes to the final strip and invariant MASTER geometry.

### H15 — resident Studio return without double-load flash
- `StudioViewModel.load(projectId)` is idempotent when the same project is already resident in the Activity-scoped ViewModel.
- Same-project return no longer clears state, publishes unnecessary `loading=true`, reloads the repository, rebuilds waveform/history state, or creates a second transient Studio state.
- Transport/recording safety is normalized before the early return when needed.
- Undo history and the resident project object survive same-project return.
- Different-project navigation still follows the normal load path.

Automated acceptance extends `GuitarLabLifecycleInstrumentedTest` with same-object residency and retained Undo capability.

## Source representation and local/programmatic validation
Physical Review IV materialization order after H11b:
1. `.source-parts/H12LevelEngine.patch`
2. `.source-parts/H12LevelUi.patch`
3. `.source-parts/H13TrimRuler.patch`
4. `.source-parts/H14MixerHorizontalScroll.patch`
5. `.source-parts/H14aMixerScrollViewportRegression.patch`
6. `.source-parts/H15ResidentStudioReturn.patch`

Patch SHA-256 evidence:
- H12 engine: `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- H12 UI: `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- H13: `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- H14: `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- H14a: `eb347d29e3bdfa61af6da984d85d985ec0871bc8165ce6961a4043fdbb03c658`
- H15: `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

Source validation performed after #622 repair:
- repaired materializer Git blob: `de488110153b8a800b69b520a29860230bcb5838`;
- repaired materializer SHA-256: `612f171be1345b59e0f81e7f0e8cfbd78de0dcbc981fe4edcf22130b1b61779f`;
- `bash -n scripts/materialize_ci_sources.sh`: **PASS** on the exact repaired file;
- H12 → H13 → H14 → H14a → H15 dry-run/application from the exact #620 materialized source snapshot: **PASS**;
- `git diff --check`: **PASS**;
- final changed/new source/test/help blobs match the documented expected Physical Review IV tree, including `MixerDockInstrumentedTest.kt` `5aa984d00035ee259cce21f9cc8717c2f5f759af`.

The repository materializer is designed for a clean checkout/materialization pass. Re-running the entire historical script against an already fully materialized #620 artifact is not a supported idempotence test because earlier RC3 guards intentionally expect repository baselines. Idempotence is enforced at the supported patch/guard boundaries (`apply_patch_once`, explicit final-blob guards) and source drift fails closed rather than reverse-applying an earlier patch through later edits.

No local Android SDK/Gradle environment was available in this session, so JVM tests, Android Lint, APK assembly and instrumentation were **not** re-run locally. #621 remains valid compilation/software evidence for H12–H15 before the H14a-only test correction; the next manual canonical CI is authoritative for the repaired materializer and H14a execution.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: #620 is the last digitally homologated baseline; H12–H15 + H14a are active PRE-GATE refinements from physical review.
- M8: H0–H11 DIGITAL PASS; H12–H15 + H14a IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE; #621 software PASS/API36 diagnostic FAIL; #622 infrastructure/materialization FAIL before build/test.

## Next authoritative gate
After this source/materializer/documentation recovery is consolidated on `main`, the user must manually dispatch one new `GuitarLab Android CI` with `signed_homologation=true` on that exact `main` SHA.

Required PASS:
1. full unit/core/audio/DSP/persistence/migration regression;
2. Android Lint and debug/release assembly;
3. API36 full instrumentation, including H12/H13/H14/H14a/H15 and retained H11 Trim regressions;
4. isolated 1920×1200 geometry;
5. signed homologation;
6. exact package/version/source/signer/checksum provenance.

No assistant-triggered workflow or rerun is permitted.

## Residual physical gate after digital PASS
Use the newly signed H12–H15 candidate only after the exact-source digital gate passes. Physical checks should focus on facts automation cannot establish: modal ergonomics, Trim marker readability over a real timeline/playhead, natural Mixer swipe feel with MASTER fixed, absence of visible Studio return flash, plus retained MK300 routing/REC/meters/synchronization/listening smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single final manual checklist.
