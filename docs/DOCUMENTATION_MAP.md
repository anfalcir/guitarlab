# Documentation map

Updated: 2026-09-16

## Active authoritative set
For `0.5.0-rc3`, use:
- `CURRENT_STATE.md` — live candidate/evidence boundary;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release path;
- `CI_PIPELINE.md` — manual CI and materialization through H25;
- `H24_HOME_PROJECT_LIBRARY.md` — Home library contract/digital evidence;
- `H25_UI_SETTINGS_SAFETY.md` — current H25 UI/settings/delete-safety contract;
- `RELEASE_NOTES_0.5.0-rc3.md` — active behavior delta;
- `TEST_AND_HOMOLOGATION_PLAN.md` — automated/manual gate discipline;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — residual physical checklist once the current source has signed DIGITAL PASS;
- architecture/product/user-guide/candidate-identity and subsystem documents remain authoritative within their scope unless superseded above.

## Current evidence boundary
CI #641 / run `35105065689` / producer `b11769f340f7056c37dfb17d95b062909dad87bf` is the last signed DIGITAL PASS and is authoritative through H24a.

H25 is newer product/test source and is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. A fresh user-dispatched signed workflow is required before H25 physical acceptance.

Historical audit/checkpoint/alpha documents remain point-in-time evidence and are intentionally not rewritten.

## Producer identity rule
Documentation commits never retroactively change an APK producer. After H25 source lands, #641 remains historical signed evidence through H24a; the next H25 physical candidate must come from a later full workflow whose exact `head_sha` passes all gates.
