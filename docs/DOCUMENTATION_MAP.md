# Documentation map

Updated: 2026-09-16

## Active authoritative set
For the active `0.5.0-rc3` line, use these documents together:
- `CURRENT_STATE.md` — authoritative current candidate/gate state;
- `IMPLEMENTATION_ROADMAP.md` — milestone progression and remaining release path;
- `CI_PIPELINE.md` — manual CI graph and source-materialization contract through H24a;
- `H24_HOME_PROJECT_LIBRARY.md` — normative Home search/filter/sort contract;
- `DOCUMENTATION_AUDIT_H24_2026-09-16.md` — current documentation/source synchronization audit;
- `RELEASE_NOTES_0.5.0-rc3.md` — active RC3 behavior delta;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — only active residual target-device checklist after exact-source automated PASS;
- `TEST_AND_HOMOLOGATION_PLAN.md` — canonical automated/manual gate discipline;
- `ARCHITECTURE.md` — system/media/editing/recording/Home-library architecture;
- `PRODUCT_REQUIREMENTS.md` — normative product requirements;
- `USER_GUIDE_POLICY.md` — mandatory synchronization contract for in-app `Ajuda`;
- `CANDIDATE_IDENTITY_POLICY.md` — version/source/signer/checksum contract;
- `STUDIO_WORKSPACE_GUIDELINES.md`, `TIMELINE_INTERACTION_GUIDELINES.md`, `STUDIO_OPTIONS_AND_MIXER.md`, `UI_COPY_STYLE.md` and dedicated media/codec contracts remain authoritative for their subsystem scope unless superseded by an active document above.

## Current evidence boundary
The last signed DIGITAL PASS is **CI #639**, run `35096711936`, exact producer source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`, authoritative through H23b.

H24 Home Project Library plus H24a test-source alignment are newer and remain **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`. CI #640 proved the software gate but failed while compiling the new Android test source; H24a corrects that import-only defect. A fresh user-dispatched signed workflow is required before H24/H24a can be promoted to DIGITAL PASS.

## Historical evidence
Files whose names identify older ALPHA releases, H0–H23 checkpoint audits, physical-review snapshots, old gate summaries or candidate handoffs are **point-in-time historical evidence**. Their old CI numbers, SHA values and PRE-GATE/PASS labels are intentionally preserved as history and must not be interpreted as current state.

Examples include `ALPHA*.md`, older `DOCUMENTATION_AUDIT_H*.md`, older candidate/gate summaries and previous review-specific handoffs. When historical files conflict with `CURRENT_STATE.md`, the active documents above win.

This classification is deliberate: updating historical records to today's state would destroy traceability.
