# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-14

This is the only active residual physical checklist for the digitally homologated H0–H10 RC3 candidate.

## Candidate identity — LOCKED
- versionName: `0.5.0-rc3`
- versionCode: `23`
- package: `studio.guitarlab.app`
- exact application/source SHA: `abc0e2a9f8708dd141735915898b508ce0948f48`
- canonical workflow: CI #617 / run ID `34918430241`
- signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`
- signed APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`
- unsigned release SHA-256: `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`
- signed artifact ID: `10377695262`
- target: Samsung SM-X230 + M-VAVE MK-300 over USB
- MK-300 hardware USB loopback: disabled

CI #617 passed the software gate, complete API 36 instrumentation, isolated 1920×1200 tablet geometry and signing/provenance matrix for this exact source. `BUILD_IDENTITY.txt` records `software+android-integration-passed;physical-validation-pending`.

Do not substitute another APK, source SHA, package, version, checksum or signer during this physical pass.

## A. Upgrade and project integrity
- Install this signed RC3 over the previous official candidate.
- Open an existing project and confirm tracks, clips, takes, markers and sections remain intact.
- Close/reopen the app and project and confirm persistence.

## B. First-tap editing and trim ergonomics
- Open a clip's pencil/actions menu and tap `Cortar` once.
- Repeat several times across different clips.
- Confirm the first valid tap always opens Trim; if editing is blocked, the app explains why instead of doing nothing.
- Move both start/end handles several times, including when close together.
- Apply, Undo, Redo and save/reopen.

PASS: no missed first tap, no silent no-op, both handles remain independently usable and retained region survives history/persistence.

## C. Level analysis convergence + history stress
On one guitar track:
- run level analysis and note recommendation;
- apply the recommendation;
- analyze again;
- confirm the same correction is not proposed again merely because source PCM is unchanged;
- perform several analyze/apply cycles if useful, then multiple Undo/Redo operations;
- switch to another project and back;
- verify Undo/Redo and transport remain responsive.

PASS: effective level converges, history buttons never become permanently stale/locked, and no result from one project appears in another.

## D. Clip vs track deletion semantics
- confirm `Excluir clipe` removes only the selected clip/segment after confirmation;
- confirm track-wide content removal is clearly labeled `Limpar toda a pista` in track configuration;
- cancel one destructive action and verify strict no-op;
- split/move/delete one recorded child and save/reopen.

PASS: destructive scope is unambiguous, no sibling/take corruption, no generic repository-validation error.

## E. Stop during recording
- arm one My Guitar track and start REC;
- during countdown press Stop once: countdown must cancel safely;
- start REC again, allow capture to begin, then press Stop;
- repeat using REC itself to stop capture;
- compare both results.

PASS: Stop and REC both finalize capture successfully through equivalent behavior; no double take, corrupt WAV, stuck REC state or duplicate finalization.

## F. Mixer-header workspace
- open Mixer in the normal tablet layout;
- confirm `Mixer` remains anchored left, practice controls occupy the central header area and Pin/Close remain accessible at right;
- exercise Comparison/Timeline controls;
- close Mixer and confirm the same practice controls return to workspace flow without duplicated state or layout collision.

PASS: reclaimed vertical space is real, controls do not overlap, shift unexpectedly or diverge between open/closed Mixer states.

## G. Live REC waveform stability
Record continuously for at least 2–3 minutes.
- observe early material while recording continues;
- confirm historical material does not progressively become sparse while recent material becomes increasingly dense;
- confirm the right edge follows recorded duration monotonically;
- confirm no backward piling, acceleration, long freeze/catch-up burst or visible timing migration;
- stop and compare transient live waveform with finalized waveform timing.

PASS: waveform density/time placement remains spatially coherent over the whole take and finalized media does not move clip timing.

## H. MK-300 routing and synchronization
- explicitly select MK-300 input/output and keep hardware loopback disabled;
- record a rhythm part against backing;
- confirm backing is not printed into the guitar take;
- confirm guitar is not systematically late relative to backing;
- disconnect selected input during a disposable take and verify fail-closed behavior;
- listen for repeatable pops/dropouts/wrong speed or one-sided output.

## I. Focused smoke
- Auto seções preview/application;
- one Loop pass and live playhead seek;
- at least two takes and active-take switch;
- one short WAV or FLAC export and playback.

## PASS criteria
- exact CI #617 identity above remains intact;
- no repeatable P0/P1 in sections B–I;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of this exact signed APK.

Do not manually duplicate deterministic model/file/math/API36 checks already covered by CI #617.
