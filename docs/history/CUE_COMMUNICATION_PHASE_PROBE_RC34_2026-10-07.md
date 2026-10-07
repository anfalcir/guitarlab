# Communication Split A/B/C/D phase probe — RC34

Updated: 2026-10-07
Status: **IMPLEMENTED / SIGNED CI PENDING / PHYSICAL DIAGNOSTIC PENDING**

## Trigger

Signed rc33 eliminated the earlier false drift/backpressure behavior, but owner testing still shows that enabling any CUE-routed track makes the MAIN program acoustically disappear while the CUE track remains audible. With MK-300 MAIN + wired CUE, Android continues to report distinct logical routes and advancing playback.

The remaining diagnostic question is which public-API transition activates the conflict.

## Probe contract

A user-triggered Options action runs four audible phases:

A. MAIN 440 Hz tone only.
B. Same MAIN tone after selecting the CUE endpoint with `setCommunicationDevice()`; no CUE AudioTrack exists.
C. MAIN tone continues while a `USAGE_VOICE_COMMUNICATION` CUE AudioTrack is playing silence.
D. MAIN 440 Hz and CUE 880 Hz play simultaneously.

Each phase is bounded (~1.4 s) and low-level (~−24 dBFS). The user reports where MAIN first becomes inaudible. The app records logical route/head/write/volume evidence per phase and exports `audio-communication-probe.json`.

## Interpretation

B failure isolates communication-device arbitration.
C failure isolates activation of a communication AudioTrack.
D failure isolates signaled communication playback.
No failure in A-D sends investigation back to the full Studio/mixer/runtime path.

The probe does not claim acoustic success from `getRoutedDevices()` and does not alter production CUE safety policy.


## Refinement before signed qualification

B and C intentionally do not reassert MAIN before measurement. D runs ~2.4 s and performs one MAIN preference reassertion halfway through. The owner records three independent observations: first MAIN-loss phase, CUE 880 Hz audibility in D, and MAIN audibility after the reassertion. Per-phase evidence additionally includes zero/short writes, underruns and MUSIC/VOICE_CALL mute state.
