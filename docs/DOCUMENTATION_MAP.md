# Documentation map

Updated: 2026-09-16

## Active authoritative set
For the current RC line use:
- `CURRENT_STATE.md` — live evidence boundary and current PRE-GATE state;
- `H28_BACKUP_IDENTITY_CONSISTENCY.md` — authoritative current backup identity/provider-consistency corrective;
- `H27_BACKUP_HISTORY_RELEASE_UX.md` — H27 history semantics/release-copy corrective and #651 predecessor;
- `H26E_CI650_DIGITAL_PASS.md` — historical CI #650/H26e audit;
- `IMPLEMENTATION_ROADMAP.md` — milestone/release path;
- `CI_PIPELINE.md` — manual CI, provenance discipline and materialization chain through H28;
- `H26_SAF_CLOUD_BACKUP.md` — original backup architecture and supersession boundary;
- `UI_COPY_STYLE.md` — product-facing copy rules;
- `RELEASE_NOTES_0.5.0-rc3.md` — active RC3 behavior delta;
- `TEST_AND_HOMOLOGATION_PLAN.md` — H28 automated gate and physical residual;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — final checklist awaiting a passing signed H28-or-later candidate.

## Current evidence boundary
CI #651 / run `35166195527` / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9` is the last signed DIGITAL PASS through H27. Signed APK SHA-256: `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

H28 changes production source after target-device provider-consistency feedback and is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Therefore current repository HEAD may contain H28 while the last signed APK does not.

Historical audit/checkpoint documents remain point-in-time evidence and are intentionally not globally rewritten.

## Producer identity rule
A source/docs commit after #651 never retroactively changes the #651 APK producer. When H28 passes a new manual signed workflow, that new exact workflow producer SHA and APK hash become the candidate identity.
