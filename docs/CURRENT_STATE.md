# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: **CI #651** / run `35166195527` / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9`.
- #651 scope: H27 DIGITAL PASS.
- Signed APK SHA-256: `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.
- Signed APK size: `13,737,498` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Current corrective: **H28 IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.
- CI remains manual-only (`workflow_dispatch`).

## #651 retained digital evidence
- JVM/unit: **287/287 PASS**, 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS;
- `assembleDebug` + `assembleRelease` PASS;
- unsigned provenance PASS;
- API36: **32/32 standard PASS + 1/1 isolated 1920×1200 geometry PASS**;
- signed provenance/package/version/zipalign/signature/certificate PASS;
- signing cleanup PASS.

Do not combine standard API36 and isolated geometry counts in canonical reporting.

## Physical finding after #651
The H27 candidate corrected history maximum/idempotency semantics, but target-device cloud backup still showed false commit failures and selective duplicates:
- remote package was physically created while the app reported that it could not be confirmed;
- an unchanged second run could fail to see that revision in the provider listing and upload it again;
- the false failure could move between projects across runs.

The behavior points to an eventually-consistent directory listing being used as immediate write confirmation. That is not a valid durability contract for a cloud-backed document provider.

## H28 corrective
H28 implements three identity/integrity layers:
1. **projectId** — immutable `GuitarProject.id`; rename preserves it; duplicate/independent restore intentionally get new IDs;
2. **revisionId** — deterministic `r_<updatedAtEpochMs>_<stateDigest>`, binding edit time to canonical project state;
3. **package SHA-256** — exact byte-integrity identity.

Provider hardening:
- deterministic remote directory `v_<revisionId>`;
- direct-URI metadata/package/commit verification after write;
- targeted `(projectId, revisionId)` settling lookup before a retry upload;
- v1 H26/H27 metadata remains backward compatible;
- retention dedup uses revision identity + package hash instead of timestamp alone.

Full rationale: `H28_BACKUP_IDENTITY_CONSISTENCY.md`.

## H28 source validation
- clean H27 → H28 materialization PASS;
- final H28 Git blob verification PASS;
- idempotent second materializer run PASS;
- corrupted H28 source-part fails closed before source mutation;
- `git diff --check` PASS;
- materializer shell syntax PASS;
- focused Kotlin compilation checks PASS.

## Evidence boundary / next gate
CI #651 does **not** contain H28. Its APK must not be used to approve the corrected provider-consistency behavior.

Next step: the user manually dispatches one fresh signed `GuitarLab Android CI`. Audit the exact counts, terminal materializer message, artifacts/provenance and signed APK identity from that run. The assistant must not dispatch or rerun it without explicit user instruction.
