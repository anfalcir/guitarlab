# GuitarLab 0.5.0-rc5 — U11 signed physical-homologation candidate

Updated: 2026-09-22  
Status: **SIGNED DIGITAL PASS — U12 PHYSICAL HOMOLOGATION CANDIDATE**

## Identity

- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc5`;
- versionCode: `25`;
- exact producer: `4218e4343746932a4de61c5abaa29ba5769a30ed`;
- Android CI: #783 / run `35793456972`;
- unsigned APK SHA-256: `6af047a1f40b0e08382a5cca74890bc186aad68a3207a14e20a87cde5df9bc2c`;
- signed APK SHA-256: `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`;
- signed APK size: `82,894,480` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2: PASS;
- zipalign: PASS.

## Digital gate

U11 closed all final digital release requirements on the same exact producer SHA.

Android CI #783:
- deterministic source materialization through U10zb PASS;
- unit/integration/property/fuzz and related software suites PASS;
- performance evidence collected;
- Android Lint PASS / 0 errors;
- debug/release build PASS;
- API36 PASS with 23/23 classified instrumented classes and 76 observed tests;
- 24/24 required cohesion screenshots retained;
- isolated 1920×1200 target-tablet geometry PASS;
- exact tested-artifact signing/provenance PASS.

Real cloud:
- U4 Cloud Integration Smoke #104 / run `35793456955` PASS;
- real `gbw-demucs` Cloud Run execution completed;
- six validated stems + manifest integrity PASS;
- cleanup/idempotent purge PASS.

Backend/security:
- U7 Cloud Backend #62 / run `35793456941` PASS;
- 14/14 Python worker/benchmark tests PASS;
- 7/7 Functions policy tests PASS;
- schemas/scripts validation PASS;
- secret-hygiene scan PASS;
- worker container build PASS;
- deploy job intentionally SKIPPED.

Drive:
- U8m provider-real acceptance remains valid supporting evidence:
  `U8m PASS · r_1790095960 · cleanup 8/8/14`;
- direct Drive API v3 remains the production backup path;
- OAuth scope remains `drive.file`.

## Product scope included

This candidate contains the unified GuitarLab product:
- Home project library and unified project shell;
- Prepare source search/import and remote six-stem separation;
- automatic managed backing/reference preparation;
- Studio recording/editing/takes/mixer/timing workflows;
- live recording waveform;
- unified Export workspace;
- unified Activity/background-operation history;
- direct Google Drive v3 backup/restore;
- responsive phone/tablet UI, accessibility and final cohesion hardening.

No tuner functionality is included.

Legacy standalone GBW/H37/pre-unification migration remains intentionally out of scope.

## Physical residual

Digital acceptance does **not** replace physical homologation.

U12 must validate the exact signed APK on:
- Samsung SM-X230 / Android 16 / API36;
- M-VAVE MK-300 over USB;
- hardware loopback OFF;
- intended real hub/power topology.

The physical campaign is limited to real routing/capture/isolation, monitoring, live waveform behavior, timing/listening, USB disconnect/reconnect, one continuous 10-minute session, real Prepare flow, real Drive user workflow and tablet ergonomics.

Canonical checklist: `docs/U12_FINAL_PHYSICAL_HOMOLOGATION.md`.

Any source/build-identity change after this candidate requires a new U11 candidate and fresh affected gates.
