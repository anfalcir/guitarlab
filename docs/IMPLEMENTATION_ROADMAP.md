# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; residual physical closure active.
- **M8 — Release hardening:** H26/H26e signed DIGITAL PASS at #650; **H27 backup-history/release-UX corrective PRE-GATE**.

## Last signed authority
**CI #650** / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` remains the last signed DIGITAL PASS, through H26/H26e only.

Its exact signed APK SHA-256 is `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

## H27 — implemented / source-validated / PRE-GATE
Physical feedback from #650 triggered a narrow but release-critical correction:
1. unchanged manual backup is idempotent instead of forcing a duplicate version;
2. automatic + manual runs share one revision-based incremental rule;
3. configured history count is a maximum per project;
4. duplicate copies of the same persisted revision are collapsed;
5. newest valid version is always preserved;
6. legacy count preference migrates to the new maximum-history meaning;
7. partial failure names the affected project and records last execution accurately;
8. backup/settings/help/readiness UI copy is hardened for a general end-user release and avoids internal APIs, release vocabulary or personal/model-specific status copy.

## Canonical materialization tail
`… → H25 → H26 → H26a → H26b → H26e → H27`.

H27 patch SHA-256: `c80f0b04f34fb1b92ea47c13f7eb70df6744e0c07392e81391875f3b10aa85e5`.

## Next release path
1. Publish H27 with `[skip ci]`; do not auto-dispatch Actions.
2. User manually runs one fresh signed `GuitarLab Android CI` when ready.
3. Audit actual JVM/API36 counts, Lint/build/provenance, H27 terminal materializer message and exact signed identity.
4. If green, promote H27 to DIGITAL PASS and bind `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` to that exact new APK.
5. On the new candidate, verify: repeated unchanged manual backup creates no duplicate; a saved project revision creates one new version; history never exceeds the selected maximum after a safe run; pre-existing same-revision duplicates are cleaned; partial failure identifies the project without deleting that project's older backups.
6. Complete the remaining provider/device and retained audio/editing smoke only on the H27 candidate or a later exact successor.
7. Final RC3 approval requires no repeatable P0/P1 and explicit approval of the exact final signed APK SHA-256.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary source/docs commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction.
