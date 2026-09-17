# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; residual physical closure active.
- **M8 — Release hardening:** H27 signed DIGITAL PASS at #651; **H28 backup identity/provider-consistency corrective PRE-GATE**.

## Last signed authority
**CI #651** / run `35166195527` / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9` is the last signed DIGITAL PASS, through H27.

Signed APK SHA-256: `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

## H28 — implemented / source-validated / PRE-GATE
Physical feedback from #651 triggered a release-critical consistency correction:
1. immutable project identity is explicitly `GuitarProject.id`, independent of project name;
2. rename remains one project; Duplicate and independent restore intentionally receive new IDs;
3. new backups persist deterministic revision IDs derived from project ID + edit timestamp + canonical state digest;
4. package SHA-256 remains a separate integrity identity;
5. same-timestamp but different project states remain distinct revisions;
6. revision directories become deterministic and retry-idempotent;
7. a successful write is confirmed from the exact document URIs, not an immediately refreshed parent listing;
8. targeted settling lookup prevents duplicate upload when a cloud provider exposes the revision late;
9. H26/H27 v1 backups remain readable and incrementally compatible.

## Canonical materialization tail
`… → H25 → H26 → H26a → H26b → H26e → H27 → H28`.

H28 decoded patch SHA-256: `3d06aa851ad1dc88dd078d60bb24ea097bbca7ef4f3e93089f47c9bd4a0a6e71`.

## Next release path
1. Publish H28 with `[skip ci]`; do not auto-dispatch Actions.
2. User manually runs one fresh signed `GuitarLab Android CI`.
3. Audit JVM/API36 counts, Lint/build/provenance, H28 terminal materializer message and exact signed identity.
4. If green, promote H28 to DIGITAL PASS and bind final physical homologation to that exact APK.
5. Physical residual focuses on: empty-folder first backup, immediate unchanged retry, rename → backup, edit/save → backup, bounded history, restore and provider failure/revocation.
6. Complete retained audio/edit/export smoke only on the exact final candidate.
7. RC3 FINAL requires no repeatable P0/P1 and explicit approval of the exact signed APK SHA-256.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary source/docs commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction.
