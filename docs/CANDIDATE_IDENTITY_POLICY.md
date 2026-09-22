# Candidate identity policy

Updated: 2026-09-22

This policy defines the identity contract for the active GuitarLab homologation candidate. Historical alpha/RC identity notes are evidence only and do not override this file.

## Frozen U11 signed candidate
- `versionName`: `0.5.0-rc5`
- `versionCode`: `25`
- canonical branch: `main`
- package: `studio.guitarlab.app`
- signed artifact name: `GuitarLabStudio-0.5.0-rc5-homologacao.apk`
- expected homologation certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

U10/C8 is CLOSED / DIGITAL PASS on Android CI #781 / run `35791192802`, exact technical source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`.

U11 is CLOSED / DIGITAL PASS on exact producer `4218e4343746932a4de61c5abaa29ba5769a30ed`, Android CI #783 / run `35793456972`. This rc5/25 build is the current signed authority and the only candidate eligible for U12 physical homologation.

Signed identity:
- producer: `4218e4343746932a4de61c5abaa29ba5769a30ed`;
- unsigned APK SHA-256: `6af047a1f40b0e08382a5cca74890bc186aad68a3207a14e20a87cde5df9bc2c`;
- signed APK SHA-256: `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`;
- signed APK size: `82,894,480` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Exact-source rule

The exact U11 candidate source is the `github.sha` of the one freeze commit that requests the final signed gate and all required exact-source cloud verification. Documentation prepared before that run must not guess or predeclare the freeze SHA.

After the run, the unsigned release identity, `RELEASE_BUILD_IDENTITY.txt`, `BUILD_IDENTITY.txt`, checksum files, workflow metadata and downloaded signed APK must all agree on:
- exact producer SHA;
- package;
- versionName;
- versionCode;
- unsigned APK SHA-256;
- signed APK SHA-256;
- signer certificate.

The optimized release pipeline compiles the release package once in the software gate while signing credentials are absent. After the independent software and API 36 gates pass, the signing job downloads that exact unsigned artifact, verifies its checksum and identity, aligns/signs it, and verifies the final signer/application identity. The signing job must not recompile source.

## Required U11 identity gate

A deliverable is eligible for U12 physical homologation only when the same canonical U11 Android run proves:
1. software gate PASS, including unsigned release assembly;
2. staged unsigned release SHA-256 and identity are bound to the run `github.sha`;
3. API 36 regression PASS, including isolated target-tablet geometry;
4. exact tested-artifact signing PASS;
5. zipalign/package/version verification PASS;
6. signer certificate matches the locked SHA-256 above;
7. signed APK SHA-256 is recorded;
8. `BUILD_IDENTITY.txt` and checksum evidence agree with workflow metadata;
9. the same freeze SHA also has the required U11 real-cloud separation smoke and backend/security verification PASS.

Any mismatch blocks delivery. A successful build from a different SHA, version or signer is not the active candidate. A later docs-only commit never changes the frozen APK producer identity.
