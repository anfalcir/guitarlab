# Android CI / release pipeline

Updated: 2026-09-14

## Purpose
`.github/workflows/android-ci.yml` is the authoritative Android quality and homologation pipeline for GuitarLab Studio. It is intentionally manual-only (`workflow_dispatch`). Commits do not trigger GitHub Actions automatically. Maintenance commits retain `[skip ci]`; the operator decides when a candidate is worth the full gate.

The pipeline optimizes the critical path without weakening coverage: software and API 36 integration gates run in parallel, while the final job signs the exact release artifact already compiled by the software gate instead of rebuilding after both gates finish.

## Job graph
### 1. Software gate — `software-gate`
This job:
- checks out the exact workflow SHA with two commits of history;
- runs `git diff --check HEAD^ --`;
- provisions only Android platform/build packages needed for compilation;
- materializes split/versioned source deltas with `scripts/materialize_ci_sources.sh`;
- publishes the exact source snapshot used by the gate;
- validates `applicationId`, `versionName` and `versionCode`;
- runs the complete JVM/unit regression suite;
- captures performance evidence;
- runs Android Lint;
- builds debug APK;
- when signed homologation is requested, builds one **unsigned release APK** from the same materialized source tree;
- proves that staged release is unsigned, validates package/version with `aapt2`, records SHA-256/source SHA and uploads it as a short-lived intermediate artifact.

Unit tests and Lint stay separate workflow steps so failures remain attributable while reusing the warm Gradle process/cache inside the job.

### 2. API 36 integration gate — `android-integration-gate`
This independent parallel job:
- repeats diff sanity and source materialization;
- provisions API 36 emulator dependencies;
- restores the API 36 AVD snapshot cache;
- boots one emulator and reuses it for both instrumentation passes;
- runs complete `connectedDebugAndroidTest` regression;
- archives default-geometry reports;
- switches the same emulator to `1920x1200`, density `240`, fixed rotation;
- runs isolated tablet-geometry instrumentation;
- archives default/target-geometry diagnostics.

Gradle state is read-only in this job to avoid duplicate cache publication/parallel write contention. The AVD snapshot cache remains enabled because controlled cache-miss evidence has not justified removing it.

### 3. Signed homologation — `homologation-apk`
Runs only when `signed_homologation=true` and both mandatory gates pass.

It does not invoke Gradle or rebuild source. It:
- downloads the exact unsigned release artifact produced by software-gate;
- verifies stored checksum and release identity against current `github.sha`, package and version;
- restores private signing material only inside the runner;
- masks password material;
- aligns with `zipalign`;
- signs with official homologation key using APK Signature Scheme v2;
- verifies alignment, signature, locked certificate SHA-256, package, `versionName`, `versionCode`;
- records unsigned/signed SHA-256 values and exact source SHA in `BUILD_IDENTITY.txt`;
- publishes APK + `BUILD_IDENTITY.txt` + `SHA256SUMS.txt`;
- destroys temporary signing material even on failure.

## Source materialization contract
`.source-parts/` and `scripts/materialize_ci_sources.sh` are part of the build contract. Materialization happens before every job that compiles/tests source; signing does not materialize because it performs no compilation.

The post-615 physical hardening sequence is deliberately serial and bisectable:

`H1 trim → H2 clip lineage/delete → H3 drag transaction → H4 recording timing → H5 live waveform → H6 integrated regression → H6 waveform test fix → H6 user-guide sync`.

Requirements:
- deterministic output for the same repository SHA;
- idempotence-oriented behavior: already-applied patches are recognized rather than duplicated;
- fail-fast drift detection: a patch that is neither applicable nor already applied fails the gate;
- no bypass of materialization in software/API36 jobs.

This ordering is intentional because H4 and H5 both observe recording frames; separating them preserves diagnosability.

## SDK provisioning and infrastructure retry
`android-actions/setup-android` establishes command-line tools/environment. `scripts/ci_install_sdk_packages.sh` installs only job-specific packages and retries `sdkmanager` infrastructure failures up to three times with short backoff. Tests are never retried automatically.

## Caching
- software gate is the normal writable Gradle-cache producer;
- API 36 gate restores Gradle cache read-only;
- AVD snapshot cache stays enabled pending controlled evidence;
- configuration cache remains disabled until project compatibility is demonstrated;
- final signing job uses no Gradle cache because it does not run Gradle.

## Artifacts and diagnostics
Artifacts are namespaced by `github.sha`:
- exact source snapshot;
- debug APK/software reports;
- unsigned release candidate + checksum/identity;
- API 36 instrumentation reports/default/tablet diagnostics;
- final signed homologation APK + `BUILD_IDENTITY.txt` + `SHA256SUMS.txt`.

A failed named step is authoritative. Final signed artifact is eligible for physical homologation only if all required upstream jobs and signing/identity verification pass for the same SHA.

## Performance/evidence history
### Pre-optimization baseline
Run #611 at `02fd4f71a6dfb893466b2b6b345b90f98496525c` passed in about **6m25s**. The largest avoidable serial cost was late `assembleRelease` in the signed job (~1m53s).

### Optimized architecture validation
Run #613 at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37` passed all three jobs end-to-end in about **3m01s**, proving release compilation reuse, no-Gradle signing, read-only integration cache and emulator reuse.

Run #614 later failed only because a new Compose test used an unavailable test API (`assertDoesNotExist`); the application/emulator architecture was not the cause and signing was correctly blocked.

Run #615 at `74bf86efbec94d249c4968c3284bf1985cd66b44` then passed the complete canonical matrix again, including software, API 36 full regression, isolated 1920×1200 geometry and signed homologation. Its signed APK SHA-256 is `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`; signer fingerprint matched the locked GuitarLab certificate.

#615 is the latest fully green signed **baseline**. The current source is newer because H0–H6 editing/recording hardening was implemented afterward and must receive its own exact-source workflow before promotion.

## Manual execution
To create the next signed homologation candidate:
1. GitHub → **Actions** → **GuitarLab Android CI**.
2. **Run workflow** on branch `main`.
3. Enable **Build signed homologation APK** (`signed_homologation=true`).
4. Start manually.
5. Confirm software, API 36 integration and signed homologation jobs all pass.
6. Confirm workflow `head_sha` equals the intended final `main` HEAD.
7. Verify APK/`SHA256SUMS.txt`/`BUILD_IDENTITY.txt` package, version, source SHA and signer all agree.

Do not treat a green run from another SHA as evidence for the active candidate.
