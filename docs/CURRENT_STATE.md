# Current State — GuitarLab Studio

Updated: 2026-10-01

## Status

**RC24 STUDIO DENSITY CORRECTION — IMPLEMENTED / DIGITAL QUALIFICATION PENDING**

Owner video `141016.mp4` rejected RC23 layout ergonomics. RC24 corrects navbar/menus/Mixer density on a qualification branch; digital and physical results are not predeclared. RC20 remains the physically accepted frozen baseline in `RELEASE_BASELINE.md`. RC23 retains the RC22 MAIN/CUE implementation; residual physical acceptance of any simultaneous MAIN/CUE hardware combination remains separate from digital CI.

## Exact signed RC23 identity

- producer/merge SHA: `b39be540f56fa700338602408a7be92d9076e10b`;
- integrated PR: https://github.com/anfalcir/guitarlab/pull/9;
- signed authority: Android CI **#939 / run `36914477871`** — https://github.com/anfalcir/guitarlab/actions/runs/36914477871;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc23`; versionCode: `43`;
- delivered filename: `GuitarLabStudio-0.5.0-rc23-homologacao.apk`;
- signed APK bytes: `80133832`;
- unsigned APK SHA-256: `219f7c4f9c5323ec0879dedb8e07b80421b919e52a79aedf5f019669e38bfd12`;
- signed APK SHA-256: `d2630d935c255a543f122b5226edb96bf9bbe10badca6b02308c94fd8f95f3ee`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signing verifies APK Signature Scheme v2 and signs the exact tested unsigned release artifact without rebuilding;
- local downloaded APK checksum matches `SHA256SUMS.txt` and `BUILD_IDENTITY.txt`; certificate/package/version verification is evidenced by the successful signing job.

## RC24 implementation and pending qualification

- versionName `0.5.0-rc24`, versionCode `44`; no producer SHA or signed artifact declared before qualification;
- Comparison/Timeline vertical panels; balanced fixed navbar slots; Níveis in Master;
- fixed Mixer only; complete/minimum 252/172 dp, 200 dp channels, contiguous four-button row with 46 dp faces inside 48 dp targets;
- complete controls preserved; minimum retains all four buttons and gain, with shared complete details for pan/meter/clip reset;
- legacy visibility retained; new minimum preference defaults complete and persists independently;
- D-100 and subsystem/guide docs updated; immutable RC24 stage follows RC23;
- focused tests cover visual/target bounds, menu fit, minimum/details pan commits, preference upgrade and filled five-track tablet geometry/captures;
- local materialization/diff checks are recorded in `history/RC24_STUDIO_DENSITY_2026-10-01.md`; Android compile/Lint/instrumentation and screenshot review await CI evidence.

## Next action

Owner reports qualification workflow result. Then inspect completed evidence and filled-project screenshots, fix any failure, remove temporary branch trigger before canonical integration and qualify/sign the exact canonical candidate. Physical acceptance on SM-X230 / Android 16 remains required; RC20 stays accepted. RC23 digital PASS and delivered artifact identity above remain historical, not UX acceptance.

No workflow execution monitoring is performed. This Android UI correction does not reopen the frozen backend or change MAIN/CUE/audio algorithms.

## Predecessors

RC22 signed digital evidence: `history/RC22_DUAL_OUTPUT_CUE_QUALIFICATION_2026-10-01.md`. Accepted RC20 artifact/backend identity remains exclusively in `RELEASE_BASELINE.md` until explicit successor physical acceptance.
