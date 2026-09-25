# Documentation map

Updated: 2026-09-24

## Authority rule

Repository documentation is divided into **live authority**, **normative subsystem contracts** and **historical evidence**.

Historical files intentionally preserve old candidate names, branches, hardware targets, gate language and statements such as “current” or “pending”. Those statements apply only to their dated context and never override the live authority set below.

## Live authority

Use these documents first for all new work:

1. `PROJECT_IDENTITY.md` — stable personal-appliance identity, support boundary, proportional quality, freeze and maintenance policy.
2. `CURRENT_STATE.md` — the only volatile ledger for current source/candidate/worker status and next action.
3. `DECISIONS.md` — durable decisions; later numbered decisions supersede earlier ones.
4. `PRODUCT_REQUIREMENTS.md` — supported product behavior.
5. `IMPLEMENTATION_ROADMAP.md` — current RC sequence and remaining milestones.
6. `U12_RC19_AUDIO_FORENSICS_AND_DIAGNOSTICS_PLAN.md` — active RC19→RC20 corrective plan until RC20 is physically accepted.
7. `TEST_AND_HOMOLOGATION_PLAN.md` — current automated vs physical qualification boundary.
8. `CANDIDATE_IDENTITY_POLICY.md` — exact-source/signing/provenance contract.
9. `CI_PIPELINE.md` — operational CI/signing/materialization contract.

`README.md` is the repository entry point and points back to this set; it is not a second volatile status ledger.

## Current release boundary

- RC19 (`0.5.0-rc19` / 39) is the latest signed digitally qualified candidate.
- RC19 is not physically accepted because a real Prepare run produced musically unusable audio.
- RC20 (`0.5.0-rc20` / planned versionCode `40`) is the corrective successor once its source is materialized and qualified.
- The locked signer remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- The current source-materialization entrypoint ends at **U12bh** via `scripts/materialize_ci_sources_u12bh.py`.
- Exact run IDs, source SHA, worker digest and next action belong in `CURRENT_STATE.md`.

## Normative subsystem contracts

These documents remain authoritative only when work touches their subsystem:

- `ARCHITECTURE.md`;
- `H37_DRIVE_V3_BACKUP.md` for the retained direct Drive v3 transport/security/integrity contract;
- `RECORDING_LATENCY_ARCHITECTURE.md` and `RECORDING_LATENCY_CONTRACT.md`;
- `MANAGED_MEDIA_POLICY.md`;
- `CODEC_SUPPORT_MATRIX.md` and `MEDIA_IO_SUPPORT_CLAIM_RULE.md`;
- `PROJECT_PORTABILITY_CONTRACT.md`;
- `TIMELINE_INTERACTION_GUIDELINES.md`;
- `STUDIO_WORKSPACE_GUIDELINES.md` and `STUDIO_OPTIONS_AND_MIXER.md`;
- `UI_VISUAL_SYSTEM.md`, `UI_COPY_STYLE.md`, `TRANSIENT_FEEDBACK_CONTRACT.md`, `USER_GUIDE_POLICY.md`;
- `SHARE_MODAL_CONTRACT.md`;
- `H28_BACKUP_IDENTITY_CONSISTENCY.md` for the retained backup identity/revision invariant.

A subsystem contract does not create a release gate for an unrelated change merely because the file exists.

## Historical/superseded evidence

Preserve for traceability, but do not use as current-state authority unless a live document explicitly cites a retained invariant:

- `U10_FINAL_DIGITAL_COHESION_GATE.md`;
- `U11_FINAL_DIGITAL_RELEASE_GATE.md`;
- `U11_SECURITY_DOCUMENTATION_AUDIT.md`;
- closed unified/cohesion milestone plans and audits;
- superseded `RELEASE_NOTES_*`;
- `RC14_FINAL_PHYSICAL_HOMOLOGATION.md` and older candidate/checklist files;
- `HISTORICAL_CANDIDATES.md`;
- all `ALPHA*`, `M4_*`, `M5_*`, `M5C_*`, `M6_*`, `M7_*` milestone/checkpoint/checklist files;
- dated documentation audits/consistency snapshots;
- `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md`;
- `M8_GLOBAL_DIGITAL_REGRESSION.md`;
- `GITLAB_CI_MIGRATION.md`;
- H/U milestone-specific closure reports already absorbed by the current line.

## Evidence reuse rule

Accepted evidence remains reusable unless a later source/backend/hardware change can materially invalidate it. A future change requalifies the affected path plus adjacent risk smoke, not the entire historical program by default.

## Producer identity rule

A documentation-only commit never retroactively changes an already-built APK or worker. Every promoted candidate remains bound to its exact producer SHA, APK hash, signer and, where applicable, immutable worker digest.
