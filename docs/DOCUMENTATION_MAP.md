# Documentation map

Updated: 2026-09-14

For the active `0.5.0-rc3` candidate, use these documents together:
- `CURRENT_STATE.md` — authoritative current project/candidate status and exact distinction between #615 baseline and post-H0–H6 source;
- `IMPLEMENTATION_ROADMAP.md` — milestone/gate disposition and H0–H6 progression;
- `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` — H0–H6 root causes, implementation record, acceptance and next exact-source gate;
- `CANDIDATE_IDENTITY_POLICY.md` — exact version/source/signer/checksum contract;
- `CI_PIPELINE.md` — manual CI/release job graph, materialization order, caches, artifacts and failure interpretation;
- `RELEASE_NOTES_0.5.0-rc3.md` — complete RC3 behavior delta including H0–H6;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — only residual target-device checks after the next exact-source automated PASS;
- `TEST_AND_HOMOLOGATION_PLAN.md` — canonical automated/manual gate discipline and H0–H6 regression expectations;
- `M8_GLOBAL_DIGITAL_REGRESSION.md` — current global digital matrix and H0–H6 pre-gate status;
- `ARCHITECTURE.md` — current system/media/editing/recording/materialization architecture;
- `DECISIONS.md` — durable product/architecture decisions, including D-066…D-070 for H0–H6;
- `PRODUCT_REQUIREMENTS.md` — normative product requirements;
- `TIMELINE_INTERACTION_GUIDELINES.md` — trim/drag/delete/take-lineage interaction contract;
- `STUDIO_WORKSPACE_GUIDELINES.md` — tablet workspace/gesture/overlay contract;
- `STUDIO_OPTIONS_AND_MIXER.md` — Studio actions, Options/Share/Mixer and clip-level editing contract;
- `USER_GUIDE_POLICY.md` — mandatory synchronization contract for the novice-facing in-app `Ajuda` guide;
- `UI_COPY_STYLE.md` — capitalization, punctuation and terminology standard for user-visible copy;
- `CODEC_SUPPORT_MATRIX.md` — import/export capability claims;
- `MANAGED_MEDIA_POLICY.md` — source/proxy/media lifecycle rules;
- `MEDIA_IO_SUPPORT_CLAIM_RULE.md` — evidence rule for media-I/O support claims;
- `PROJECT_PORTABILITY_CONTRACT.md` — `.guitarlab` round-trip/restore contract;
- `SHARE_MODAL_CONTRACT.md` — project/master output UX contract;
- `PRODUCT_VISION.md` — high-level product direction;
- `HISTORICAL_CANDIDATES.md` — candidate history and current-vs-historical boundary;
- `DOCUMENTATION_AUDIT_H0_H6_2026-09-14.md` — **current document-by-document audit** after CI #615 and H0–H6 consolidation.

`DOCUMENTATION_AUDIT_2026-09-14.md` is preserved as the earlier same-day audit from the post-#613 physical-review phase. It is historical audit evidence and must not override the newer H0–H6 audit above.

`M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` and all older alpha/RC candidate/checkpoint files are historical evidence. They must not be used as the active RC3 checklist when they conflict with the current documents above.

## Current evidence boundary
CI #615 at `74bf86efbec94d249c4968c3284bf1985cd66b44` is the latest fully green signed baseline. Current `main` is newer because H0–H6 editing/recording hardening was implemented afterward. Therefore no document may claim #615 validates the current HEAD.

The exact next promoted source SHA and APK checksum are intentionally not predeclared. After the next manually dispatched workflow passes, `github.sha`, `BUILD_IDENTITY.txt`, `SHA256SUMS.txt`, APK package/version and locked signer must all agree.
