# Android CI / release pipeline

Updated: 2026-09-14

## Purpose

`.github/workflows/android-ci.yml` is the authoritative Android quality and homologation pipeline for GuitarLab Studio. It is intentionally manual-only (`workflow_dispatch`). Commits do not trigger GitHub Actions automatically. Maintenance commits should retain `[skip ci]`; the operator decides when a candidate is worth the full gate.

The pipeline optimizes the critical path without weakening coverage: the software gate and API 36 integration gate run in parallel, while the final job signs the exact release artifact already compiled by the software gate instead of rebuilding the application after both gates finish.

## Job graph

### 1. Software gate — `software-gate`

This job:
- checks out the exact workflow SHA with two commits of history;
- runs `git diff --check HEAD^ --`;
- provisions only Android platform/build packages needed for compilation;
- materializes split sources with `scripts/materialize_ci_sources.sh`;
- publishes the exact source snapshot used by the gate;
- validates `applicationId`, `versionName` and `versionCode`;
- runs the complete JVM/unit regression suite;
- captures the existing performance evidence;
- runs Android Lint;
- builds the debug APK;
- when signed homologation is requested, also builds an **unsigned release APK** from the same materialized source tree;
- proves the staged release is unsigned, validates its package/version with `aapt2`, records its SHA-256 and source SHA, and uploads it as a short-lived intermediate artifact.

Unit tests and Lint stay as separate workflow steps so a failure remains immediately attributable even though the same Gradle daemon can be reused inside the job.

### 2. API 36 integration gate — `android-integration-gate`

This job runs independently and in parallel with the software gate. It:
- repeats diff sanity and source materialization independently;
- provisions API 36 emulator dependencies;
- restores the current API 36 AVD snapshot cache;
- boots one emulator and reuses it for both instrumentation passes;
- runs the complete `connectedDebugAndroidTest` regression;
- archives those default-geometry reports;
- switches the same emulator to `1920x1200`, density `240`, fixed rotation;
- runs only `TargetTabletGeometryInstrumentedTest` as the isolated tablet-geometry pass;
- archives both default and target-geometry diagnostics.

The API 36 AVD cache is intentionally retained. The pre-optimization successful run restored about 1.4 GB in roughly 22 seconds, but the available comparable runs did not provide a controlled cache-miss baseline. Removing it without a measured cache-miss comparison would be theory-driven optimization.

Gradle state is read-only in this job. The pre-optimization baseline restored about 522 MB of useful Gradle state but then spent roughly 9 seconds saving about 238 MB and contended with the parallel software job for some cache keys. The integration gate therefore consumes existing cache state but does not compete to publish duplicate state.

### 3. Signed homologation — `homologation-apk`

This job runs only when `signed_homologation=true` and only after **both** mandatory gates pass.

It does not invoke Gradle and does not rebuild source. Instead it:
- downloads the exact unsigned release artifact produced by `software-gate`;
- verifies its saved SHA-256 and release-build identity against the current `github.sha`, package and version;
- restores the private signing bundle from GitHub Secrets;
- masks password material before use;
- aligns the staged release with build-tools `zipalign`;
- signs it with the official homologation key using APK Signature Scheme v2;
- verifies ZIP alignment, APK signature, locked certificate SHA-256, package, `versionName`, and `versionCode`;
- records both unsigned and signed SHA-256 values plus the exact source SHA in `BUILD_IDENTITY.txt`;
- publishes the signed APK, identity file and `SHA256SUMS.txt`;
- destroys the temporary signing material even if a later step fails.

This preserves source-to-binary provenance while removing the previous late `assembleRelease` recompilation. Signing transforms the already-built release package; it does not compile another source tree.

## Source materialization

`.source-parts/` and `scripts/materialize_ci_sources.sh` remain part of the build contract. Materialization happens before every job that compiles or tests project source. The script remains deterministic/idempotent and is not bypassed by the optimized release path.

The signing job does not materialize sources because it performs no compilation. It verifies the staged binary against the source SHA and build identity emitted by the software gate.

## SDK provisioning and infrastructure retry

`android-actions/setup-android` is used only to establish current command-line tools and environment paths; its default `tools platform-tools` payload is disabled with `packages: ''`.

`scripts/ci_install_sdk_packages.sh` installs only packages requested by each job and retries `sdkmanager` infrastructure failures up to three times with a short backoff. Tests are never retried automatically. The integration job alone provisions Emulator/system-image dependencies; the signing job installs only the release build-tools needed by `zipalign`, `aapt2` and `apksigner`.

## Caching

- The software gate remains the normal writable Gradle-cache producer.
- The API 36 gate restores Gradle cache read-only to eliminate duplicate save/cleanup work and parallel cache-write contention.
- The API 36 AVD snapshot cache remains enabled pending a measured cache-hit versus cache-miss experiment.
- Configuration cache is not enabled because compatibility has not yet been demonstrated for this project.
- No cache is used by the final signing job because it no longer runs Gradle.

## Artifacts and diagnostics

Important artifacts are namespaced by `github.sha` so evidence from different candidates cannot be confused:
- exact source snapshot;
- debug APK and software reports;
- unsigned release candidate plus its checksum/identity;
- API 36 instrumentation reports and copied default/tablet-geometry diagnostics;
- final signed homologation APK plus `BUILD_IDENTITY.txt` and `SHA256SUMS.txt`.

A gate failure should be read from the failing named step first, then from its uploaded reports/artifacts when present. The final signed artifact is eligible for physical homologation only if all required upstream jobs and every signing/identity verification pass.

## Pre-optimization baseline

Baseline used for this refactor:
- manual workflow run: **#611**;
- source SHA: `02fd4f71a6dfb893466b2b6b345b90f98496525c`;
- result: all mandatory jobs PASS;
- end-to-end duration: about **6m25s**;
- API 36 gate: about **3m34s**;
- signed homologation job: about **2m36s**;
- late `assembleRelease` inside the signed job: about **1m53s**.

The primary optimization moves that release compilation into the already-parallel software gate and leaves the serial final job as signing/verification only. Additional improvements remove default legacy SDK-tools/Emulator installation from jobs that do not need it, reuse the per-job Gradle daemon, remove the redundant standalone instrumentation compile invocation, and avoid integration-cache writes.

## Post-optimization evidence
Manual run **#613** on `db5a4208848e4b6ca2163ce715d0c5bb464cfe37` passed all three jobs end-to-end in about **3m01s**. Run #612 had already proven the full API 36 suite itself green, but its geometry step failed because a multiline shell continuation was interpreted by `android-emulator-runner` as a literal Gradle task named `\`; that workflow-only defect was corrected and #613 verified the fix.

The optimized architecture is therefore measured, not theoretical: release compilation is reused from the warm software job, the signing job does not invoke Gradle, integration cache is read-only, and the same emulator is reused for full API 36 plus isolated tablet geometry.

## Manual execution

To create the next signed homologation candidate:

1. Open the repository on GitHub and select **Actions**.
2. Select **GuitarLab Android CI**.
3. Choose **Run workflow**.
4. Select branch `main`.
5. Enable **Build signed homologation APK** (`signed_homologation=true`).
6. Start the workflow manually.
7. Confirm that software, API 36 integration, and signed homologation jobs all pass before downloading the final artifact.

Do not treat a green job from another SHA as evidence for the active candidate. `github.sha`, the unsigned release identity, `BUILD_IDENTITY.txt`, `SHA256SUMS.txt`, version metadata and signer fingerprint must agree for the same workflow run.
