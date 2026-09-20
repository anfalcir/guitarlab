# Documentation map

Updated: 2026-09-20

## Authority rule
Repository documentation is split into **live authority**, **normative contracts**, and **historical evidence**.

A historical document may intentionally contain an old statement such as “current”, “latest”, “pending”, an old branch name or an old hardware target because it preserves contemporaneous evidence. Such statements apply only to that document's dated context and **never override the live authority set below**.

## Live authority — signed #663 / H37b source-candidate line
Use these first for current status, release identity and remaining work:
- `README.md` — repository entry point;
- `CURRENT_STATE.md` — current signed authority, H37b source-candidate state and remaining physical residual;
- `H37_DRIVE_V3_BACKUP.md` — normative H37 direct Drive v3 transport, OAuth/security, resume/integrity and SAF-migration contract;
- `ARCHITECTURE.md` — current system/release architecture;
- `DECISIONS.md` — durable product/engineering decisions, with later decisions superseding earlier ones;
- `PRODUCT_VISION.md` and `PRODUCT_REQUIREMENTS.md` — destination and required behavior;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release sequence;
- `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md` — H29→1.1 implementation/closure plan;
- `CANDIDATE_IDENTITY_POLICY.md` — candidate identity/provenance rules;
- `CI_PIPELINE.md` — manual CI, source materialization and signing provenance;
- `TEST_AND_HOMOLOGATION_PLAN.md` — current automated/physical evidence boundary;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — current target-device checklist bound to CI #663;
- `RELEASE_NOTES_0.5.0-rc3.md` — current signed RC3 behavior delta;
- `RELEASE_NOTES_0.5.0-rc4.md` — H37/H37a/H37b RC4 source-candidate delta; SOURCE PRE-GATE until a later signed CI supersedes #663;
- `H35_TAKE_SYNC_QUIET_CALIBRATION.md` — global-vs-take synchronization and quiet/silent calibration contract;
- `H36_SETTINGS_UX_POLISH.md` — Settings/calibration presentation refinement plus H36a/H36b/H36c corrective history and #663 DIGITAL PASS evidence.

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
CI #663 / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d` is the current signed DIGITAL PASS through H36c.

Signed APK SHA-256: `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

H28 backup/provider-consistency behavior is already physically accepted in the signed #663 baseline. H36/H36a/H36b/H36c are included in that signed authority. H37 reached CI #664 but failed at Kotlin compilation; H37a corrected the compile boundary; H37b is the current SOURCE PRE-GATE OAuth hardening and does not become signed authority until a fresh manual canonical workflow passes; its first real OAuth/Drive backup/restore acceptance is also still required. Remaining audio physical work is recording alignment, USB loss/reconnect/capture preservation, the continuous 10-minute quality exercise and H35 target-device synchronization/calibration behavior. H33 real-controller physical acceptance remains a separate 1.1 boundary.

## Repository audits
- `REPOSITORY_SANITIZATION_2026-09-20.md` records the branch audit supporting a single-branch (`main`) repository cleanup.
- `DOCUMENTATION_CONSISTENCY_AUDIT_2026-09-20.md` records the full 88-file documentation consistency review and the live-vs-historical authority rules.

## Producer identity rule
A later docs-only commit never retroactively changes an already-produced APK identity. Every promoted candidate remains bound to its exact workflow producer SHA and signed APK hash.