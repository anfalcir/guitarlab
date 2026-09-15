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
`H1 trim → H2 lineage/delete → H3 drag transaction → H4 timing → H5 waveform → H6 integrated regression/guide → H7 state/level/transport → H8 workspace flow → H9 waveform spatial stability → H10 race closure/guide → H11 mixer/waveform/metering → H11a synchronous Trim entry → H11b waveform-selection semantics isolation → H12 all-track levels → H13 Trim ruler → H14 Mixer overflow → H14a viewport-safe swipe regression → H15 resident Studio return`.

Physical Review IV source parts, in required order:
- `.source-parts/H12LevelEngine.patch`
- `.source-parts/H12LevelUi.patch`
- `.source-parts/H13TrimRuler.patch`
- `.source-parts/H14MixerHorizontalScroll.patch`
- `.source-parts/H14aMixerScrollViewportRegression.patch`
- `.source-parts/H15ResidentStudioReturn.patch`

Requirements for the canonical CI path:
- deterministic one-pass output for the same clean repository SHA;
- fail-fast/fail-closed drift detection;
- no materialization bypass in compile/test jobs;
- no reverse-application of an earlier patch through later source states;
- API36 first-tap/independent Trim-handle regression stays enabled;
- H12 all-track level modal regression stays enabled;
- H13 fixed Trim-ruler markers stay enabled;
- H14/H14a overflow physical-swipe + fixed MASTER regression stays enabled and is viewport-independent;
- H15 same-project lifecycle/resident-state regression stays enabled;
- waveform-selection and H11 feature tests stay enabled;
- do not mask interaction failures by increasing timeout, using `scrollToItem`, unmerged-tree bypasses or weakening assertions.

`apply_patch_once` accepts either a clean forward application or a reverse dry-run indicating that exact patch is already present. Overlapping historical stages use explicit final-blob guards. Unexpected base/final hashes fail closed.

## Artifacts
Artifacts remain namespaced by `github.sha`: exact source snapshot, debug/software reports, unsigned candidate + identity, API36 reports/diagnostics, and signed APK + identity/checksums.

## Evidence history
- CI #615: full signed pre-H0–H6 baseline PASS.
- CI #616: H0–H6 digital PASS.
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: H0–H10 software + API36 + tablet geometry + signing PASS.
- CI #618 / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: diagnostic software PASS / API36 FAIL / signing skipped.
- CI #619 / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`: diagnostic software PASS / API36 FAIL / signing skipped.
- **CI #620 / run `34924500870` / source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`: canonical H0–H11 PASS.** Software, API36, unchanged Trim regression, tablet geometry and signed homologation all passed.
- **CI #621 / run `34998393778` / source `afecde0efd4d22e58115eaedadf60eea0eb3615c`: Physical Review IV diagnostic.** Software/Lint/build/provenance PASS; API36 21/22 PASS; only failure was the H14 overflow regression because it assumed four swipes suffice on every viewport; signing skipped. H14a removes that viewport assumption while preserving real swipe and fixed-MASTER assertions.
- **CI #622 / run `35000635666` / source `5832c6800a7523a0bed0b64b9400a00c1fa876c2`: materialization infrastructure FAIL.** Both software and API36 jobs stopped at `Materialize split source` before compilation/tests with `line 167: unexpected EOF while looking for matching '"'`; signing skipped. Root cause was a truncated materializer, not H12–H15 functional behavior.
- **CI #623 / run `35003025673` / source `973ecae78ee3c159e975b4b1d65a2f733aedf59a`: repaired-chain software PASS / API36 21/22 diagnostic.** Materialization, unit/performance, Lint, debug+release assembly and unsigned provenance passed. API36 failed only `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`; H12/H13/H15 instrumentation passed; signing was skipped. This isolated H14a v1's gesture lane rather than product/materializer failure.

## #622 materializer recovery + #623 H14a refinement
Root cause was identified as a file truncation inside the H7–H10 guard introduced while adding H14a. The repair:
- restores the complete canonical materializer tail;
- preserves H12, H13, H14, H14a and H15 as independent source parts;
- applies H14a strictly after H14 and before H15;
- retains patch-level reverse-match idempotence and explicit guarded final-blob checks;
- preserves fail-closed behavior on unexpected source drift;
- does not change `.github/workflows/android-ci.yml` or introduce automatic triggers.

Validation evidence for the repaired script:
- Git blob `de488110153b8a800b69b520a29860230bcb5838`;
- SHA-256 `612f171be1345b59e0f81e7f0e8cfbd78de0dcbc981fe4edcf22130b1b61779f`;
- `bash -n scripts/materialize_ci_sources.sh`: **PASS**;
- H12 → H13 → H14 → H14a → H15 serial dry-run/application from the exact #620 materialized source snapshot: **PASS**;
- `git diff --check`: **PASS**;
- post-#623 H14a v2 audited `MixerDockInstrumentedTest.kt` blob: `bf81aa9414a376679634f8ddf3f0b9bbf58fde5d`;
- H14a v2 patch SHA-256: `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`.

The whole historical materializer is intended to execute from a clean repository checkout. Re-running it against an already fully materialized #620 source artifact is not a meaningful whole-script idempotence test because earlier RC3 guards intentionally validate repository baselines. Supported idempotence is patch/guard scoped; clean-checkout determinism and fail-closed drift handling remain the canonical contract.

## CI #620 signed identity
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

## Physical Review IV evidence boundary
H12–H15/H14a are **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. #623 now proves the repaired chain passes materialization plus the complete software/Lint/build/provenance gate and reaches API36 with 21/22 passing. The newer H14a v2 gesture-lane change remains source-validated only until the next exact-source runtime gate.

## Next manual execution
After the repaired chain and documentation are consolidated on `main`:
1. GitHub → Actions → **GuitarLab Android CI**.
2. Select `main`.
3. Set `signed_homologation=true`.
4. Confirm the workflow SHA equals the final `main` SHA.
5. Require software, API36/geometry and signed homologation to PASS.
6. Verify APK/`SHA256SUMS.txt`/`BUILD_IDENTITY.txt` package, version, source SHA and signer all agree.

Do not rerun #620/#621/#622/#623 as substitute evidence for the revised H14a v2 candidate and do not dispatch CI automatically.
