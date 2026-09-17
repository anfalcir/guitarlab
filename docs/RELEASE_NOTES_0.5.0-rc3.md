# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Last signed digital homologation — CI #650
Run `35154021384`, producer `07c99155789774cb39f9b4382829f9e1d16649e3`, remains the last signed DIGITAL PASS through H26/H26e.

Signed APK SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

Physical backup validation of this build exposed a release-semantic defect, so #650 is no longer the candidate for final backup approval.

## H27 corrective — PRE-GATE
Prepared for the next signed candidate:
- repeated backup of an unchanged saved project no longer creates another version;
- manual and automatic backup share revision-based incremental behavior;
- version history is bounded by a **maximum** per project rather than a protected minimum;
- duplicate copies of the same saved revision are cleaned automatically during a safe run;
- the newest valid version is always preserved;
- existing configured version count migrates to the new maximum-history meaning;
- last-run status and partial failure details identify what actually happened;
- backup UI is written for a normal end user: internal storage/API/integrity protocol terms are removed from ordinary screens;
- general settings no longer hard-code the user's current hardware model in device-detection status;
- ordinary media-readiness copy avoids implementation terms when a user-actionable explanation is sufficient.

## Validation state
H27 source/materializer integrity, idempotency, corruption fail-closed behavior, focused backup-domain runtime logic and release-copy static checks have passed locally. Full Gradle/Lint/API36/signing gates have **not** been claimed for H27 yet.

A fresh manually dispatched signed CI is required. CI #650 remains historical authority for the exact H26e source it tested and must not be represented as H27 evidence.
