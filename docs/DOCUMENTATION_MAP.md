# Documentation map

Updated: 2026-09-16

## Active authoritative set
For the current RC line use:
- `CURRENT_STATE.md` — live evidence boundary and PRE-GATE state;
- `H27_BACKUP_HISTORY_RELEASE_UX.md` — authoritative current backup-history and release-copy corrective;
- `H26E_CI650_DIGITAL_PASS.md` — exact historical CI #650/H26e audit;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release path;
- `CI_PIPELINE.md` — manual CI, provenance discipline and materialization chain through H27;
- `H26_SAF_CLOUD_BACKUP.md` — stable backup architecture and H26→H27 supersession boundary;
- `UI_COPY_STYLE.md` — product-facing copy rules;
- `RELEASE_NOTES_0.5.0-rc3.md` — active RC3 behavior delta;
- `TEST_AND_HOMOLOGATION_PLAN.md` — H27 automated gate and next physical residual;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — final checklist awaiting binding to a passing signed H27 candidate;
- architecture/product/user-guide/candidate-identity and subsystem documents remain authoritative unless superseded above.

## Current evidence boundary
CI #650 / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` is the last signed DIGITAL PASS through H26/H26e. Its signed APK SHA-256 is `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

H27 changes source after physical backup feedback and is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Therefore the current repository HEAD may contain H27 while the last signed APK does not.

Historical audit/checkpoint documents remain point-in-time evidence and are intentionally not globally rewritten.

## Producer identity rule
A source/docs commit after #650 never retroactively changes the #650 APK producer. When H27 passes a new manual signed workflow, that new exact workflow producer/SHA and APK hash become the candidate identity.
