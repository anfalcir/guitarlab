# Documentation map

Updated: 2026-09-14

For the active `0.5.0-rc3` line, use these documents together:
- `CURRENT_STATE.md` — authoritative current status, CI #616 boundary and H7–H10 PRE-GATE state;
- `IMPLEMENTATION_ROADMAP.md` — milestone/gate progression through H10;
- `DOCUMENTATION_AUDIT_H7_H10_2026-09-14.md` — **current document-by-document audit** for Physical Review II;
- `CANDIDATE_IDENTITY_POLICY.md` — version/source/signer/checksum contract;
- `CI_PIPELINE.md` — manual CI graph and H1–H10 materialization contract;
- `RELEASE_NOTES_0.5.0-rc3.md` — RC3 behavior delta including H7–H10;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — only active residual target-device checklist after the next exact-source PASS;
- `TEST_AND_HOMOLOGATION_PLAN.md` — canonical automated/manual gate discipline through H10;
- `ARCHITECTURE.md` — system/media/editing/recording architecture;
- `DECISIONS.md` — durable decisions, including D-071…D-075 for Physical Review II;
- `STUDIO_WORKSPACE_GUIDELINES.md` — tablet workspace/state/recording/waveform interaction contract;
- `TIMELINE_INTERACTION_GUIDELINES.md` — timeline trim/drag/delete/take-lineage contract;
- `STUDIO_OPTIONS_AND_MIXER.md` — Studio actions/options/share/mixer behavior;
- `PRODUCT_REQUIREMENTS.md` — normative product requirements;
- `USER_GUIDE_POLICY.md` — mandatory synchronization contract for in-app `Ajuda`;
- `UI_COPY_STYLE.md` — user-visible copy conventions;
- media/project support contracts and matrices remain authoritative in their dedicated documents.

## Evidence boundary
CI #616 at source `3051619c219e346daca00d2242f60ef03f2d80db` is the latest digitally homologated H0–H6 baseline. Physical Review II H7–H10 is newer. Therefore no document may claim #616 validates the current post-H10 source.

The exact next promoted source SHA and APK checksum are intentionally recorded only after the next manually dispatched workflow passes on final `main`.

## Historical documentation
`DOCUMENTATION_AUDIT_H0_H6_2026-09-14.md` and `DOCUMENTATION_AUDIT_2026-09-14.md` remain historical audit evidence. Older alpha/RC candidate/checkpoint files must not override the active documents above.