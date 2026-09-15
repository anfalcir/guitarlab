# Android CI / release pipeline

Updated: 2026-09-15

## Purpose
`.github/workflows/android-ci.yml` is the authoritative Android quality/homologation pipeline. It remains intentionally manual-only (`workflow_dispatch`); commits do not trigger GitHub Actions automatically.

Software and API 36 integration gates run independently where possible. The final signed job signs the exact unsigned artifact produced by the software gate; it does not rebuild source.

## Job graph
### 1. Software gate
- checkout exact workflow SHA;
- `git diff --check HEAD^ --`;
- materialize source with `scripts/materialize_ci_sources.sh`;
- upload exact source snapshot;
- validate app identity;
- run complete JVM/unit regression and performance evidence;
- Android Lint;
- build debug and one unsigned release candidate;
- validate unsigned release package/version/checksum/provenance and upload it.

### 2. API 36 integration gate
- repeat diff sanity/materialization;
- boot cached API 36 emulator;
- run complete connected instrumentation;
- run isolated 1920×1200 / density 240 tablet-geometry regression;
- upload integration diagnostics/reports.

### 3. Signed homologation
Runs only when `signed_homologation=true` and both mandatory upstream gates pass.
- download exact tested unsigned candidate;
- verify source/package/version/checksum;
- restore private signing material only in runner;
- align/sign using official homologation key;
- verify APK Signature Scheme v2, certificate, package/version;
- produce `BUILD_IDENTITY.txt` and `SHA256SUMS.txt`;
- upload signed artifact and destroy restored signing material.

## Source materialization contract
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract.

Canonical hardening order:
`H1 trim → H2 lineage/delete → H3 drag transaction → H4 timing → H5 waveform → H6 integrated regression/guide → H7 state/level/transport → H8 workspace flow → H9 waveform spatial stability → H10 race closure/guide → H11 mixer/waveform/metering → H11a synchronous Trim entry → H11b waveform-selection semantics isolation → H12 batch level analysis → H13 Trim time ruler → H14 Mixer overflow → H15 resident Studio re-entry → H16 integrated polish`.

Physical Review IV source parts:
- H12: `.source-parts/H12TrackLevelBatch.patch.gz`
- H13: `.source-parts/H13TrimTimelineRuler.patch.gz`
- H14: `.source-parts/H14MixerOverflow.patch.gz`
- H15: `.source-parts/H15StudioReentry.patch.gz`
- H16: `.source-parts/H16IntegratedPolish.patch.gz`

Requirements for the canonical CI path:
- deterministic one-pass output for the same repository SHA;
- fail-fast drift detection;
- no materialization bypass in compile/test jobs;
- no timeout increase/assertion weakening to mask interaction defects;
- retained independent Trim-handle and waveform-selection regressions stay enabled;
- H12 batch modal must prove explicit review/apply semantics and busy-state locking;
- H13 must expose T1/T2 on the fixed time ruler without replacing independent handles;
- H14 must prove track-strip horizontal movement with invariant MASTER position;
- H15 must prove same-project re-entry does not blank the resident Studio state.

## Artifacts
Artifacts remain namespaced by `github.sha`: exact source snapshot, debug/software reports, unsigned candidate + identity, API36 reports/diagnostics, and signed APK + identity/checksums.

## Evidence history
- CI #615: full signed pre-H0–H6 baseline PASS.
- CI #616: H0–H6 digital PASS.
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: H0–H10 software + API36 + tablet geometry + signing PASS.
- CI #618 / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software PASS; API36 FAIL on Trim-handle regression; signing skipped.
- CI #619 / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`: software PASS; H11 feature-specific integration PASS; API36 FAIL on the same Trim-handle regression; signing skipped.
- **CI #620 / run `34924500870` / source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`: canonical H0–H11 PASS.** Software, API36, unchanged Trim regression, tablet geometry and signed homologation all passed.

## CI #620 retained signed identity
- artifact: `GuitarLabStudio-0.5.0-rc3-homologacao`, artifact ID `10380000533`;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- source SHA: `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`;
- unsigned APK SHA-256: `14c4862371871cf6db85548bd6abc3405cdfcd278d726a5a9f097d533183bafd`;
- signed APK SHA-256: `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- APK Signature Scheme v2: true;
- signers: 1;
- key: RSA 4096;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Physical Review IV gate status
H12–H16 are IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE. They were serially reproduced against the exact #620 materialized source, but no Android workflow has yet run for them. Therefore #620 remains the last digitally homologated binary and must not be presented as evidence for H12–H16.

The next signed candidate requires a **new** manual workflow on the final H12–H16 `main` HEAD with `signed_homologation=true`. Do not rerun #620 as evidence for changed source.

## Current execution policy
No assistant-triggered workflow dispatches or reruns. Future signed candidates require the user's explicit manual dispatch. Documentation-only commits after a successful gate do not invalidate that binary, but source/test/materializer changes do require a new exact-source gate.
