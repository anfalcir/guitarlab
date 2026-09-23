# GuitarLab Studio 0.5.0-rc9 — Release Notes

Updated: 2026-09-23
Status: **SIGNED DIGITAL PASS / U12 PHYSICAL HOMOLOGATION CANDIDATE**

## Identity

- package: `studio.guitarlab.app`
- versionName: `0.5.0-rc9`
- versionCode: `29`
- exact producer: `635124acfbf133553a96c8b2013f2245f58a6877`
- Android CI: #794 / run `35856278980` — PASS
- U4 Cloud Integration Smoke: #111 / run `35856278976` — PASS
- U7 Cloud Backend: #69 / run `35856279117` — PASS, deploy skipped
- signed artifact: `GuitarLabStudio-0.5.0-rc9-homologacao` / id `10748475073`
- signed APK SHA-256: `4fcf529b935a584217b3b882ca8dfa05e3c360f99cfe9d70071080175439dd6c`
- signed APK size: `79,945,360` bytes
- artifact ZIP SHA-256: `86077306c01afc80a07ba10b14080aec9fdb1a6a2e6416ef193bad190d274f53`
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

A later documentation-only commit does not change this producer identity.

## Why rc9 exists

Rc8 fixed persistence-before-scheduling and state-directed reboot/cancellation recovery, but target-device testing exposed another orphan class: a local durable separation could remain in `CANCEL_REQUESTED` while no corresponding remote Firestore/Cloud Run job existed.

Because rc8 treated the local nonterminal record as proof of an active operation, project recovery was blocked and cancellation could remain requested indefinitely.

## Corrective behavior

Rc9 makes local↔remote reconciliation explicit:

- missing remote + `UPLOADING/READY/QUEUED`: replay upload/enqueue idempotently;
- missing remote + `CANCEL_REQUESTED`: terminalize local job as `CANCELLED`;
- missing remote + `RUNNING/COMPLETED/IMPORTING`: terminalize as `EXPIRED`;
- cancellation retry exhaustion: terminalize as failure instead of leaving `CANCEL_REQUESTED`;
- general separation worker retry exhaustion: terminalize safely;
- restore `SOURCE_READY` only when the terminal job belongs to the current source generation and no newer active separation exists;
- prefer active job generation over a late terminal record from an older job when presenting the current project state;
- preserve the accepted source throughout orphan recovery;
- keep raw backend error codes out of the primary Prepare UX.

## Regression coverage

Rc9 adds deterministic coverage for:
- the exact physical failure: `CANCEL_REQUESTED + backend.status()==null`;
- `RUNNING + remote missing`;
- `COMPLETED/IMPORTING + remote missing`;
- idempotent replay of pre-dispatch missing-remote states;
- bounded cancellation failure;
- source-generation ownership protection;
- late old-terminal vs new-active race;
- API36 recovered-orphan UI with source preserved and retry available.

The standard software/Lint/build/API36/signing gates also passed, and the same producer passed the real-cloud U4 contract.

## Physical retest

The first rc9 physical action is not a clean-project smoke. Upgrade/install rc9 over the actual rc8 project that is stuck at “Cancelamento solicitado…”. Opening Prepare must converge that orphan to a terminal state, preserve the accepted source and expose a retry action.

Then:
1. start a new separation;
2. confirm the new identity reaches Firestore/Functions/Cloud Run;
3. background/foreground and reboot while pending;
4. exercise cancel-before-dispatch or cancel-in-flight;
5. confirm old terminal state never masks the new active generation;
6. continue only the remaining U12 hardware/subjective checks.

Rc8 and earlier candidates are historical evidence only and are not eligible for final U12 approval.
