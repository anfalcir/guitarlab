# Documentation map

Updated: 2026-09-16

## Active authoritative set
For the active `0.5.0-rc3` line, use these documents together:
- `CURRENT_STATE.md` — authoritative current candidate/gate state;
- `IMPLEMENTATION_ROADMAP.md` — milestone progression and remaining release path;
- `CI_PIPELINE.md` — manual CI graph and source-materialization contract through H24a;
- `H24_HOME_PROJECT_LIBRARY.md` — normative Home search/filter/sort contract and #641 evidence;
- `DOCUMENTATION_AUDIT_H24_2026-09-16.md` — documentation/source synchronization audit plus #641 promotion;
- `RELEASE_NOTES_0.5.0-rc3.md` — active RC3 behavior delta;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — only active residual target-device checklist after #641;
- `TEST_AND_HOMOLOGATION_PLAN.md` — canonical automated/manual gate discipline;
- `ARCHITECTURE.md` — system/media/editing/recording/Home-library architecture;
- `PRODUCT_REQUIREMENTS.md` — normative product requirements;
- `USER_GUIDE_POLICY.md` — mandatory synchronization contract for in-app `Ajuda`;
- `CANDIDATE_IDENTITY_POLICY.md` — version/source/signer/checksum contract;
- subsystem-specific Studio/timeline/media/codec documents remain authoritative within their scope unless superseded above.

## Current evidence boundary
The current signed DIGITAL PASS is **CI #641**, run `35105065689`, exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf`, authoritative through H24a.

Evidence summary:
- 269/269 JVM/unit PASS;
- Android Lint/build/provenance PASS;
- API36 **25/25 standard + 1/1 isolated geometry**;
- signed homologation PASS;
- signed APK SHA-256 `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`;
- locked signer match PASS.

H24/H24a are therefore DIGITAL PASS. Their remaining evidence is target-device UX smoke only. H23b also remains digitally passed, with focused physical recording-timing validation still pending.

CI #640 is historical corrective evidence: software gate PASS, Android-test compile failure before instrumentation, signing skipped. It is superseded by #641.

## Historical evidence
Files whose names identify older ALPHA releases, H0–H23 checkpoint audits, physical-review snapshots, old gate summaries or candidate handoffs are **point-in-time historical evidence**. Their old CI numbers, SHA values and PRE-GATE/PASS labels are intentionally preserved and must not be interpreted as current state.

When historical files conflict with `CURRENT_STATE.md`, the active set above wins. This classification is deliberate: rewriting historical records to today's state would destroy traceability.

## Producer identity rule
Any documentation-only commit created after this promotion does not change the signed product producer. The physical candidate remains the APK produced by `b11769f340f7056c37dfb17d95b062909dad87bf` / CI #641 until source changes and a later full signed workflow is promoted.
