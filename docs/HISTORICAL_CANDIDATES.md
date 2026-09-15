# Historical candidate index

Updated: 2026-09-14

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
- CI #613 at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37` — earlier fully green optimized RC3 signed baseline;
- CI #614 at `9be232643f1d34f7e3a08417e44773e4f8b9ef7f` — historical workflow/test-API compatibility failure; signing correctly blocked;
- CI #615 at `74bf86efbec94d249c4968c3284bf1985cd66b44` — latest fully green signed pre-H0–H6 RC3 baseline; signed APK SHA-256 `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

The current source advanced after #615 with H0–H6 editing/recording hardening. Therefore #615 remains historical baseline evidence and cannot be used as proof for the new HEAD.

## Current canonical sources
Current truth is defined by the active documents indexed in `DOCUMENTATION_MAP.md`, principally:
- `CURRENT_STATE.md`;
- `IMPLEMENTATION_ROADMAP.md`;
- `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md`;
- `ARCHITECTURE.md`;
- `DECISIONS.md`;
- `PRODUCT_REQUIREMENTS.md`;
- `TIMELINE_INTERACTION_GUIDELINES.md`;
- `MANAGED_MEDIA_POLICY.md`;
- `CANDIDATE_IDENTITY_POLICY.md`;
- `CI_PIPELINE.md`;
- `M8_GLOBAL_DIGITAL_REGRESSION.md`;
- `TEST_AND_HOMOLOGATION_PLAN.md`;
- `RELEASE_NOTES_0.5.0-rc3.md`;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`.

No Alpha/M5/M6/M7 historical checklist may override those RC3 documents.
