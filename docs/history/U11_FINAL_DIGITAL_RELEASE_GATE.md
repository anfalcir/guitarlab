# U11 — Final Digital Release Gate — GuitarLab

Updated: 2026-09-22
Status: **CLOSED / DIGITAL PASS — SIGNED CANDIDATE FROZEN**
Candidate: `0.5.0-rc5` / versionCode `25`
Package: `studio.guitarlab.app`
Canonical branch: `main`

## Objective

Freeze and sign one exact candidate for the consolidated U12 physical campaign.

U10/C8 is already CLOSED / DIGITAL PASS on Android CI #781 / run `35791192802`, exact technical source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`.

The frozen U11 producer SHA is `4218e4343746932a4de61c5abaa29ba5769a30ed`. No later documentation-only commit changes this APK producer identity.

## Preserved prerequisite evidence

### U10 / C8
- exact technical source: `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`;
- Android CI #781 / run `35791192802`: PASS;
- deterministic materialization through U10zb: PASS;
- software/Lint/build: PASS;
- API36: 23/23 classified instrumented classes;
- 76 observed instrumented tests across 5/5 groups: PASS;
- 24/24 required screenshots retained;
- isolated 1920×1200 geometry: PASS;
- deterministic visual review: PASS;
- integration artifact digest: `sha256:1c355f88bef4e20393a5c26c39fb4db94da8383089023724988af55953f509e3`;
- exact-source artifact digest: `sha256:e17472db3fc95fe2ae2e49c56571f06f72a0465c4fbbeecb0ad14e7c0d02210f`.

### Drive v3 provider-real acceptance
U8m provider-real acceptance remains supporting authority because U10 does not replace the production Drive v3 transport/store contract:
- result: `U8m PASS · r_1790095960 · cleanup 8/8/14`;
- sanitized report SHA-256: `84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`.

The final U11 run still repeats current automated Drive/unit/integration regressions. A new provider-real destructive campaign is not required merely for a version/docs freeze.

### Latest signed historical authority
Until U11 closes:
- Android CI #669 / run `35591631207`;
- producer `14b69271f2ae04529fa14475e1a34f2fdd864553`;
- `0.5.0-rc4` / versionCode `24`;
- signed APK SHA-256 `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Pre-freeze audit

`docs/history/U11_SECURITY_DOCUMENTATION_AUDIT.md`: **PASS**.

Candidate identity, live-document consistency, U10zb materialization ready-state, credential hygiene, Drive `drive.file` scope and tuner exclusion were verified before this trigger.

## Freeze gates

The final freeze commit must trigger all exact-source gates on the same SHA.

### A. Android signed candidate gate
Trigger: `[run ci signed]`

Required:
- [ ] deterministic U10zb source materialization PASS;
- [ ] JVM/unit/audio/DSP/persistence/property/fuzz regressions PASS;
- [ ] reproducible performance evidence collected;
- [ ] Android Lint PASS / 0 errors;
- [ ] debug/release assembly PASS;
- [ ] exact unsigned release staged and checksummed;
- [ ] API36 grouped regression PASS;
- [ ] 23/23 classified instrumented classes or an explicitly explained larger valid count;
- [ ] isolated target-tablet geometry PASS;
- [ ] C8 screenshot matrix retained and reviewed;
- [ ] signing job downloads the exact tested unsigned artifact rather than recompiling;
- [ ] zipalign verification PASS;
- [ ] package = `studio.guitarlab.app`;
- [ ] versionName = `0.5.0-rc5`;
- [ ] versionCode = `25`;
- [ ] signer SHA-256 = `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- [ ] unsigned APK SHA-256 recorded;
- [ ] signed APK SHA-256 recorded;
- [ ] `BUILD_IDENTITY.txt` / checksum evidence agrees with workflow SHA and app identity.

### B. Real cloud separation smoke
Trigger: `[run u4 cloud]`

Required:
- [ ] keyless Google Cloud authentication PASS;
- [ ] current production six-stem smoke PASS;
- [ ] output contract validates all expected stems;
- [ ] no source change/deploy is performed by the smoke;
- [ ] workflow producer SHA equals the U11 freeze SHA.

### C. Backend/security verification
Trigger: `[run u7 cloud]`

Push-triggered U7 execution is verification-only and must not deploy.

Required:
- [ ] worker tests PASS;
- [ ] Functions tests PASS;
- [ ] schemas/scripts validation PASS;
- [ ] repository/backend secret-hygiene scan PASS;
- [ ] worker container build PASS;
- [ ] no cloud deploy job executes from the push freeze trigger;
- [ ] workflow producer SHA equals the U11 freeze SHA.

## Documentation/security freeze audit

Before the trigger:
- [ ] live authority docs consistently state U10 CLOSED / U11 ACTIVE;
- [ ] active candidate identity is rc5/25;
- [ ] latest signed historical authority is #669 until this gate succeeds;
- [ ] current materializer tail is U10zb;
- [ ] no live authority claims H37/U2/U8 is still a pending current milestone;
- [ ] U12 physical checklist contains only unproven physical claims;
- [ ] no committed keystore/private key/client secret/refresh token/service-account credential;
- [ ] Drive scope remains `drive.file`;
- [ ] no tuner functionality has been introduced.

## Freeze rule

After the final trigger commit:
- no production/source/build-identity change is allowed;
- a source/build-identity change requires a new U11 candidate SHA and fresh final gates;
- docs-only post-run evidence commits may advance `main` but never alter the frozen APK producer identity.

## Final closure evidence

All exact-source U11 workflows completed successfully on `4218e4343746932a4de61c5abaa29ba5769a30ed`:

- **GuitarLab Android CI #783 / run `35793456972`: SUCCESS**
  - software gate PASS;
  - Android Lint PASS;
  - debug/release assembly PASS;
  - exact unsigned candidate staged and checksummed;
  - API36 PASS with 23/23 classified instrumented classes and 76 observed tests across 5/5 groups;
  - 24/24 required screenshots retained;
  - isolated 1920×1200 tablet geometry PASS;
  - signed homologation job PASS;
- **U4 Cloud Integration Smoke #104 / run `35793456955`: SUCCESS**
  - real Cloud Run execution `gbw-demucs-mt954` completed;
  - six validated stems plus manifest PASS;
  - manifest SHA-256 `d90027a04034edf2d2235283eba60cac042daf627c489075ac27432bcb7a0471`;
  - source cleanup and post-validation purge idempotency PASS;
- **U7 Cloud Backend #62 / run `35793456941`: SUCCESS**
  - 14/14 Python worker/benchmark tests PASS;
  - 7/7 Functions policy tests PASS;
  - schemas/scripts validation PASS;
  - secret-hygiene scan PASS;
  - worker container build PASS;
  - deploy job SKIPPED as required for push-triggered verification.

Signed candidate identity:
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc5`;
- versionCode: `25`;
- unsigned APK SHA-256: `6af047a1f40b0e08382a5cca74890bc186aad68a3207a14e20a87cde5df9bc2c`;
- signed APK SHA-256: `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`;
- signed APK size: `82,894,480` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2 verification: PASS;
- zipalign verification: PASS.

Artifacts:
- signed homologation artifact id `10723945284`, digest `sha256:f8ec128067ed6703836b70ed76af21d69e3ba8a1c1a184e15994ed769a413bb4`;
- Android integration artifact id `10723736193`, digest `sha256:c9a867d4bf2e18cb8454accfdc5e26a1268b6b9b5ab496338af7978a3d47cebc`;
- unsigned release artifact id `10723480991`, digest `sha256:281355badfe8487e9e88ff221f6f24c4bde48aece4fb6d4f5bef182390cfd8ce`;
- source artifact id `10722659834`, digest `sha256:5d5b75aa00ffeef31694286ef4d1f199ee13067a4aa4775db42474c327b3e12d`.

The downloaded signed artifact was independently rehashed after extraction and matched `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`.

## Exit

**U11 is CLOSED / DIGITAL PASS.** The exact signed rc5 candidate is frozen at `4218e4343746932a4de61c5abaa29ba5769a30ed`. No source/build-identity change is allowed without creating a new candidate and rerunning U11.

The program advances to **U12 — consolidated final physical homologation** on Samsung SM-X230 + M-VAVE MK-300.
