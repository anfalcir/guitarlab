# Documentation map

Updated: 2026-09-20

## Active authoritative set
For the current RC and approved forward plan use:
- `CURRENT_STATE.md` — live signed authority, physical evidence boundary and current residual;
- `H35_TAKE_SYNC_QUIET_CALIBRATION.md` — H35 source contract for global-vs-take synchronization and quiet/silent calibration behavior;
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
CI #657 / run `35290128876` / producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6` is the current signed DIGITAL PASS through H33b.

Signed APK SHA-256: `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.

H34/H35 are SOURCE PRE-GATE READY. H34 expands the global residual range to ±500 ms; H35 separates future-recording global adjustment from persistent take-specific synchronization and hardens digital/physical calibration behavior. Deterministic local/materialization proofs are complete; #657 remains the signed authority until the H34/H35 source receives its exact-source Android gate.

Target-device backup testing after #653 remains valid physical evidence for the protected H28 contract.

## Forward-plan boundary
The H29-H33 plan is intentionally narrow:
- all approved work through H32 is hardening/refinement of existing capabilities;
- the practical song/session quality target is bounded at 10 minutes;
- the only approved new feature family is H33 external MIDI/footswitch control;
- excluded feature ideas remain excluded unless the scope is explicitly reopened later.

## Producer identity rule
A later source/docs commit never retroactively changes an already-produced APK identity. Every promoted candidate must be bound to its exact workflow producer SHA and signed APK hash.
