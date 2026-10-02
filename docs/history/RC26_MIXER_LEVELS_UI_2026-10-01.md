# RC26 — Studio Mixer / Níveis presentation refinement

Date: 2026-10-01
Status: RC26a SOURCE PRE-GATE

## Owner-approved contract

This correction is intentionally presentation-scoped. It does not change playback, recording, MAIN/CUE routing, level-analysis math, persistence or export semantics.

- Keep exactly three left-side Studio navbar actions in this order: Comparação, Timeline, Níveis.
- Níveis uses a gauge/speedometer icon that is not reused by the visible Mixer toggle or Options/settings controls.
- Níveis opens the existing all-tracks level-analysis modal directly.
- Remove the redundant Níveis text button from Master.
- Track and Master PK/RMS use the full useful inner strip width.
- Preserve CLIP reset by moving the complete-mode reset target to the strip header rather than reserving meter width.
- Keep the Master column at its existing width; make its volume slider consume the useful horizontal width.
- Place `VOL` and the dB readout together on one line below the Master slider.
- Preserve track width, Master width, 252/172 dp complete/minimum dock heights and absolute transport centering.

## Implementation

- `StudioWorkspaceBar` reserves equal 152 dp side slots so the added third left action does not shift the transport.
- `MixerDock` keeps Material Slider interaction semantics; only Master's presentation changes.
- Complete track/Master meter rows no longer reserve a permanent 48 dp CLIP column.
- Existing level-analysis dialog/state remains the single implementation.
- Candidate identity is `0.5.0-rc26` / versionCode `46`.

## Regression scope

Focused coverage must prove:

- fixed navbar geometry across comparison, Mixer mode and visibility changes;
- direct Níveis callback and narrow-row reachability;
- full-width track and Master meter geometry;
- wide Master volume geometry and visible one-line readout;
- existing gain/pan/CLIP interactions remain functional;
- five-channel tablet complete/minimum screenshots remain usable.

The exact source must pass the normal `[run ci]` software + API36 gate before any digital qualification claim. No signed RC26 artifact or physical acceptance is predeclared.

## Qualification attempts

- Android CI #948 / run `36942666882`: failed before build at `git diff --check` because this history file had Markdown trailing whitespace. Corrected without changing product source.
- Android CI #949 / run `36943179605`: unit tests passed. Lint failed on `MixerDock.kt` because the new Master slider track used `BoxWithConstraints` without reading its scope. API36 separately reached the Mixer regression and showed that `mixer-track-meters-modes` exists only in the unmerged semantics tree; the geometry test queried the merged tree.
- RC26a replaces only that unnecessary `BoxWithConstraints` with `Box` and makes the new geometry probes use `useUnmergedTree = true`. No layout dimension, control behavior, meter math, audio path or product contract changes.

The next gate must therefore re-run both Lint and API36 rather than treating either #948 or #949 as a qualification result.
