# Documentation map

Updated: 2026-09-17

## Active authoritative set
For the current RC and approved forward plan use:
- `CURRENT_STATE.md` — live signed authority, physical evidence boundary and current residual;
- `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md` — authoritative implementation plan for H29-H33 / 1.0→1.1;
- `H28_BACKUP_IDENTITY_CONSISTENCY.md` — backup identity/provider-consistency architecture and H28 corrective;
- `H27_BACKUP_HISTORY_RELEASE_UX.md` — predecessor history semantics/release-copy corrective;
- `IMPLEMENTATION_ROADMAP.md` — milestone and release sequence;
- `RECORDING_LATENCY_ARCHITECTURE.md` and `RECORDING_LATENCY_CONTRACT.md` — current recording timing/compensation contract;
- `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` — historical H0–H6 hardening rationale retained as regression background;
- `M8_GLOBAL_DIGITAL_REGRESSION.md` — historical/global regression matrix;
- `CI_PIPELINE.md` — manual CI, provenance discipline and source materialization;
- `H26_SAF_CLOUD_BACKUP.md` — original backup architecture and supersession boundary;
- `UI_COPY_STYLE.md` and `TRANSIENT_FEEDBACK_CONTRACT.md` — product-facing UX/copy rules;
- `RELEASE_NOTES_0.5.0-rc3.md` — active RC3 behavior delta;
- `TEST_AND_HOMOLOGATION_PLAN.md` — current regression and physical residual framework;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — final target-device checklist.

## Current evidence boundary
CI #653 / run `35207902169` / producer `d09fc003e2ae2d699238eb39ba699f75a746fea4` is the current signed DIGITAL PASS through H28.

Signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`.

Target-device backup testing after #653 confirms the H28 corrected backup workflow is functioning as intended.

Source checkpoint `1df91e16ad0a928b0d5ab93bfd975b49b6d2da62` stages H29-H33 as **SOURCE PRE-GATE READY / MANUAL CI PENDING**. It has deterministic materialization/idempotency/fail-closed evidence, but no Android CI/signing evidence yet. H28/CI #653 therefore remains the signed authority until a manually dispatched run is audited.

Historical checkpoint documents remain point-in-time evidence and are intentionally not rewritten to pretend they described later H28/H29+ behavior.

## Forward-plan boundary
The H29-H33 plan is intentionally narrow:
- all approved work through H32 is hardening/refinement of existing capabilities;
- the practical song/session quality target is bounded at 10 minutes;
- the only approved new feature family is H33 external MIDI/footswitch control;
- excluded feature ideas remain excluded unless the scope is explicitly reopened later.

## Producer identity rule
A later source/docs commit never retroactively changes an already-produced APK identity. Every promoted candidate must be bound to its exact workflow producer SHA and signed APK hash.
