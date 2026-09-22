# Documentation map

Updated: 2026-09-22

## Authority rule
Repository documentation is split into **live authority**, **normative contracts**, and **historical evidence**.

A historical document may intentionally contain an old statement such as “current”, “latest”, “pending”, an old branch name or an old hardware target because it preserves contemporaneous evidence. Such statements apply only to that document's dated context and **never override the live authority set below**.

## Live authority — unified line through U8j / signed #669 historical release authority
Use these first for current status, release identity and remaining work:
- `README.md` — repository entry point;
- `CURRENT_STATE.md` — current unified source candidate, latest signed authority and remaining digital/physical residual;
- `H37_DRIVE_V3_BACKUP.md` — normative H37 direct Drive v3 transport, OAuth/security, resume/integrity and SAF-migration contract;
- `ARCHITECTURE.md` — current system/release architecture;
- `DECISIONS.md` — durable product/engineering decisions, with later decisions superseding earlier ones;
- `PRODUCT_VISION.md` and `PRODUCT_REQUIREMENTS.md` — destination and required behavior;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release sequence;
- `UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md` — approved U0→U12 successor program for the GBW/GuitarLab unification, including autonomous Work protocol and final physical-homologation boundary;
- `UNIFIED_PRODUCT_COHESION_AUDIT.md` — release-blocking C1-C8 consolidation contract ensuring Prepare/Studio/Export/cloud/backup behave and look like one GuitarLab product;
- `U0_BASELINE_INVENTORY.md` — closed U0 baseline, schema/fixture freeze and GBW→GuitarLab ownership map; its machine-readable companion is `integration/u0/inventory.json`;
- `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md` — H29→1.1 implementation/closure plan;
- `CANDIDATE_IDENTITY_POLICY.md` — candidate identity/provenance rules;
- `CI_PIPELINE.md` — manual CI, source materialization and signing provenance;
- `TEST_AND_HOMOLOGATION_PLAN.md` — current automated/physical evidence boundary;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — current target-device checklist bound to CI #663;
- `RELEASE_NOTES_0.5.0-rc3.md` — current signed RC3 behavior delta;
- `RELEASE_NOTES_0.5.0-rc4.md` — H37/H37a/H37b RC4 delta and historical source-pre-gate narrative; its signed gate was later closed by CI #669;
- `H35_TAKE_SYNC_QUIET_CALIBRATION.md` — global-vs-take synchronization and quiet/silent calibration contract;
- `H36_SETTINGS_UX_POLISH.md` — Settings/calibration presentation refinement plus H36a/H36b/H36c corrective history and #663 DIGITAL PASS evidence.

C6 is digitally closed by Android CI #744 / run `35729709715` at exact
materialized source `89a7cca5acd2d5a1c9aabbb5f72e028347dd00db`. U8j is CLOSED /
DIGITAL PASS on Android CI #746 / run `35732391636`, exact source
`ca44bd8efada617bbc30c8c3f3cf9f3d6b18a3c1`. U8 remains active for the
production vNext backup/restore cutover and its fault/real-Drive gates.
The latest signed release authority is Android CI #669 / run `35591631207`,
producer `14b69271f2ae04529fa14475e1a34f2fdd864553`, until a later signed unified
candidate exists.

## Normative subsystem contracts
These define behavior and remain valid unless explicitly superseded:
- `RECORDING_LATENCY_ARCHITECTURE.md` and `RECORDING_LATENCY_CONTRACT.md`;
- `MANAGED_MEDIA_POLICY.md`;
- `CODEC_SUPPORT_MATRIX.md` and `MEDIA_IO_SUPPORT_CLAIM_RULE.md`;
- `PROJECT_PORTABILITY_CONTRACT.md`;
- `TIMELINE_INTERACTION_GUIDELINES.md`;
- `STUDIO_WORKSPACE_GUIDELINES.md` and `STUDIO_OPTIONS_AND_MIXER.md`;
- `UI_VISUAL_SYSTEM.md`, `UI_COPY_STYLE.md`, `TRANSIENT_FEEDBACK_CONTRACT.md`, `USER_GUIDE_POLICY.md`;
- `SHARE_MODAL_CONTRACT.md`;
- `H28_BACKUP_IDENTITY_CONSISTENCY.md` as the protected backup identity/revision contract preserved by H37;
- `H26_SAF_CLOUD_BACKUP.md` and `H27_BACKUP_HISTORY_RELEASE_UX.md` only for the retained architecture/supersession details they explicitly identify.

## Historical/superseded evidence
These are preserved for traceability and must not be used as current-state authority:
- `HISTORICAL_CANDIDATES.md` and all `ALPHA*`, `M4_*`, `M5_*`, `M5C_*`, `M6_*`, `M7_*` candidate/checkpoint/checklist documents;
- older release notes `RELEASE_NOTES_0.4.0-alpha2.md`, `RELEASE_NOTES_0.5.0-rc1.md`, `RELEASE_NOTES_0.5.0-rc2.md`;
- all dated `DOCUMENTATION_AUDIT_*.md` snapshots;
- `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` — historical H0-H6 rationale;
- `M8_GLOBAL_DIGITAL_REGRESSION.md` — historical regression snapshot around CI #615;
- `GITLAB_CI_MIGRATION.md` — inactive migration proposal;
- milestone-specific evidence such as `M2_HOMOLOGATION_EVIDENCE.md`, `H22_AUDIO_ROUTE_UX.md`, `H24_HOME_PROJECT_LIBRARY.md`, `H25_UI_SETTINGS_SAFETY.md`, `H26D_MATERIALIZER_RECOVERY.md`, `H26E_CI650_DIGITAL_PASS.md` and similar exact-source records.

## Current evidence boundary
CI #669 / run `35591631207` / producer `14b69271f2ae04529fa14475e1a34f2fdd864553` is the latest signed DIGITAL PASS. The signed job passed on package `studio.guitarlab.app`, version `0.5.0-rc4` / code `24`, with certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Signed APK SHA-256: `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

The newest unified source is intentionally unsigned: U8j closed digitally at CI #746 / run `35732391636` on `ca44bd8efada617bbc30c8c3f3cf9f3d6b18a3c1`. Historical #663/H36c and H37/H37a/H37b evidence remains preserved for traceability but no longer defines the latest signed authority. The unified line still requires production U8 cutover/fault/real-Drive proof before a new signed final-homologation candidate is justified. Remaining hardware-only audio acceptance is deferred to the consolidated final physical campaign; H33 real-controller physical acceptance remains a separate 1.1 boundary unless later roadmap authority supersedes it.

## Repository audits
- `REPOSITORY_SANITIZATION_2026-09-20.md` records the branch audit supporting a single-branch (`main`) repository cleanup.
- `DOCUMENTATION_CONSISTENCY_AUDIT_2026-09-20.md` records the full 88-file documentation consistency review and the live-vs-historical authority rules.

## Producer identity rule
A later docs-only commit never retroactively changes an already-produced APK identity. Every promoted candidate remains bound to its exact workflow producer SHA and signed APK hash.
