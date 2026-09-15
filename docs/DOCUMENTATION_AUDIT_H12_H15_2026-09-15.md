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
- elevated z-order preserves readability over playhead overlap;
- close marker labels use separate vertical offsets.

Regression extension: `PhysicalEditingHardeningInstrumentedTest` now requires the fixed ruler and both T1/T2 ruler semantics in addition to both independent handles.

## H14 — Mixer overflow
Implementation contracts:
- track-strip area is a `LazyRow` with horizontal user scrolling;
- MASTER is outside that scroll container;
- strip selection and all existing Mixer controls remain on their original strips;
- MASTER geometry must remain invariant while the track list is swiped.

Regression extension: `MixerDockInstrumentedTest` creates 10 strips, swipes to the final strip and asserts fixed MASTER left/right bounds.

## H15 — resident Studio return + help synchronization
Implementation contracts:
- same project already resident + not loading => no repository reload and no temporary `loading=true` publication;
- playback/recording mode is normalized safely before returning;
- project object/history/waveform-derived state is preserved for the resident session;
- navigating to a different project still uses the normal load path;
- in-app `Ajuda` documents the new global level workflow, fixed-ruler T1/T2 presentation and Mixer overflow swipe with fixed MASTER.

Regression extension: `GuitarLabLifecycleInstrumentedTest` performs Studio → Options → same Studio after a reversible edit and asserts the same resident project object plus retained Undo history.

## Source materialization
Required order after H11b:
1. `H12LevelEngine.patch`
2. `H12LevelUi.patch`
3. `H13TrimRuler.patch`
4. `H14MixerHorizontalScroll.patch`
5. `H15ResidentStudioReturn.patch` — resident return plus in-app guide synchronization

Patch SHA-256:
- `H12LevelEngine.patch`: `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- `H12LevelUi.patch`: `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- `H13TrimRuler.patch`: `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- `H14MixerHorizontalScroll.patch`: `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- `H15ResidentStudioReturn.patch`: `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

## Source-validation evidence
The exact source snapshot uploaded by CI #620 (`faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`) was used as the baseline.

Validation performed before repository consolidation:
- each Physical Review IV patch forward dry-run: PASS;
- ordered serial patch application H12→H15: PASS;
- `git diff --check`: PASS;
- final changed/new source/test/help files compared against the independently developed clean H12→H15 tree: byte-for-byte match;
- no workflow file is part of Physical Review IV.

Final expected materialized blobs for the changed/new application/test/help files:
- `AllTracksLevelDialogInstrumentedTest.kt`: `a8bdf73b58b8aa42170fc9254b48b9edcde3a9fa`
- `GuitarLabLifecycleInstrumentedTest.kt`: `28a49b2b81baf9f86cff59de18e0b15973285986`
- `MixerDockInstrumentedTest.kt`: `ec0380f23a343dea9b2c53f441060cc1cece3a89`
- `PhysicalEditingHardeningInstrumentedTest.kt`: `66c5bf83b43e5f7d806c0d06a21f5fffded95113`
- `AllTracksLevelDialog.kt`: `2a4b2abfb3b8753f6e466b801b56eaee4b449aec`
- `MixerDock.kt`: `14fb1c2c215e7e320c14cfb7a73feeefb2d19ac2`
- `StudioPlaceholderScreen.kt`: `1f98b57ab878184a5e5ea3f0d680891cc9f81eae`
- `StudioShellScreen.kt`: `82d1dd26e3b25564fd11454aaeaf5b637122796f`
- `StudioViewModel.kt`: `cd77f7a98cb9601b2c580acecb6dd25aff79c4c1`
- `StudioUserGuideDialog.kt`: `70d6ff6678ee15a8445f7b4b3850697de25d6b99`

## Evidence boundary / closure
H12–H15 status is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

They become DIGITAL PASS only when a new manually dispatched canonical workflow on the final `main` SHA passes software/Lint/build, API36 full regression, isolated tablet geometry and signed homologation with matching source/package/version/signer/checksums.

CI must remain user-dispatched. Do not automatically run or rerun GitHub Actions.
