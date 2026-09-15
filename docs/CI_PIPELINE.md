# Android CI / release pipeline

Updated: 2026-09-14

## Purpose
`.github/workflows/android-ci.yml` is the authoritative Android quality/homologation pipeline. It is intentionally manual-only (`workflow_dispatch`); commits do not trigger GitHub Actions automatically.

Software and API 36 integration gates run independently/parallel where possible. The final signed job signs the exact unsigned artifact produced by the software gate; it does not rebuild source.

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
`H1 trim → H2 lineage/delete → H3 drag transaction → H4 timing → H5 waveform → H6 integrated regression/guide → H7 state/level/transport → H8 workspace flow → H9 waveform spatial stability → H10 race closure/guide`.

H7–H10 are guarded as one final-state unit because later stages refine files touched earlier. The materializer checks final Git blob hashes for the H7–H10 target set; a second invocation recognizes the finished state rather than reverse-matching an intermediate patch.

Requirements:
- deterministic output for the same repository SHA;
- idempotence-oriented repeated invocation;
- fail-fast drift detection;
- no materialization bypass in any compile/test job.

## Artifacts
Artifacts remain namespaced by `github.sha`: exact source snapshot, debug/software reports, unsigned candidate + identity, API36 reports/diagnostics, and signed APK + identity/checksums.

## Evidence history
- CI #613: optimized full RC3 pipeline PASS.
- CI #614: Compose test API compatibility failure; signing correctly blocked.
- CI #615: full signed pre-H0–H6 baseline PASS.
- CI #616 / source `3051619c219e346daca00d2242f60ef03f2d80db`: H0–H6 software + API36 + tablet geometry + signing PASS. Signed APK SHA-256 `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`.
- H7–H10 are newer than #616 and are PRE-GATE until a new exact-source run passes.

## Manual execution
To create the next signed H7–H10 homologation candidate:
1. GitHub → Actions → **GuitarLab Android CI**.
2. Run workflow on branch `main` only after source/docs consolidation.
3. Enable `signed_homologation=true`.
4. Confirm all three jobs PASS.
5. Confirm run `head_sha` equals the intended final `main` HEAD.
6. Verify APK/`SHA256SUMS.txt`/`BUILD_IDENTITY.txt` package, version, source SHA and signer all agree.

Do not treat a green run from another SHA as evidence for the active candidate.