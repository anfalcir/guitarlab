# Documentation map

Updated: 2026-09-25

## Authority rule

The root of `docs/` contains **live documentation only**. Completed development plans, release campaigns, milestone reports, audits, candidate checklists and superseded release notes belong under `docs/history/`.

Historical files intentionally preserve old candidate names, states and words such as “current”, “open”, “pending” or “release-blocking”. Those statements apply only to their dated context.

## Live authority — read first

1. `PROJECT_IDENTITY.md` — stable product identity, support boundary, freeze and maintenance triggers.
2. `CURRENT_STATE.md` — concise present-tense operational state.
3. `RELEASE_BASELINE.md` — immutable exact identity of the accepted APK/backend baseline.
4. `PRODUCT_REQUIREMENTS.md` — supported product behavior.
5. `ARCHITECTURE.md` — current technical boundaries.
6. `DECISIONS.md` — durable numbered decisions; later decisions supersede earlier conflicting ones.
7. `TEST_AND_HOMOLOGATION_POLICY.md` — proportional qualification/evidence-reuse policy for future maintenance.
8. `CANDIDATE_IDENTITY_POLICY.md` — exact-source/build-once/sign-exactly provenance contract.
9. `CI_PIPELINE.md` — operational CI/materialization/signing/backend controls.
10. `PRODUCT_VISION.md` — product intent and scope guardrail.

`README.md` is the repository entry point and links to this authority set. It is not a second release ledger.

## Normative subsystem contracts

These remain live only for their subsystem:

- `BACKUP_IDENTITY_CONTRACT.md`;
- `DRIVE_BACKUP_CONTRACT.md`;
- `MANAGED_MEDIA_POLICY.md`;
- `PROJECT_PORTABILITY_CONTRACT.md`;
- `CODEC_SUPPORT_MATRIX.md`;
- `MEDIA_IO_SUPPORT_CLAIM_RULE.md`;
- `OUTPUT_WORKFLOW_CONTRACT.md`;
- `RECORDING_LATENCY_ARCHITECTURE.md`;
- `RECORDING_LATENCY_CONTRACT.md`;
- `STUDIO_OPTIONS_AND_MIXER.md`;
- `STUDIO_WORKSPACE_GUIDELINES.md`;
- `TIMELINE_INTERACTION_GUIDELINES.md`;
- `TRANSIENT_FEEDBACK_CONTRACT.md`;
- `UI_COPY_STYLE.md`;
- `UI_VISUAL_SYSTEM.md`;
- `USER_GUIDE_POLICY.md`.

A subsystem contract does not create an unrelated release/maintenance gate merely because it exists.

## Archived RC20 closure set

The final development/release campaign is preserved under `history/`, including:

- `IMPLEMENTATION_ROADMAP_RC20_FINAL_2026-09-25.md`;
- `RC20_COMPLETION_PLAN_FINAL_2026-09-25.md`;
- `TEST_AND_HOMOLOGATION_PLAN_RC20_FINAL_2026-09-25.md`;
- `RC20_PHYSICAL_HOMOLOGATION_FINAL_2026-09-25.md`;
- `RELEASE_NOTES_0.5.0-rc20.md`;
- `FINAL_DOCUMENTATION_AUDIT_2026-09-25.md`.

Older H/U/C/M/Alpha/RC plans, release notes, audits and forensic reports remain historical evidence only.

## Root cleanliness rule

Do not add a development roadmap, temporary handoff, completion plan, release checklist, forensic report or superseded candidate note back to `docs/` root.

If future development reopens:

1. create the new dated/identified plan under the appropriate work context;
2. keep stable invariants in live contracts;
3. on closure, archive the plan/report under `history/`;
4. update only the live contracts actually changed by the new baseline.

## Evidence reuse

Accepted evidence remains reusable unless a later source/backend/hardware change can materially invalidate it. Future work requalifies the affected path plus adjacent integration smoke, not the entire historical program by default.

## Producer identity

Documentation-only commits never retroactively change an already-built APK or worker. Frozen producer SHA, APK hashes, signer and worker digest are defined by `RELEASE_BASELINE.md`, not by the latest documentation commit on `main`.
