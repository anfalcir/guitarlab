# Current State — GuitarLab Studio

Updated: 2026-10-01

## Status

**RC23 STUDIO NAVBAR / NARROW MIXER — SIGNED DIGITAL PASS / OWNER TABLET VALIDATION PENDING**

RC23 is integrated into canonical `main` and its exact signed APK is ready for the owner's tablet validation. RC20 remains the physically accepted frozen baseline in `RELEASE_BASELINE.md`. RC23 retains the RC22 MAIN/CUE implementation; residual physical acceptance of any simultaneous MAIN/CUE hardware combination remains separate from digital CI.

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

## Implemented behavior and digital evidence

- Comparison/Adjustments/Timeline open existing shared action families from stable slots in the transport navbar; no permanent practice row remains;
- enabling/disabling modes, closing/pinning Mixer and changing transport do not resize/reorder navbar slots;
- complete Mixer channels are 168 dp instead of 232 dp, using two rows of 48 dp Mute/Solo/CUE/REC targets;
- PK/RMS/peak hold/CLIP, full-width gain/pan sliders and the separate fixed Master are retained;
- visibility, pinning and expanded height persist independently; fixed/docked mode is the default;
- D-099, normative subsystem contracts and shared in-app help describe the new interaction;
- immutable RC23 materialization follows RC22; cold reconstruction, repeated execution, reverse application and terminal blob checks pass;
- CI #938 qualified the corrected branch source; canonical CI #939 passed software (unit tests, Lint, APK build), API36 regression and signed-artifact identity gates;
- corrected full-app tablet capture was inspected: five complete channels and separate Master are visible, the Mixer is fixed, and the practice row is absent. The prior loading-frame image is superseded;
- the temporary branch workflow trigger was retired before integration;
- full chronology/artifact evidence: `history/RC23_STUDIO_LAYOUT_QUALIFICATION_2026-10-01.md`.

## Remaining owner acceptance

Install the exact RC23 signed APK over the current same-package/signer installation. Check the existing project on SM-X230 / Android 16: five-channel readability, fixed Mixer preference after reopening, Comparison A/B use, Níveis, Timeline actions, gain/pan/Mute/Solo/CUE/Arm access, and ordinary playback/recording. Digital evidence does not predeclare physical ergonomics or simultaneous physical MAIN/CUE acceptance.

No recurring workflow monitoring is performed: the owner verifies completion and reports results. Documentation-only updates do not change the signed producer or artifact identity, and do not reopen the frozen Demucs backend.

## Predecessors

RC22 signed digital evidence: `history/RC22_DUAL_OUTPUT_CUE_QUALIFICATION_2026-10-01.md`. Accepted RC20 artifact/backend identity remains exclusively in `RELEASE_BASELINE.md` until explicit successor physical acceptance.
