# Documentation Audit — Physical Review IV + H16 final polish

Updated: 2026-09-15

## Evidence progression
- CI #620: signed DIGITAL PASS through H11/H11a/H11b.
- CI #621: H14 diagnostic, software PASS / API36 21/22.
- CI #622: materializer infrastructure failure before compilation.
- CI #623: repaired materializer/software PASS / API36 21/22; isolated H14a gesture-lane issue.
- CI #624 / run `35005147318` / source `7858dca021a51e0e08835e3fa3f86e6d3b657215`: complete signed DIGITAL PASS through H12–H15/H14a, API36 **22/22** plus isolated 1920×1200 geometry.
- CI #625 / run `35010012582` / source `476fa740408130adf6a4e9665d166e724a9184dd`: complete signed DIGITAL PASS through **H16**, API36 **22/22** plus isolated 1920×1200 geometry.

CI #625 supersedes #624 as the active digitally homologated RC3 baseline because it contains H16 while retaining the earlier regressions.

## H12–H15 retained closure
H12 all-track levels, H13 Trim-ruler presentation, H14/H14a Mixer overflow with fixed MASTER and H15 resident Studio return remain **DIGITAL PASS** and were retained by the complete #625 regression.

## H16 trigger
A subsequent physical screenshot/review after #624 exposed two final presentation issues plus one naming/layout clarity issue:
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
- ruler right-padding shrink is removed;
- rail height is centralized at 62 dp;
- ruler returns to 20 dp;
- rail and ruler have zero interstitial spacing;
- T1/T2 lines span the existing rail+ruler and labels overlay the existing rail with elevated z-order;
- no dedicated Trim lane is allowed;
- waveform handles remain the edit controls.

## H16 regression contracts
`AutoSectionsSlotInstrumentedTest` requires the three-segment bar, `Desativado`, Ajustes/Níveis and proportional widths.

`PhysicalEditingHardeningInstrumentedTest` requires:
- marker rail and ruler directly adjacent;
- ruler height compact relative to marker rail;
- T1 X equal to the marker-rail timeline projection after a real Trim-handle drag;
- both Trim handles remain independently operable.

## H16 source validation
Patch archive: `.source-parts/H16FinalUiTrimOverlay.patch.gz`

Decoded patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

Before runtime gate, validation against exact #624 materialized source established:
- `patch --dry-run -p1`: PASS;
- forward application: PASS;
- reverse dry-run after application: PASS;
- `git apply --check`: PASS;
- `git diff --check`: PASS;
- changed Kotlin files had no parser-level syntax errors in the available local parser pass.

Expected H16 materialized blobs recorded before CI:
- `StudioPlaceholderScreen.kt` `248ec396a80368486f91b29eb54e43e3f874c276`
- `TimelineMarkerRail.kt` `da3137a7e7b8481d6341cf212453ff0352c5bacd`
- `AutoSectionsSlotInstrumentedTest.kt` `e0682666a7f6662437e6874200f15de17c87aa0a`
- `PhysicalEditingHardeningInstrumentedTest.kt` `f1f629a3b56fc62d1bc3d357486077560140e256`
- `StudioUserGuideDialog.kt` `bb82787f3ff2009c0d1a5781b3961747ebebcdd5`

## CI #625 — H16 runtime/signing closure
The user manually dispatched the canonical workflow on exact H16 source `476fa740408130adf6a4e9665d166e724a9184dd`.

Observed result:
- materialization including H16: PASS;
- software/unit/audio/DSP/persistence/migration/performance: PASS;
- Android Lint: PASS;
- debug + release assembly and unsigned provenance: PASS;
- API36 connected suite: **22 tests, 0 failures, 0 errors, 0 skipped**;
- isolated target geometry 1920×1200: PASS;
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`: PASS;
- retained `PhysicalEditingHardeningInstrumentedTest`: PASS;
- Mixer overflow/fixed MASTER and resident-Studio regressions: PASS;
- signed homologation: PASS.

Signed identity:
- package `studio.guitarlab.app`;
- versionName `0.5.0-rc3`;
- versionCode `23`;
- unsigned APK SHA-256 `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`;
- signed APK SHA-256 `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2, one RSA-4096 signer.

## Current evidence boundary
- Through H16: **DIGITAL PASS — CI #625**.
- M7 digital gate: complete; final physical closure pending.
- M8 RC3 hardening through H16: DIGITAL PASS.
- Exact physical candidate: the #625 signed APK from source `476fa740408130adf6a4e9665d166e724a9184dd`.

No further CI is needed unless product/source code changes. The residual authority is `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` on the real Samsung SM-X230 + M-VAVE MK-300. CI remains user-dispatched only.
