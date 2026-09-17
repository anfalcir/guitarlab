# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Last signed digital homologation — CI #651
Run `35166195527`, producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9`, is the last signed DIGITAL PASS through H27.

Signed APK SHA-256: `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

Physical backup validation of this build exposed an eventual-consistency defect between successful provider writes and immediate directory-list confirmation, so #651 is not the final backup candidate.

## H28 corrective — PRE-GATE
Prepared for the next signed candidate:
- immutable project identity uses the existing `GuitarProject.id`, so rename never creates a separate backup project;
- new backups persist deterministic revision identity tied to edit timestamp plus canonical project-state digest;
- package SHA-256 remains independent integrity evidence;
- deterministic revision paths make a retry naturally idempotent;
- remote commit confirmation re-reads the exact URIs written instead of depending on immediate listing visibility;
- targeted settling lookup checks for a just-published revision before uploading another copy;
- legacy H26/H27 backups remain readable;
- deduplication distinguishes different H28 states even if they happen to share the same edit timestamp.

## Validation state
H28 source/materializer application, exact final hashes, idempotency, corruption fail-closed behavior, diff/shell checks and focused Kotlin compilation checks have passed locally.

Full Gradle/Lint/API36/signing gates have **not** been claimed for H28. A fresh manually dispatched signed CI is required.
