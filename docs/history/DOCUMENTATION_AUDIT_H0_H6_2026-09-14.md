# Documentation audit — RC3 H0–H6 — 2026-09-14

## Purpose
This is the second document-by-document consolidation pass on 2026-09-14, executed after CI #615 and the H0–H6 physical editing/recording hardening implementation.

`DOCUMENTATION_AUDIT_2026-09-14.md` remains preserved as evidence of the earlier post-#613 physical-review pass. Its then-current statement that #613 was the latest green baseline is historical context, not current truth. This file supersedes that earlier audit **only for current-state interpretation**.

## Current facts used by this audit
- active candidate: `0.5.0-rc3`, versionCode `23`;
- package: `studio.guitarlab.app`;
- locked homologation certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- latest fully green signed baseline: CI #615 at `74bf86efbec94d249c4968c3284bf1985cd66b44`;
- #615 signed APK SHA-256: `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`;
- current `main` is newer than #615 because H0–H6 editing/recording hardening was implemented afterward;
- H0–H6 is **IMPLEMENTED / PRE-GATE**, not canonical CI PASS;
- next promoted APK requires one new manually dispatched exact-source workflow with signed homologation enabled;
- after that automated PASS, `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active target-device checklist.

## Active/canonical documents — second-pass result

| Document | Result | H0–H6 / current-state disposition |
|---|---|---|
| `README.md` | **Updated** | #615 baseline, H0–H6 current source, next exact-source gate and short residual physical policy now explicit. |
| `ARCHITECTURE.md` | **Updated** | Added independent trim handles, transactional take lineage, explicit drag intent, session timing model, frame-span live waveform and serial materialization architecture. |
| `CANDIDATE_IDENTITY_POLICY.md` | Reviewed — no change required | Already defines RC3/code23/package/signer and exact-source workflow identity without predeclaring a future SHA. |
| `CI_PIPELINE.md` | **Updated** | #615 is now latest green baseline; H1→H6 materialization order and final H6 test-fix/guide patches documented. |
| `CODEC_SUPPORT_MATRIX.md` | Reviewed — no change required | H0–H6 does not change codec support claims/capability gates. |
| `CURRENT_STATE.md` | **Updated** | Authoritative current state now records #615 baseline and every H0–H6 block as implemented/pre-gate. |
| `DECISIONS.md` | **Updated** | Added D-066…D-070 for independent trim handles, take-lineage reconciliation, shared clip-delete/trash command, measured synchronization and frame-based live waveform. |
| `DOCUMENTATION_MAP.md` | **Update required in this pass** | Must point to this audit as the current audit and retain the earlier audit as historical evidence. |
| `HISTORICAL_CANDIDATES.md` | **Updated** | Removed obsolete Alpha13-as-active wording; RC3/#615 and current canonical source list now authoritative. |
| `IMPLEMENTATION_ROADMAP.md` | **Updated** | M8 records H0–H6 implemented/exact-source gate pending; #615 is latest green pre-H0–H6 baseline. |
| `M8_GLOBAL_DIGITAL_REGRESSION.md` | **Updated** | Removed obsolete RC1/#593 active-state claim; matrix now distinguishes #615 baseline from H1–H6 pre-gate rows. |
| `MANAGED_MEDIA_POLICY.md` | Reviewed — no change required | Existing immutable-source/conservative-orphan/deletion policy already requires reference-safe media retention and is compatible with split-child deletion. |
| `MEDIA_IO_SUPPORT_CLAIM_RULE.md` | Reviewed — no change required | H0–H6 introduces no media-format capability claim. |
| `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` | **Updated** | Converted from proposal to H0–H6 implementation record with root causes, acceptance and exact next gate. |
| `PRODUCT_REQUIREMENTS.md` | **Updated** | Added normative trim, clip-delete/lineage, drag transaction, recording synchronization and live-waveform requirements. |
| `PRODUCT_VISION.md` | Reviewed — no change required | Product purpose is unchanged; H0–H6 hardens existing practice/recording scope. |
| `PROJECT_PORTABILITY_CONTRACT.md` | Reviewed — no change required | Bundle/restore contract unchanged; new clip-lineage regression is internal project-state integrity. |
| `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` | **Updated** | Checklist reduced to target-only trim, split/move/delete/trash, MK300 isolation/synchronization, long waveform and focused smoke. |
| `RELEASE_NOTES_0.5.0-rc3.md` | **Updated** | Added H0–H6 behavior/regression and correct #615/pending-HEAD validation status. |
| `SHARE_MODAL_CONTRACT.md` | Reviewed — no change required | Export/share behavior is unaffected by H0–H6. |
| `STUDIO_OPTIONS_AND_MIXER.md` | **Updated** | Correct Studio action order includes Ajuda; clip-level delete/trash and drag transaction are now represented. |
| `STUDIO_WORKSPACE_GUIDELINES.md` | **Updated** | Added trim-handle ownership, trash target, split-lineage, explicit drag intent and live-waveform temporal UX rules. |
| `TEST_AND_HOMOLOGATION_PLAN.md` | **Updated** | Adds H0–H6 automated contracts, serial materializer, #615 evidence and exact-source next gate. |
| `TIMELINE_INTERACTION_GUIDELINES.md` | **Updated** | Normative Move/Delete/NoOp, trash confirmation, split take lineage, handle collision and regression rules. |
| `UI_COPY_STYLE.md` | Reviewed — no change required | New wording (`Excluir clipe`, existing `Aplicar`, red `X`) is compatible with current pt-BR copy conventions. |
| `USER_GUIDE_POLICY.md` | Reviewed — no change required | Existing policy already requires guide synchronization; H6 supplies the corresponding guide patch. |

## Historical/superseded documents — document-by-document disposition
The following files were reclassified against current RC3 truth and intentionally left unchanged. Their old version/gate wording is evidence of the state at the time, not a current instruction.

| Document | Disposition |
|---|---|
| `ALPHA05_BUILD_NOTE.md` | Historical/superseded — preserve. |
| `ALPHA05_CANDIDATE.md` | Historical/superseded — preserve. |
| `ALPHA06_CANDIDATE.md` | Historical/superseded — preserve. |
| `ALPHA13_CANDIDATE.md` | Historical/superseded — preserve. |
| `ALPHA13_GATE_SUMMARY.md` | Historical/superseded — preserve. |
| `ALPHA13_HOMOLOGATION_HANDOFF.md` | Historical/superseded — preserve. |
| `ALPHA13_RELEASE_NOTES.md` | Historical/superseded — preserve. |
| `GITLAB_CI_MIGRATION.md` | Historical/superseded migration option — preserve. |
| `M2_HOMOLOGATION_EVIDENCE.md` | Historical hardware evidence — preserve. |
| `M4_ALPHA05_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve. |
| `M4_ALPHA06_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve. |
| `M4_ALPHA07_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve. |
| `M4_CLIP_MANAGEMENT_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M4_COMMERCIAL_POLISH_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M4_METERING_MASTER_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M4_PLAYBACK_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M4_TRACK_MIX_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M5C_ALPHA09_UX_CONSOLIDATION.md` | Historical approval/UX evidence — preserve. |
| `M5C_COMPLETE_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M5C_FOUNDATION_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M5C_MIXER_V4_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M5C_RECORDING_COORDINATOR_PLAN.md` | Historical implementation plan — preserve. |
| `M5C_UX_IMPLEMENTATION_PLAN.md` | Historical implementation plan — preserve. |
| `M5_ALPHA08_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve. |
| `M5_ALPHA09_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve. |
| `M5_ALPHA10_CORRECTIVE_CHECKPOINT.md` | Historical corrective evidence — preserve. |
| `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md` | Historical rejected candidate evidence — preserve. |
| `M5_ALPHA12_FINAL_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve. |
| `M5_ALPHA13_FINAL_HOMOLOGATION_CHECKLIST.md` | Historical/superseded — preserve; not active. |
| `M5_ALPHA13_SCOPE_DELTA.md` | Historical scope evidence — preserve. |
| `M5_ALPHA13_VALIDATION_STATUS.md` | Historical validation evidence — preserve. |
| `M5_ALPHA14_FINAL_HOMOLOGATION_CHECKLIST.md` | Historical M5 closure evidence — preserve. |
| `M5_CAPTURE_ENGINE_CHECKPOINT.md` | Historical implementation evidence — preserve. |
| `M5_FINAL_CANDIDATE_POLICY.md` | Historical candidate policy — preserve. |
| `M5_MEDIA_IO_CONSOLIDATION.md` | Historical implementation evidence — preserve. |
| `M5_RECORDING_IMPLEMENTATION_PLAN.md` | Historical implementation plan — preserve. |
| `M6_ALPHA1_HOMOLOGATION_CHECKLIST.md` | Historical M6 closure evidence — preserve. |
| `M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` | Historical M7 candidate evidence — preserve; not the active physical checklist. |
| `RELEASE_NOTES_0.4.0-alpha2.md` | Historical release notes — preserve. |
| `RELEASE_NOTES_0.5.0-rc1.md` | Historical release notes — preserve. |
| `RELEASE_NOTES_0.5.0-rc2.md` | Historical release notes — preserve. |
| `STUDIO_VIDEO_REVIEW_2026-09-08.md` | Historical physical-review evidence — preserve. |
| `DOCUMENTATION_AUDIT_2026-09-14.md` | Earlier same-day audit evidence — preserve; superseded by this file for current-state interpretation. |

## Source/materialization documentation audit
Versioned active hardening inputs now documented and expected under `.source-parts`:
- `H1TrimHardening.patch`;
- `H2ClipLifecycle.patch`;
- `H3DragTransaction.patch`;
- `H4RecordingSync.patch.gz`;
- `H5LiveWaveform.patch.gz`;
- `H6IntegratedRegression.patch.gz`;
- `H6WaveformTestFix.patch`;
- `H6UserGuideSync.patch.gz`.

Canonical application order in `scripts/materialize_ci_sources.sh` is H1 → H2 → H3 → H4 → H5 → H6 integrated regression → H6 test fix → H6 guide sync. No documentation may claim canonical PASS for these deltas until the next exact-source manual workflow is green.

## Closure criteria for this documentation pass
This documentation pass is closed when:
1. `DOCUMENTATION_MAP.md` points to this audit as the current audit and keeps the earlier audit historical;
2. `main` HEAD is recorded after the final documentation commit;
3. materializer still contains every H1–H6 patch in the intended order;
4. no workflow was automatically dispatched by documentation commits;
5. the next requested action is one explicit manual signed workflow for that exact final HEAD.
