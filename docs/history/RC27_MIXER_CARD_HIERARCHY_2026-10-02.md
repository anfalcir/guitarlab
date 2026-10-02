# RC27 — Mixer card visual hierarchy

Date: 2026-10-02
Status: RC27a VISUAL EVIDENCE RE-GATE

## Trigger and owner-approved target

Physical review of the signed RC26 Studio found the Mixer substantially improved but still visually flat: track/Master titles lacked a clear centered identity hierarchy and functional groups read as one undifferentiated control stack. The owner approved the refined preview on 2026-10-02 and requested repository implementation with full documentation and regression coverage.

## Presentation contract

RC27 is presentation-only.

Track cards:
- center the track accent dot + title as one identity group;
- retain the existing 200 dp normal-font width and 4 dp side padding;
- keep Mute/Solo/CUE/REC as one contiguous four-target action bank;
- softly separate header, action bank, metering and mix using restrained 6 dp internal surfaces;
- keep PK/RMS at the RC26 full useful width;
- keep Volume/Pan in one mix section with a quiet internal divider;
- retain subtle track-color identity when unselected and strengthen border/tonal emphasis when selected;
- preserve comparison/clip/pan status affordances without turning them into title alignment anchors.

Master:
- center `MASTER`;
- keep the fixed 144 dp column;
- use the same header/meter/volume section grammar;
- never add an empty/fake action bank merely to mirror track cards;
- preserve full-width metering and the wide volume slider with one-line `VOL +dB` readout.

Global invariants:
- complete/minimum dock heights remain 252/172 dp at font scale 1.0;
- 48×48 dp action targets and 46×46 dp faces remain unchanged;
- large-font width/header/meter growth remains unchanged;
- no audio, routing, persistence, level-analysis, export or MAIN/CUE behavior changes;
- no tuner/pitch scope is introduced.

## Implementation scope

Runtime:
- `app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt`;
- `app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt`;
- candidate identity `0.5.0-rc27` / versionCode `47`.

Regression:
- `MixerDockInstrumentedTest` adds centered-header and section width/order assertions while retaining touch/slider/large-font behavior;
- `StudioMixerTabletInstrumentedTest` keeps five managed-audio channels, complete/minimum density checks and captures RC27-named screenshot artifacts.

Documentation:
- live visual system, Mixer contract, workspace guidelines, requirements, decisions, test policy, CI pipeline, documentation map, current state and README are updated in the same source candidate;
- D-103 records the durable card-hierarchy decision.

## Deterministic source chain

A documentation-only post-sign RC26 commit occurred after the RC26a terminal materializer. RC27 therefore first seals that truthful signed-RC26 documentation delta as `materialize_ci_sources_rc26b.py`, then applies the RC27 runtime/tests/docs patch through `materialize_ci_sources_rc27.py`. Earlier payloads are not rewritten.

## Qualification gate

Required before any RC27 digital PASS claim:
- deterministic materialization and diff sanity;
- Unit Tests;
- Android Lint;
- debug/release assembly and unsigned provenance;
- API36 affected regression including Mixer component/large-font interaction;
- target-tablet five-channel geometry;
- visual inspection of RC27 complete/minimum screenshot artifacts.

Backend, Drive, Demucs and provider-real gates remain reusable because RC27 does not touch those paths. Signing is not requested until the exact RC27 source passes the digital gate and screenshot review. Physical acceptance remains separate.

## Qualification attempt #952

Android CI #952 / run `37034736534` on source `720ace0c76de20205ca6adde53d145fe4d867c8d` passed materialization, Unit Tests, Android Lint, build and API36 regression. The minimum-mode target-tablet screenshot and focused Mixer component screenshots visibly match the approved centered-header/soft-segmentation contract.

The required complete-mode target-tablet screenshot did not qualify as evidence: it captured the Studio shell while the loading spinner was still painted. The test had waited for project/waveform semantics, but not for the ViewModel `loading` flag to clear. Because RC27's own qualification contract requires reviewable complete and minimum screenshots, no digital PASS/signing claim is made from #952.

RC27a is test/evidence-only. It adds an explicit `!studio.state.value.loading` wait and verifies representative Master/waveform nodes before the complete capture, followed by Compose and Android window-idle synchronization. Runtime Mixer source, dimensions and behavior are unchanged.
