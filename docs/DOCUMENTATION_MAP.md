# Documentation map

Updated: 2026-09-22

## Authority rule
Repository documentation is split into **live authority**, **normative contracts**, and **historical evidence**.

A historical document may intentionally contain an old statement such as “current”, “latest”, “pending”, an old branch name or an old hardware target because it preserves contemporaneous evidence. Such statements apply only to that document's dated context and **never override the live authority set below**.

## Live authority — unified line through U11 signed closure / U12 active
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
- `U10_FINAL_DIGITAL_COHESION_GATE.md` — canonical #781 evidence closing U10/C7/C8 and handing the exact unsigned technical source to U11;
- `U11_FINAL_DIGITAL_RELEASE_GATE.md` — exact rc5 freeze/signing/cloud/security gate and producer-identity contract;
- `U11_SECURITY_DOCUMENTATION_AUDIT.md` — pre-freeze materialization, credential-hygiene, OAuth-scope, tuner-exclusion and live-doc consistency PASS;
- `U12_FINAL_PHYSICAL_HOMOLOGATION.md` — consolidated final SM-X230 + MK-300 physical-only acceptance campaign;
- `U0_BASELINE_INVENTORY.md` — closed U0 baseline, schema/fixture freeze and GBW→GuitarLab ownership map; its machine-readable companion is `integration/u0/inventory.json`;
- `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md` — H29→1.1 implementation/closure plan;
- `CANDIDATE_IDENTITY_POLICY.md` — candidate identity/provenance rules;
- `CI_PIPELINE.md` — manual CI, source materialization and signing provenance;
- `TEST_AND_HOMOLOGATION_PLAN.md` — current automated/physical evidence boundary;
- `U12_FINAL_PHYSICAL_HOMOLOGATION.md` — current target-device checklist bound to the exact signed U11 rc5 candidate;
- `RELEASE_NOTES_0.5.0-rc3.md` — historical signed RC3 behavior delta;
- `RELEASE_NOTES_0.5.0-rc4.md` — H37/H37a/H37b RC4 delta and historical source-pre-gate narrative; its signed gate was later closed by CI #669;
- `RELEASE_NOTES_0.5.0-rc5.md` — current signed U11 rc5/25 candidate identity, digital gate and U12 physical residual;
- `H35_TAKE_SYNC_QUIET_CALIBRATION.md` — global-vs-take synchronization and quiet/silent calibration contract;
- `H36_SETTINGS_UX_POLISH.md` — Settings/calibration presentation refinement plus H36a/H36b/H36c corrective history and #663 DIGITAL PASS evidence.

C6 is digitally closed by Android CI #744 / run `35729709715` at exact
materialized source `89a7cca5acd2d5a1c9aabbb5f72e028347dd00db`. U8j is CLOSED /
DIGITAL PASS on Android CI #746 / run `35732391636`; U8k production cutover is
CLOSED / DIGITAL PASS on #750; U8l network/HTTP fault injection is CLOSED /
DIGITAL PASS on #756; and U8m closes the provider-real gate on exact technical
source `2375dcb72983376cb486eccf41faf1734633cc94` after Android CI #757 / run `35753982993` plus real Google
Drive result `U8m PASS · r_1790095960 · cleanup 8/8/14`, report SHA-256
`84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`.
**U10/C8 is CLOSED / DIGITAL PASS** on Android CI #781 / run `35791192802`,
exact technical source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`, with 23/23
instrumented classes, 76 observed API36 tests and 24/24 retained visual artifacts.
**U11 is CLOSED / DIGITAL PASS and U12 is active.** The latest signed release authority is Android CI #783 / run `35793456972`, producer `4218e4343746932a4de61c5abaa29ba5769a30ed`, package `studio.guitarlab.app`, `0.5.0-rc5` / versionCode `25`.

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
CI #783 / run `35793456972` / producer `4218e4343746932a4de61c5abaa29ba5769a30ed` is the latest signed DIGITAL PASS. The signed job passed on package `studio.guitarlab.app`, version `0.5.0-rc5` / code `25`, with certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Signed APK SHA-256: `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`.

U10/C8 technical closure remains preserved at exact source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63` / Android CI #781. U11 subsequently froze and signed the exact rc5 candidate at producer `4218e4343746932a4de61c5abaa29ba5769a30ed` / Android CI #783. U8m real-provider acceptance remains supporting authority: `U8m PASS · r_1790095960 · cleanup 8/8/14`, sanitized report SHA-256 `84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`. Historical #663/H36c and H37/H37a/H37b evidence remains preserved for traceability. Remaining hardware-only acceptance is the consolidated U12 physical campaign; H33 real-controller physical acceptance remains a separate 1.1 boundary unless later roadmap authority supersedes it.

## Repository audits
- `REPOSITORY_SANITIZATION_2026-09-20.md` records the branch audit supporting a single-branch (`main`) repository cleanup.
- `DOCUMENTATION_CONSISTENCY_AUDIT_2026-09-20.md` records the full 88-file documentation consistency review and the live-vs-historical authority rules.

## Producer identity rule
A later docs-only commit never retroactively changes an already-produced APK identity. Every promoted candidate remains bound to its exact workflow producer SHA and signed APK hash.
