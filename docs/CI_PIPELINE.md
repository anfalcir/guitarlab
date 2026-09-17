# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs without explicit user instruction.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Current source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact SHA-256/Git blob checks.

Canonical tail: `… → H25 → H26 → H26a → H26b → H26e → H27`.

H27 input: `.source-parts/H27BackupReleaseUx.patch.gz.b64`.
Decoded gzip SHA-256: `2cbfa3bf7d3291778d73d5a3ffdf04f2e16ebf0c129a893f807d9cc548f53c05`.
Decoded patch SHA-256: `c80f0b04f34fb1b92ea47c13f7eb70df6744e0c07392e81391875f3b10aa85e5`.
Expected terminal message: `Source patch chain materialized through H27 with verified final hashes`.

H27 verifies final Git blobs for all 14 production/test files changed by the backup-history and release-copy corrective. It applies only after a valid H26e state. A second invocation is idempotent; a corrupted source-part fails before mutation.

## Last signed authority — CI #650
CI #650 / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` remains the last signed DIGITAL PASS **through H26/H26e only**.

Retained evidence:
- 285/285 JVM/unit PASS, 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS with 44 warnings + 3 hints, 0 errors;
- `assembleDebug` and `assembleRelease` PASS;
- unsigned provenance PASS;
- **31/31 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signing provenance and cleanup PASS.

Do not add standard API36 and isolated geometry counts together in canonical reporting.

## H27 gate requirement
H27 changes production source and tests after #650. Therefore #650 cannot prove H27.

The next manually dispatched signed run must prove:
- materializer reaches H27 verified final hashes;
- all existing + H27 JVM/unit tests PASS;
- Lint PASS;
- debug/release assembly and unsigned provenance PASS;
- standard API36 including updated Backup-screen release-copy assertions PASS;
- isolated geometry PASS;
- exact tested unsigned artifact is signed;
- package/version/signer/signature/zipalign/source provenance PASS;
- signing material cleanup PASS.

Actual counts must be taken from that run; do not predict or reuse #650 counts as H27 counts.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later source/docs HEAD never changes an earlier APK producer identity. Detailed #650 audit remains in `H26E_CI650_DIGITAL_PASS.md`; H27 rationale and source proof live in `H27_BACKUP_HISTORY_RELEASE_UX.md`.
