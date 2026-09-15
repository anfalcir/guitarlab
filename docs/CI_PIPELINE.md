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
`H1 trim → H2 lineage/delete → H3 drag transaction → H4 timing → H5 waveform → H6 integrated regression/guide → H7 state/level/transport → H8 workspace flow → H9 waveform spatial stability → H10 race closure/guide → H11 mixer/waveform/metering → H11a synchronous Trim entry`.

H11 is represented by `.source-parts/H11MixerWaveformMetering.patch.gz`. H11a is represented by `.source-parts/H11TrimEntryRaceFix.patch` and is applied immediately after H11.

Requirements for the canonical CI path:
- deterministic one-pass output for the same repository SHA;
- fail-fast drift detection;
- no materialization bypass in compile/test jobs;
- API 36 first-tap Trim regression must remain enabled; do not mask failures by increasing timeout or weakening assertions.

## Artifacts
Artifacts remain namespaced by `github.sha`: exact source snapshot, debug/software reports, unsigned candidate + identity, API36 reports/diagnostics, and signed APK + identity/checksums.

## Evidence history
- CI #615: full signed pre-H0–H6 baseline PASS.
- CI #616: H0–H6 digital PASS.
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: H0–H10 software + API36 + tablet geometry + signing PASS. Signed APK SHA-256 `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.
- CI #618 / run ID `34922529980` / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software gate PASS; API 36 integration FAIL on one test, `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`; signed homologation correctly skipped. Root cause was the deferred `pendingTrimClipId` + `LaunchedEffect` Trim dispatch. H11a removes that race.

## Manual execution
To create the corrected H11/H11a signed homologation candidate:
1. GitHub → Actions → **GuitarLab Android CI**.
2. Run workflow on branch `main` only after source/docs consolidation.
3. Enable `signed_homologation=true`.
4. Confirm all three jobs PASS.
5. Confirm run `head_sha` equals the intended final `main` HEAD.
6. Verify APK/`SHA256SUMS.txt`/`BUILD_IDENTITY.txt` package, version, source SHA and signer all agree.

Do not rerun #618 as evidence for H11a and do not dispatch CI automatically.
