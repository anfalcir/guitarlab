# Historical candidate index

Updated: 2026-09-20

Historical documents preserve contemporaneous evidence and do not override current canonical sources. A statement such as `pending`, `active candidate` or an old version number inside a historical checkpoint is interpreted only in that document's original time context.

## Superseded M4 candidates
- `ALPHA05_BUILD_NOTE.md` — historical build note;
- `ALPHA05_CANDIDATE.md` — historical candidate;
- `ALPHA06_CANDIDATE.md` — historical candidate;
- `M4_ALPHA05_HOMOLOGATION_CHECKLIST.md`, `M4_ALPHA06_HOMOLOGATION_CHECKLIST.md`, `M4_ALPHA07_HOMOLOGATION_CHECKLIST.md` — historical gates;
- all `M4_*_CHECKPOINT.md` files — implementation evidence.

## Superseded M5 candidates/checkpoints
- `M5_ALPHA08_HOMOLOGATION_CHECKLIST.md` — historical/superseded;
- `M5_ALPHA09_HOMOLOGATION_CHECKLIST.md` — historical; alpha09 broadly approved with minor UX findings;
- `M5C_ALPHA09_UX_CONSOLIDATION.md` — historical approval record;
- `M5_ALPHA10_CORRECTIVE_CHECKPOINT.md` — historical Trim/clear-delete checkpoint;
- `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md` — historical candidate physically rejected for drag gesture cancellation;
- `M5_ALPHA12_FINAL_HOMOLOGATION_CHECKLIST.md` — historical drag/Track Settings correction candidate superseded before closure;
- `M5_ALPHA13_*` documents — historical M5 candidate/checkpoint evidence; they are **not** active checklists;
- `M5_ALPHA14_FINAL_HOMOLOGATION_CHECKLIST.md` — historical evidence associated with the later M5 closure;
- `M5C_*`, `M5_CAPTURE_ENGINE_CHECKPOINT.md`, `M5_MEDIA_IO_CONSOLIDATION.md`, `M5_RECORDING_IMPLEMENTATION_PLAN.md` and `M5_FINAL_CANDIDATE_POLICY.md` — implementation/candidate history, not current architecture or pending work.

## M6/M7 historical evidence
- `M6_ALPHA1_HOMOLOGATION_CHECKLIST.md` — historical physical evidence for closed M6;
- `M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` — historical M7 candidate evidence; it is not the active RC3 checklist;
- older release notes such as `RELEASE_NOTES_0.4.0-alpha2.md`, `RELEASE_NOTES_0.5.0-rc1.md` and `RELEASE_NOTES_0.5.0-rc2.md` preserve candidate history.

## RC3 evidence progression
- CI #613 / #615 — earlier RC3 signed baselines retained as historical evidence;
- CI #620–#651 — intermediate hardening/backup milestones retained by their dated audit/checkpoint files;
- CI #653 — H28 signed DIGITAL PASS; the triggering backup/provider-consistency defect was subsequently accepted on target hardware;
- CI #657 — H29-H33/H33a/H33b signed DIGITAL PASS;
- CI #658 — H35 intermediate run: materialization, unit tests and API36 passed; Lint correctly blocked signing;
- **CI #659 / run `35512894518` / producer `a6a53e8ba9e75b32565e451870758c7c65ad687f` — current signed DIGITAL PASS through H35a**, signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.

Older candidate statements using words such as “current”, “latest”, “pending”, “active branch” or “PR draft” remain true only in their dated historical context.
## Current canonical sources
Current truth is defined by the active documents indexed in `DOCUMENTATION_MAP.md`, principally:
- `CURRENT_STATE.md`;
- `ARCHITECTURE.md`;
- `DECISIONS.md`;
- `PRODUCT_VISION.md` and `PRODUCT_REQUIREMENTS.md`;
- `IMPLEMENTATION_ROADMAP.md`;
- `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`;
- `RECORDING_LATENCY_ARCHITECTURE.md` and `RECORDING_LATENCY_CONTRACT.md`;
- `H35_TAKE_SYNC_QUIET_CALIBRATION.md`;
- `CANDIDATE_IDENTITY_POLICY.md`;
- `CI_PIPELINE.md`;
- `TEST_AND_HOMOLOGATION_PLAN.md`;
- `RELEASE_NOTES_0.5.0-rc3.md`;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`;
- media, timeline and UI contract documents explicitly classified as normative in `DOCUMENTATION_MAP.md`.

`PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md`, `M8_GLOBAL_DIGITAL_REGRESSION.md`, `GITLAB_CI_MIGRATION.md`, dated documentation audits and Alpha/M4/M5/M6/M7 checkpoint/checklist files are historical evidence, not current status.

No historical checklist or dated audit may override the active set above.
