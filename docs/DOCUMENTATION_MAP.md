# Documentation map

Updated: 2026-09-16

## Active authoritative set
For `0.5.0-rc3`, use:
- `CURRENT_STATE.md` — live candidate/evidence boundary;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release path;
- `CI_PIPELINE.md` — manual CI and materialization through H25;
- `H24_HOME_PROJECT_LIBRARY.md` — Home library contract/digital evidence;
- `H25_UI_SETTINGS_SAFETY.md` — H25 UI/settings/delete-safety contract and #642 evidence;
- `RELEASE_NOTES_0.5.0-rc3.md` — active behavior delta;
- `TEST_AND_HOMOLOGATION_PLAN.md` — automated/manual gate discipline;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — residual target-device checklist for the exact #642 candidate;
- architecture/product/user-guide/candidate-identity and subsystem documents remain authoritative within their scope unless superseded above.

## Current evidence boundary
The current signed DIGITAL PASS is **CI #642**, run `35121955150`, exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, authoritative through H25.

Evidence summary:
- 269/269 JVM/unit PASS;
- Android Lint/build/provenance PASS;
- API36 **28/28 standard + 1/1 isolated geometry**;
- signed homologation PASS;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- locked signer match PASS.

H25 is therefore DIGITAL PASS. Remaining evidence is residual target-device UX/interaction smoke plus retained H24/H23b physical checks.

Historical audit/checkpoint/alpha documents remain point-in-time evidence and are intentionally not rewritten.

## Producer identity rule
Documentation-only commits never retroactively change an APK producer. The active physical candidate remains the APK produced by `7bfd876b6a5b0701ab0cf5203de36c31cd117632` / CI #642 until another source-changing candidate passes a later full signed workflow.
