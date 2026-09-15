# Documentation Audit — Physical Review IV + H16 final polish

Updated: 2026-09-15

## Evidence progression
- CI #620: signed DIGITAL PASS through H11/H11a/H11b.
- CI #621: H14 diagnostic, software PASS / API36 21/22.
- CI #622: materializer infrastructure failure before compilation.
- CI #623: repaired materializer/software PASS / API36 21/22; isolated H14a gesture-lane issue.
- CI #624 / run `35005147318` / source `7858dca021a51e0e08835e3fa3f86e6d3b657215`: complete signed DIGITAL PASS through H12–H15/H14a, API36 **22/22** plus 1920×1200 geometry.

#624 therefore supersedes #620 as the last digitally homologated RC3 baseline.

## H12–H15 closure
H12 all-track levels, H13 Trim-ruler presentation, H14/H14a Mixer overflow with fixed MASTER and H15 resident Studio return are **DIGITAL PASS** at #624.

## H16 trigger
A subsequent physical screenshot/review exposed two final presentation issues plus one naming/layout clarity issue:
1. `Mixer` inside the Comparação mode group was ambiguous with the separate top-bar Mixer-panel button;
2. `Níveis` visually belonged in an adjustment category rather than inside comparison modes;
3. H13's Trim tick used a ruler width 8 dp narrower than the marker rail and increased the ruler to 46 dp, causing a slight left offset and a dedicated-looking vertical Trim lane.

## H16 decisions
### Practice bar
- comparison-neutral label is `Desativado`;
- top-bar Mixer panel toggle remains `Mixer`;
- Níveis moves to `Ajustes`;
- docked order is `Comparação | Ajustes | Timeline`;
- proportional weights are `0.34 / 0.16 / 0.50`.

### Trim geometry
- marker rail and ruler share one horizontal timeline geometry;
- ruler right padding shrink is removed;
- rail height is centralized at 62 dp;
- ruler returns to 20 dp;
- rail and ruler have zero interstitial spacing;
- T1/T2 lines span the existing rail+ruler and labels overlay the existing rail with elevated z-order;
- no dedicated Trim lane is allowed;
- waveform handles remain the edit controls.

## H16 regression contracts
`AutoSectionsSlotInstrumentedTest` now requires the three-segment bar, `Desativado`, Ajustes/Níveis and proportional widths.

`PhysicalEditingHardeningInstrumentedTest` now requires:
- marker rail and ruler to be directly adjacent;
- ruler height to remain compact relative to the marker rail;
- T1 X to equal the marker-rail timeline projection after a real Trim-handle drag.

## H16 source validation
Patch archive: `.source-parts/H16FinalUiTrimOverlay.patch.gz`

Decoded patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

Against the exact #624 materialized source:
- `patch --dry-run -p1`: PASS;
- forward application: PASS;
- reverse dry-run after application: PASS;
- `git apply --check`: PASS;
- `git diff --check`: PASS;
- changed Kotlin files: no parser-level syntax errors detected in the available local parser pass.

Expected final blobs:
- `StudioPlaceholderScreen.kt` `248ec396a80368486f91b29eb54e43e3f874c276`
- `TimelineMarkerRail.kt` `da3137a7e7b8481d6341cf212453ff0352c5bacd`
- `AutoSectionsSlotInstrumentedTest.kt` `e0682666a7f6662437e6874200f15de17c87aa0a`
- `PhysicalEditingHardeningInstrumentedTest.kt` `f1f629a3b56fc62d1bc3d357486077560140e256`
- `StudioUserGuideDialog.kt` `bb82787f3ff2009c0d1a5781b3961747ebebcdd5`

## Current evidence boundary
- Through H15/H14a: **DIGITAL PASS #624**.
- H16: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

H16 becomes DIGITAL PASS only after one new manually dispatched exact-source workflow passes software/Lint/build/provenance, API36 full regression, isolated tablet geometry and signed homologation. CI remains user-dispatched only.
