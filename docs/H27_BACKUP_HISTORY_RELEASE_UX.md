# H27 — Backup history semantics and release UX hardening

Status: **DIGITAL PASS at CI #651; provider-consistency behavior superseded by H28**
Updated: 2026-09-16

## Trigger and scope
H27 corrected issues found during physical validation of the H26e/#650 build:
- manual backup no longer intentionally forces a duplicate of an unchanged persisted revision;
- `1/3/5/10` history setting now means a maximum per project, not a protected minimum;
- safe retention collapses duplicate copies of the same legacy persisted revision;
- partial failure reports identify the affected project;
- ordinary UI copy was revised for an end user rather than exposing APIs, protocol terms, development context or personal hardware wording.

## Digital authority
CI #651 / run `35166195527` / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9` passed the full H27 gate:
- 287/287 JVM/unit PASS;
- Android Lint PASS;
- debug/release assembly and unsigned provenance PASS;
- 32/32 standard API36 PASS;
- 1/1 isolated 1920×1200 geometry PASS;
- signed provenance/package/version/zipalign/signature/certificate PASS.

Signed APK SHA-256: `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

## Physical supersession by H28
Physical backup testing of that exact H27 APK then exposed a different defect: a cloud-backed provider could successfully create the remote backup but delay its visibility in directory listings. H27 used that immediate relist as commit confirmation/dedup evidence, creating false failures and selective duplicates on retry.

H28 supersedes that provider-consistency behavior with stable project/revision identity, deterministic revision paths, direct-URI commit verification and targeted settling lookup. See `H28_BACKUP_IDENTITY_CONSISTENCY.md`.

## H27 materialization identity
H27 source part: `.source-parts/H27BackupReleaseUx.patch.gz.b64`

Decoded gzip SHA-256: `2cbfa3bf7d3291778d73d5a3ffdf04f2e16ebf0c129a893f807d9cc548f53c05`

Decoded patch SHA-256: `c80f0b04f34fb1b92ea47c13f7eb70df6744e0c07392e81391875f3b10aa85e5`

Historical terminal message: `Source patch chain materialized through H27 with verified final hashes`.

H27 remains valid digital evidence for the exact source tested by #651, but #651 must not be represented as containing H28 or as the final backup candidate.
