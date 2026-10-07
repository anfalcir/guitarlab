# RC32 Communication Split — digital qualification

Updated: 2026-10-07
Status: **PASS / SIGNED / PHYSICAL REQUALIFICATION PENDING**

## Final producer

- commit: `8360b695b6df8b56bc480479d07d1eee6f1c6add`;
- commit intent: preserve the specific CUE suppression reason on top of the RC32 stabilization source;
- versionName: `0.5.0-rc32`;
- versionCode: `52`;
- package: `studio.guitarlab.app`.

## CI authority

Android CI **#972 / run `37612951851`** completed successfully on the exact producer SHA.

Blocking jobs:

- Unit tests + Lint + APK build: PASS;
- API 36 emulator regression: PASS;
- Signed homologation APK: PASS.

The software job validated exact source identity, unit tests, performance evidence, Android Lint, debug/release assembly and unsigned candidate provenance. The API36 job passed the representative instrumented regression. The signing job downloaded the tested unsigned candidate, signed without recompilation, verified package/version/certificate and uploaded the homologation artifact.

## Artifact identity

- unsigned APK SHA-256: `d02ecc5f91c6abef86dbc2a38296f45d4c7551187580461829926557494c682d`;
- signed APK SHA-256: `f8cdcd1183d8efd4448f17d911b1284143741f192a03bb3d78b17f78fc4ee0e2`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact: `GuitarLabStudio-0.5.0-rc32-homologacao`;
- artifact id: `11478632922`;
- artifact ZIP digest: `sha256:b264466155fc37daba632e14639e960fa1bd8f588168d39fa91490ebeb0e84a6`;
- artifact gate: `software+android-integration-passed;physical-validation-pending`.

The downloaded artifact was independently extracted after CI. The APK SHA-256 calculated from the extracted file exactly matches `signedApkSha256` in `BUILD_IDENTITY.txt` and `SHA256SUMS.txt`.

## Scope qualified digitally

RC32 changes only the affected Android CUE path and its evidence:

- Communication Split priority for communication-capable endpoints;
- removal of automatic global `MODE_IN_COMMUNICATION`;
- strategy-specific startup offset policy;
- bounded non-blocking CUE FIFO;
- sustained communication drift policy;
- reduced duplicate runtime preflight churn;
- specific runtime suppression diagnostics;
- product-facing CUE text and preservation of the specific failure reason;
- version/diagnostic/source guards and focused tests.

No cloud worker, Drive transport, project format or export pipeline is changed by this candidate.

## Residual physical requirement

Digital PASS cannot prove OEM/HAL route stability, post-mix fidelity or long-run dual-output usefulness. Owner-device requalification on SM-X230 + MK-300 + wired headset remains mandatory before RC32 can replace the accepted baseline.
