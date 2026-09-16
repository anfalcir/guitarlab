# Documentation map

Updated: 2026-09-16

## Active authoritative set
For the current RC line use:
- `CURRENT_STATE.md` — live evidence boundary and exact signed candidate identity;
- `H26E_CI650_DIGITAL_PASS.md` — consolidated CI #650/H26e audit and promotion evidence;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release path;
- `CI_PIPELINE.md` — manual CI, artifact/provenance discipline and materialization chain through H26e;
- `H26_SAF_CLOUD_BACKUP.md` — H26 SAF backup/restore/retention contract and DIGITAL PASS boundary;
- `H25_UI_SETTINGS_SAFETY.md` — H25 UI/settings/delete-safety contract and historical #642 evidence;
- `H24_HOME_PROJECT_LIBRARY.md` — Home library contract;
- `RELEASE_NOTES_0.5.0-rc3.md` — active RC3 behavior and exact candidate identity;
- `TEST_AND_HOMOLOGATION_PLAN.md` — automated evidence and minimal physical residual;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — physical checklist bound to the exact #650 signed APK;
- architecture/product/user-guide/candidate-identity and subsystem documents remain authoritative unless superseded above.

## Current evidence boundary
CI #650 / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` is the current signed DIGITAL PASS through H26/H26e:
- 285/285 JVM/unit PASS;
- performance evidence PASS;
- Lint/build/provenance PASS (Lint reports: 44 warnings + 3 hints, 0 errors);
- API36 **31/31 standard + 1/1 isolated geometry**;
- signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

H26 is no longer PRE-GATE. The remaining RC3 work is physical/provider/audio residual homologation only.

Historical audit/checkpoint/alpha documents remain point-in-time evidence and are intentionally not globally rewritten. `H26D_MATERIALIZER_RECOVERY.md` is explicitly marked superseded by H26e.

## Producer identity rule
Documentation commits after #650 can advance repository HEAD but do not retroactively change the #650 APK producer. The signed candidate remains bound to `07c99155789774cb39f9b4382829f9e1d16649e3` and its exact signed SHA-256.