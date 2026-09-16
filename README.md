# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/H26E_CI650_DIGITAL_PASS.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/H26_SAF_CLOUD_BACKUP.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

Current signed digital authority is **CI #650** / run `35154021384` / exact APK producer `07c99155789774cb39f9b4382829f9e1d16649e3`, with **H26/H26e DIGITAL PASS**.

Audited evidence: **285/285 JVM/unit PASS**; performance evidence PASS; Android Lint PASS (reports contain 44 warnings + 3 hints, 0 errors); debug/release assembly and unsigned provenance PASS; API36 **31/31 standard + 1/1 isolated 1920×1200 geometry**; signed homologation/provenance/cleanup PASS.

APK identity:
- unsigned SHA-256: `c536ba4af1b9fbe679caedd43d41e6e43a9f1c999301a8757f55a180acf4ce8d`;
- signed SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- signed size: `13,737,498` bytes;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- one RSA-4096 signer; APK Signature Scheme v2 verified, v1/v3/v3.1/v4 not used.

## H26 — SAF Cloud Backup
H26 is **DIGITAL PASS** at CI #650. It provides provider-neutral backup/restore through Android Storage Access Framework (SAF), including full/single-project backup, full/single-version restore, automatic incremental backup, configurable target folder, fail-safe retention, transactional `COMMITTED` versions and remote SHA-256/size verification.

Physical closure is still required on the Samsung SM-X230 with a real DocumentsProvider such as Google Drive. RC3 is therefore **not FINAL** yet.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. The canonical H26 tail is **`… → H25 → H26 → H26a → H26b → H26e`**. H26e deterministically installs the complete validated `BackupScreenInstrumentedTest.kt` and fails closed on source/hash drift.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. H26 uses user-selected SAF document-tree access and does not require broad Google Drive credentials or Drive API access.