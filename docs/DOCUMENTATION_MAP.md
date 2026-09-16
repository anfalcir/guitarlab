# Documentation map

Updated: 2026-09-16

## Active authoritative set
For the current RC line use:
- `CURRENT_STATE.md` — live signed/pre-gate evidence boundary;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release path;
- `CI_PIPELINE.md` — manual CI and materialization chain through H26;
- `H26_SAF_CLOUD_BACKUP.md` — H26 SAF backup/restore/retention contract;
- `H25_UI_SETTINGS_SAFETY.md` — H25 UI/settings/delete-safety contract and #642 evidence;
- `H24_HOME_PROJECT_LIBRARY.md` — Home library contract;
- `RELEASE_NOTES_0.5.0-rc3.md` — active behavior delta;
- `TEST_AND_HOMOLOGATION_PLAN.md` — automated/manual gate discipline;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — physical checklist, currently awaiting binding to a signed H26 candidate;
- architecture/product/user-guide/candidate-identity and subsystem documents remain authoritative unless superseded above.

## Current evidence boundary
CI #642 / run `35121955150` / producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632` remains the latest signed DIGITAL PASS through H25: 269/269 JVM/unit, Lint/build/provenance PASS, API36 **28/28 standard + 1/1 isolated geometry**, signed SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

H26 is source-implemented and PRE-GATE. It requires a new exact-source signed workflow before its behavior becomes digital evidence.

Historical audit/checkpoint/alpha documents remain point-in-time evidence and are intentionally not rewritten.

## Producer identity rule
Documentation/source publication after #642 does not retroactively change the #642 APK producer. A future H26 signed authority must be the exact head SHA of its own successful workflow.
