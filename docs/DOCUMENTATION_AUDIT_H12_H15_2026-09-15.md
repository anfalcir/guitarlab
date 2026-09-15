# Documentation Audit — Physical Review IV / H12–H15

Updated: 2026-09-15

## Scope
This audit records the post-CI-#620 physical-review refinements requested from Samsung SM-X230 use. It preserves #620 as the last digitally homologated H0–H11 baseline and does not treat source validation as a substitute for the next Android CI gate.

## Physical evidence that triggered this block
The user reported and demonstrated four concrete gaps:
1. single-track level analysis worked, but there was no practical all-track workflow;
2. Trim T1/T2 time bubbles overlapped the waveform handles and made exact cut times hard to read;
3. projects wider than the Mixer viewport could leave hidden track strips inaccessible while MASTER should remain fixed;
4. returning from Home/Options to the same Studio showed a visible short double-load/flicker.

## H12 — all-track level workflow
Implementation contracts:
- `Níveis` action is integrated into the Comparação practice segment rather than adding a new top-level chrome area;
- dedicated modal lists every track and its analysis/application state;
- global analysis skips tracks with no audible material;
- per-track analyze/reanalyze/apply remains available;
- global apply uses one `saveLatest` transaction and therefore one Undo entry;
- analysis publication/apply is guarded by exact track + audible-clip snapshot equality;
- concurrent duplicate analysis is prevented through `trackLevelAnalysisBusy`.

New regression: `AllTracksLevelDialogInstrumentedTest`.

## H13 — Trim ruler clarity
Implementation contracts:
- time bubbles are removed from over the Trim editor;
- existing waveform Trim handles remain the manipulation controls;
- T1/T2 presentation moves to the fixed timeline time ruler;
- each marker exposes a yellow tick at the exact frame fraction and a precise floating time label;
- elevated z-order preserves readability;
- close marker labels use separate vertical offsets.

Regression extension: `PhysicalEditingHardeningInstrumentedTest` requires the fixed ruler and both T1/T2 ruler semantics in addition to both independent handles.

## H14 — Mixer overflow
Implementation contracts:
- track-strip area is a `LazyRow` with horizontal user scrolling;
- MASTER is outside that scroll container;
- strip selection and all existing Mixer controls remain on their original strips;
- MASTER geometry must remain invariant while the track list is swiped.

Regression extension: `MixerDockInstrumentedTest` creates 10 strips, swipes toward the final strip and asserts fixed MASTER left/right bounds.

### CI #621 diagnostic and H14a correction
CI #621 / run `34998393778` / source `afecde0efd4d22e58115eaedadf60eea0eb3615c` passed the complete software gate and failed exactly one of 22 API36 tests: the H14 overflow regression. Signed homologation was correctly skipped.

The regression hard-coded `repeat(4)` swipes. On the default Pixel 7 emulator the fixed MASTER plus dock spacing leaves a narrower track viewport than the target tablet, so four swipes are not sufficient to traverse ten strips.

H14a v1 made the regression viewport-independent without weakening the interaction contract:
- retained real `swipeLeft` gestures on `mixer-track-scroll`;
- checked whether the final strip intersected the actual scroller bounds after each gesture;
- used a bounded 20-gesture safety ceiling rather than a fixed success count;
- kept exact MASTER left/right bounds assertions;
- did not use `scrollToItem`, timeout inflation, unmerged-tree bypass or assertion removal.

H14a remains a distinct source-part after H14 for traceability and bisectability.

H14a v1 patch SHA-256: `eb347d29e3bdfa61af6da984d85d985ec0871bc8165ce6961a4043fdbb03c658`.
H14a v1 final `MixerDockInstrumentedTest.kt` blob: `5aa984d00035ee259cce21f9cc8717c2f5f759af`.

## H15 — resident Studio return + help synchronization
Implementation contracts:
- same project already resident + not loading => no repository reload and no temporary `loading=true` publication;
- playback/recording mode is normalized safely before returning;
- project object/history/waveform-derived state is preserved for the resident session;
- navigating to a different project still uses the normal load path;
- in-app `Ajuda` documents the H12–H14 user-facing workflows.

Regression extension: `GuitarLabLifecycleInstrumentedTest` performs Studio → Options → same Studio after a reversible edit and asserts the same resident project object plus retained Undo history.

## CI #622 — materializer failure and root cause
CI #622 / run `35000635666` / source `5832c6800a7523a0bed0b64b9400a00c1fa876c2` did not reach compilation or functional tests.

Both mandatory upstream jobs stopped in `Materialize split source` with:

```text
scripts/materialize_ci_sources.sh: line 167: unexpected EOF while looking for matching `"'
```

Signed homologation was correctly skipped. Repository inspection showed that `scripts/materialize_ci_sources.sh` had been physically truncated inside the H7–H10 guard at the start of `if [[ -f "$PH...`. The commit that added H14a had retained only the first part of the materializer, removing its canonical tail. This is an infrastructure/source-materialization defect, not evidence of an H12/H13/H14/H14a/H15 functional regression.

The repair restored the complete canonical script and retained H14/H14a as independent stages rather than folding the regression correction into H14.

## CI #623 — repaired chain validation and H14a v2
CI #623 / run `35003025673` / source `973ecae78ee3c159e975b4b1d65a2f733aedf59a` validated that the #622 repair is operational:
- materialization: **PASS**;
- software/unit/performance/Lint/debug+release/provenance: **PASS**;
- API36: **21/22 PASS**;
- sole failure: the same Mixer overflow test;
- H12/H13/H15 connected regressions: **PASS**;
- signed homologation: skipped.

Inspection of the exact #623 source and Compose touch-test semantics identified a narrower test issue. `swipeLeft()` uses the node centerline, and the Mixer centerline crosses horizontal volume/pan slider regions. H14a v2 therefore uses an explicit physical `swipe(start, end)` through the non-slider header band (90%→10% width, 8% height). It keeps the 20-attempt ceiling, actual viewport-intersection check and exact MASTER geometry assertions. No product scroll implementation is changed.

H14a v2 patch SHA-256: `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`.
Final expected `MixerDockInstrumentedTest.kt` blob after H14a v2: `bf81aa9414a376679634f8ddf3f0b9bbf58fde5d`.

## Source materialization
Required order after H11b:
1. `H12LevelEngine.patch`
2. `H12LevelUi.patch`
3. `H13TrimRuler.patch`
4. `H14MixerHorizontalScroll.patch`
5. `H14aMixerScrollViewportRegression.patch`
6. `H15ResidentStudioReturn.patch`

Patch SHA-256:
- `H12LevelEngine.patch`: `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- `H12LevelUi.patch`: `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- `H13TrimRuler.patch`: `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- `H14MixerHorizontalScroll.patch`: `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- `H14aMixerScrollViewportRegression.patch` v1: `eb347d29e3bdfa61af6da984d85d985ec0871bc8165ce6961a4043fdbb03c658`
- `H14aMixerScrollViewportRegression.patch` v2: `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`
- `H15ResidentStudioReturn.patch`: `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

## Source-validation evidence after #623 H14a v2
The exact source snapshot emitted by CI #620 (`faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`) was used as the stable materialized H11 baseline for the Physical Review IV patch-chain check.

Validation performed:
- repaired materializer Git blob: `de488110153b8a800b69b520a29860230bcb5838`;
- repaired materializer SHA-256: `612f171be1345b59e0f81e7f0e8cfbd78de0dcbc981fe4edcf22130b1b61779f`;
- `bash -n scripts/materialize_ci_sources.sh`: PASS;
- each Physical Review IV source part forward dry-run: PASS;
- ordered serial patch application H12 engine → H12 UI → H13 → H14 → H14a → H15: PASS;
- `git diff --check`: PASS;
- final changed/new source/test/help blobs: byte-for-byte match with the audited expected tree;
- `.github/workflows/android-ci.yml` was not changed by the recovery.

Final expected materialized blobs:
- `AllTracksLevelDialogInstrumentedTest.kt`: `a8bdf73b58b8aa42170fc9254b48b9edcde3a9fa`
- `GuitarLabLifecycleInstrumentedTest.kt`: `28a49b2b81baf9f86cff59de18e0b15973285986`
- `MixerDockInstrumentedTest.kt`: `bf81aa9414a376679634f8ddf3f0b9bbf58fde5d`
- `PhysicalEditingHardeningInstrumentedTest.kt`: `66c5bf83b43e5f7d806c0d06a21f5fffded95113`
- `AllTracksLevelDialog.kt`: `2a4b2abfb3b8753f6e466b801b56eaee4b449aec`
- `MixerDock.kt`: `14fb1c2c215e7e320c14cfb7a73feeefb2d19ac2`
- `StudioPlaceholderScreen.kt`: `1f98b57ab878184a5e5ea3f0d680891cc9f81eae`
- `StudioShellScreen.kt`: `82d1dd26e3b25564fd11454aaeaf5b637122796f`
- `StudioViewModel.kt`: `cd77f7a98cb9601b2c580acecb6dd25aff79c4c1`
- `StudioUserGuideDialog.kt`: `70d6ff6678ee15a8445f7b4b3850697de25d6b99`

### Idempotence boundary
The full historical materializer is intended for a clean repository checkout. Re-running that whole script against an already fully materialized #620 source artifact is not the supported idempotence path because earlier RC3 guards intentionally validate repository baselines. Idempotence is instead enforced at patch/guard boundaries: exact reverse dry-run detection for independent patches and final-blob checks for overlapping chains. Unexpected drift fails closed.

### Validation not claimed
#623 provides actual CI evidence for materialization, JVM/performance, Lint, debug/release assembly and unsigned provenance on the repaired chain. The post-#623 H14a v2 change is newer test code and remains source-validated/pre-gate until one new exact-source API36/signing run.

## Evidence boundary / closure
- H0–H11/H11a/H11b: DIGITAL PASS via #620.
- H12–H15 + H14a: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.
- #621: software PASS + one H14 test diagnostic failure.
- #622: materializer infrastructure failure before build/test.
- #623: repaired-chain software/build PASS; API36 21/22 with sole H14a v1 gesture-lane failure.

H12–H15/H14a become DIGITAL PASS only when a new manually dispatched canonical workflow on the final `main` SHA passes software/Lint/build, API36 full regression, isolated tablet geometry and signed homologation with matching source/package/version/signer/checksums.

CI must remain user-dispatched. Do not automatically run or rerun GitHub Actions.
