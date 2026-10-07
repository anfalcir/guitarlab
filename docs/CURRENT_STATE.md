# Current State — GuitarLab Studio

Updated: 2026-10-07

## Status

**RC34 COMMUNICATION PHASE PROBE — IMPLEMENTED / SIGNED CI PENDING / PHYSICAL DIAGNOSTIC PENDING**

The current source candidate is `0.5.0-rc34` / versionCode `54`. RC33 / Android CI #974 remains the last signed digital PASS until this exact probe candidate completes the signed workflow.

## Why RC34 exists

Owner testing of signed rc33 produced a repeatable pattern on Samsung SM-X230 / Android 16:

- with no track routed to CUE, normal MAIN playback is fully audible;
- when at least one track is routed to CUE, that track is audible on the secondary wired output while the MAIN program becomes inaudible;
- with MK-300 as MAIN and wired headset as CUE, Android still reports distinct logical routes and rc33 reports `ROUTE_QUALIFIED` without drift, backpressure or route-loss suppression.

This proves the remaining question is no longer startup/drift qualification. It is **which communication-routing transition causes MAIN to become acoustically ineffective despite the framework continuing to report a valid MAIN route**.

## RC34 diagnostic probe

Options exposes an explicit, audible four-phase probe after a Communication Split pair is validated:

- **A — MAIN only:** 440 Hz low-level tone on the selected MAIN; no communication device selected by the probe and no CUE AudioTrack.
- **B — communication device selected:** MAIN tone continues; the selected CUE endpoint becomes the Android communication device, but no CUE AudioTrack exists yet.
- **C — CUE AudioTrack active with silence:** MAIN tone continues; a `USAGE_VOICE_COMMUNICATION` CUE track is opened/played but receives silence.
- **D — both active:** MAIN continues at 440 Hz and CUE receives a distinct 880 Hz low-level tone.

Each phase lasts about 1.4 s. The user records the first phase in which MAIN becomes inaudible (A/B/C/D), or reports that MAIN remained audible / could not be determined.

The probe records per phase expected/effective physical route keys, communication-device key, AudioManager mode, playback-head movement, samples accepted, zero-write counts, actual rates and STREAM_MUSIC / STREAM_VOICE_CALL volume state. The result is exported as `audio-communication-probe.json` and journaled.

The probe is diagnostic only: it does not promote a logically routed pair to physical/acoustic support.

## Safety

Probe tones are intentionally low level (~−24 dBFS), short and user-triggered. The probe runs only after explicit MAIN+CUE validation, owns/restores the communication session, and releases both AudioTracks in all completion/error paths. Existing fail-closed Studio routing, zero-duck invariant and mixer semantics are unchanged.

## Interpretation target

- MAIN disappears in **B** → `setCommunicationDevice()` / communication-device arbitration is sufficient to suppress effective MAIN.
- MAIN survives B but disappears in **C** → opening/playing `USAGE_VOICE_COMMUNICATION` activates the conflicting AudioPolicy/HAL path.
- MAIN survives C but disappears in **D** → the conflict begins only when the communication stream carries real signal.
- MAIN survives all phases → the full Studio/mixer/runtime path, not the basic public communication routing primitive, remains the next suspect.

## Accepted baseline and roadmap

`RELEASE_BASELINE.md` remains authoritative for the accepted RC20 baseline. USB multichannel MAIN 1/2 + CUE 3/4 remains the preferred deterministic professional roadmap when supported by hardware.
