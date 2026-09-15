# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed **DIGITAL PASS**: CI #624 / run `35005147318` / source `7858dca021a51e0e08835e3fa3f86e6d3b657215`.
- #624 signed APK SHA-256: `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`.
- #624 unsigned APK SHA-256: `6dd4fb64803e161466f465f385a728ab67bc539ebd9d13307a195461a5bfbb08`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`.

## Evidence boundary
CI #624 remains the authoritative signed baseline through H15/H14a. A new final polish block, **H16**, is implemented after that baseline and is currently **SOURCE-VALIDATED / PRE-GATE**. Until one new manually dispatched exact-source workflow passes, H16 must not be described as DIGITAL PASS and the #624 APK remains the last digitally homologated binary.

## CI #624 — canonical baseline
The exact #624 source passed:
- source materialization;
- JVM/unit/core/audio/DSP/persistence/migration regression;
- performance evidence;
- Android Lint;
- debug + release build and unsigned provenance;
- API 36 connected instrumentation: **22/22 PASS**;
- isolated 1920×1200 tablet geometry;
- signed homologation;
- package/version/source/signer/checksum validation.

## H16 — final UI / Trim overlay polish — SOURCE-VALIDATED / PRE-GATE

### H16.A — practice bar semantics and balance
- The comparison mode backed by `GuitarAuditionMode.MIXER` is now labeled **`Desativado`**, clarifying that comparison emphasis is off.
- The independent top-bar button that opens/closes the Mixer panel remains named **`Mixer`**.
- `Níveis` leaves the Comparação segment and moves to a dedicated center segment **`Ajustes`**.
- The docked bar becomes **Comparação | Ajustes | Timeline**.
- Segment widths are deliberately proportional (`0.34 / 0.16 / 0.50`) rather than equal thirds so Ajustes stays compact and Timeline retains room for longer actions.
- Compact and non-docked layouts preserve the same logical ordering.
- `AutoSectionsSlotInstrumentedTest` now verifies the three-segment contract, `Desativado`, `Ajustes`, the Níveis action and the proportional geometry.

### H16.B — Trim T1/T2 on the real timeline geometry
The physical screenshot exposed two H13 presentation defects: the yellow tick was slightly left of the real timeline point, and H13 had enlarged the ruler into a de facto dedicated Trim lane.

H16 corrects both structurally:
- T1/T2 now use the same effective horizontal width as `TimelineMarkerRail`; the old ruler-only `end = 8.dp` shrink is removed.
- Marker-rail height is centralized as `TimelineMarkerRailHeight = 62.dp` and reused by the workspace header.
- The time ruler returns from `46.dp` to a compact `20.dp`.
- Marker rail and ruler are grouped with zero spacing, removing the artificial lane/gap while preserving the normal workspace gap below the ruler.
- Net reserved header height is reduced by 30 dp relative to H13 (`46 + 4` → `20 + 0`).
- While Cut is active, each yellow T1/T2 line spans the existing marker rail plus ruler and renders above playhead/loop content; precise labels are overlaid inside the existing rail instead of allocating vertical space.
- Nearby labels still separate vertically.
- Waveform Trim handles remain the actual editing controls and are not covered by the ruler overlay.
- `PhysicalEditingHardeningInstrumentedTest` now asserts direct rail→ruler adjacency, compact ruler height and T1 X alignment against the canonical marker-rail timeline fraction after a real handle drag.

## H16 source evidence
Source part: `.source-parts/H16FinalUiTrimOverlay.patch.gz`

Decoded patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

Validated against the exact materialized source artifact emitted by CI #624:
- `patch --dry-run -p1`: **PASS**;
- forward patch application: **PASS**;
- reverse dry-run after application: **PASS**;
- `git apply --check`: **PASS**;
- `git diff --check`: **PASS**;
- changed Kotlin files: no parser-level syntax errors detected with local `kotlinc` parsing pass.

Expected post-H16 materialized blobs:
- `StudioPlaceholderScreen.kt`: `248ec396a80368486f91b29eb54e43e3f874c276`
- `TimelineMarkerRail.kt`: `da3137a7e7b8481d6341cf212453ff0352c5bacd`
- `AutoSectionsSlotInstrumentedTest.kt`: `e0682666a7f6662437e6874200f15de17c87aa0a`
- `PhysicalEditingHardeningInstrumentedTest.kt`: `f1f629a3b56fc62d1bc3d357486077560140e256`
- `StudioUserGuideDialog.kt`: `bb82787f3ff2009c0d1a5781b3961747ebebcdd5`

Local Android compilation/runtime is not claimed because this environment does not contain the project Gradle wrapper/Android SDK stack. The next canonical manual CI remains the authority for Android compilation, API36 runtime geometry and signing.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H15/H14a: **DIGITAL PASS** at #624.
- H16: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

## Next authoritative gate
Manually dispatch **GuitarLab Android CI** on the exact then-current `main` SHA with `signed_homologation=true`.

Required PASS:
1. source materialization including H16;
2. full software/unit/audio/DSP/persistence/migration regression;
3. Android Lint + debug/release assembly + provenance;
4. API36 full connected instrumentation, including updated practice-bar and Trim geometry assertions;
5. isolated 1920×1200 geometry;
6. signed homologation with exact package/version/source/signer/checksum agreement.

No assistant-triggered workflow or rerun is permitted.

## Residual physical closure after that PASS
If the new gate is fully green, the resulting signed APK becomes the intended final physical baseline. Manual review should then focus only on real-device facts: visual harmony of `Comparação | Ajustes | Timeline`, natural T1/T2 overlay/readability while crossing playhead/loop, Mixer swipe feel with fixed MASTER, Studio-return flicker, and MK300 routing/REC/meters/synchronization/listening.
