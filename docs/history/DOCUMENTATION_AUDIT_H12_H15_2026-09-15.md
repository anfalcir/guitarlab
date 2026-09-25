# Documentation Audit — Physical Review IV + H16/H17 final polish

Updated: 2026-09-15

## Evidence progression
- CI #620: signed DIGITAL PASS through H11/H11a/H11b.
- CI #621: H14 diagnostic, software PASS / API36 21/22.
- CI #622: materializer infrastructure failure before compilation.
- CI #623: repaired materializer/software PASS / API36 21/22; isolated H14a gesture-lane issue.
- CI #624 / run `35005147318` / source `7858dca021a51e0e08835e3fa3f86e6d3b657215`: signed DIGITAL PASS through H12–H15/H14a, API36 **22/22** plus isolated 1920×1200 geometry.
- CI #625 / run `35010012582` / source `476fa740408130adf6a4e9665d166e724a9184dd`: signed DIGITAL PASS through H16, API36 **22/22** plus isolated 1920×1200 geometry.
- CI #626 / run `35017084625` / source `f187ab2ba7596c4aa04d223f007409b2fb39f490`: signed DIGITAL PASS through **H17**, API36 **22/22** plus isolated 1920×1200 geometry **1/1**.

CI #626 supersedes #625 as the active digitally homologated RC3 baseline while retaining the previous regressions.

## H12–H16 retained closure
H12 all-track levels, H13 Trim-ruler presentation, H14/H14a Mixer overflow with fixed MASTER, H15 resident Studio return and H16 practice-bar/Trim-overlay polish remain DIGITAL PASS and were retained by the complete #626 regression.

## H17 trigger and decisions
Physical review of #625 exposed two remaining presentation defects:
1. numeric T1/T2 boxes were still visible and the CUT indicator visually occupied the wrong upper rail;
2. `Ajustes + Níveis` was left-anchored inside its dedicated segment, making it appear too close to Comparação.

H17 resolves these by:
- removing visible T1/T2 time-label boxes;
- rendering only short yellow CUT ticks inside the time ruler;
- preserving exact canonical timeline X projection;
- centering the `Ajustes + Níveis` content cluster inside the center segment;
- preserving the `Comparação | Ajustes | Timeline` structure.

## H17 regression contracts
`PhysicalEditingHardeningInstrumentedTest` requires:
- no visible `T1 ` / `T2 ` time labels;
- CUT ticks fully contained inside the time-ruler vertical bounds;
- exact X projection after a real Trim-handle drag;
- both Trim handles remain independently operable.

`AutoSectionsSlotInstrumentedTest` requires:
- Níveis fully contained within Ajustes;
- no overlap with neighboring segments;
- approximately symmetric left/right inset for the centered Ajustes+Níveis cluster.

## H17 source validation
Source part: `.source-parts/H17CutRulerPracticeSpacing.patch`

Patch SHA-256:
`38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`

Before runtime gate, exact #625 post-H16 materialized source passed forward/reverse patch checks, `git apply --check`, `git diff --check`, Kotlin parser scan and materializer `bash -n`.

## CI #626 — H17 runtime/signing closure
The user manually dispatched the canonical workflow on exact source `f187ab2ba7596c4aa04d223f007409b2fb39f490`.

Observed result:
- materialization through H17: PASS;
- software/unit/audio/DSP/persistence/migration/performance: PASS;
- Android Lint: PASS;
- debug + release assembly and unsigned provenance: PASS;
- API36 connected suite: **22 tests, 0 failures, 0 errors, 0 skipped**;
- isolated target geometry 1920×1200: **1 test, 0 failures, 0 errors, 0 skipped**;
- H17 practice-bar regression: PASS;
- H17 Trim/CUT regression: PASS;
- Mixer overflow/fixed MASTER and resident-Studio regressions: PASS;
- signed homologation: PASS.

Signed identity:
- package `studio.guitarlab.app`;
- versionName `0.5.0-rc3`;
- versionCode `23`;
- unsigned APK SHA-256 `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`;
- signed APK SHA-256 `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2, one RSA-4096 signer.

## Current evidence boundary
- Through H17: **DIGITAL PASS — CI #626**.
- M7 digital gate: complete; final physical closure pending.
- M8 RC3 hardening through H17: DIGITAL PASS.
- Exact physical candidate: the #626 signed APK from source `f187ab2ba7596c4aa04d223f007409b2fb39f490`.

No further CI is needed unless product/source code changes. The residual authority is `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` on the real Samsung SM-X230 + M-VAVE MK-300. CI remains user-dispatched only.
