# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: **CI #650** / run `35154021384` / exact APK producer `07c99155789774cb39f9b4382829f9e1d16649e3`.
- #650 scope: H26/H26e DIGITAL PASS.
- Signed APK SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.
- Signed APK size: `13,737,498` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Current source correction: **H27 IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).

## Physical finding after #650
Target-device validation exposed that the H26 user contract was not release-ready:
- manual backup forced a new version even for an unchanged persisted revision;
- automatic backup was incremental, so an auto run followed by a manual run could protect the same revision twice;
- the `1/3/5/10` setting represented a protected minimum rather than the intended maximum history size;
- aggregate status did not identify a failing project clearly;
- normal user UI exposed implementation vocabulary and device-specific/development-oriented wording.

The visible screenshot alone cannot prove the exact timing/order of all remote writes, but the source defect above is deterministic and sufficient to reject #650 as the final backup candidate.

## H27 — PRE-GATE corrective
H27 implements:
- manual total/project backup with `force = false` so unchanged revisions are idempotent;
- bounded history using `maximumVersionsPerProject`;
- same-revision duplicate cleanup, preserving the newest copy;
- newest valid version always preserved;
- max-history enforcement even when age cleanup is disabled;
- migration of the legacy configured count into the new maximum-history setting;
- fail-safe cleanup rules retained for total failure, partial failure, cancellation and single-project scope;
- `Última execução` state plus per-project failure detail;
- end-user copy audit across backup/settings/help/readiness surfaces, removing internal API/release jargon and personal/model-specific status copy from normal product UI.

Full details: `H27_BACKUP_HISTORY_RELEASE_UX.md`.

## H27 source validation completed
- exact H26e materialized baseline → H27 patch application PASS;
- exact H27 patch SHA-256 `c80f0b04f34fb1b92ea47c13f7eb70df6744e0c07392e81391875f3b10aa85e5`;
- exact final Git blob verification for 14 changed production/test files;
- materializer idempotent rerun PASS;
- deliberately corrupted H27 source-part fails closed before source mutation;
- `git diff --check` PASS;
- materializer shell syntax PASS;
- focused backup-domain runtime harness PASS;
- release-surface static copy audit PASS for the targeted internal/personal terms.

## #650 retained evidence
The previous gate remains valid evidence for the source it tested:
- JVM/unit: **285/285 PASS**;
- performance evidence PASS;
- Android Lint PASS with 44 warnings + 3 hints, 0 errors;
- debug/release assembly and unsigned provenance PASS;
- API36 **31/31 standard + 1/1 isolated geometry**;
- signed provenance/package/version/zipalign/signature/certificate PASS;
- signing cleanup PASS.

It must **not** be reused as H27 evidence.

## Next gate
Run one fresh **manual** `GuitarLab Android CI` with signed homologation after H27 publication. Audit actual test counts, source materializer terminal message, artifacts and exact signed identity from that run. The assistant must not dispatch or rerun it without explicit user instruction.

Until that gate passes, do not continue final backup-history homologation on the #650 APK. Other #650 observations remain useful regression evidence, but final RC3 approval must be bound to the next signed H27 candidate and followed by the reduced real-device residual checklist.
