# Current State — GuitarLab Studio

Updated: 2026-10-07

## Status

**RC35 RECOVERY ORDER PROBE — IMPLEMENTED / SIGNED CI PENDING / PHYSICAL DIAGNOSTIC PENDING**

The current source candidate is `0.5.0-rc35` / versionCode `55`. RC34 / Android CI #978 is the latest signed digital PASS.

## RC34 physical finding

Owner evidence `GuitarLab-Diagnostics-1791411563637.zip` on Samsung SM-X230 / Android 16 with MAIN=M-VAVE MK-300 and CUE=wired headset isolates the acoustic failure more precisely:

- A / MAIN-only: MAIN is audible.
- B / `setCommunicationDevice(CUE)`, with no CUE AudioTrack: MAIN remains audible.
- C / `USAGE_VOICE_COMMUNICATION` AudioTrack enters `play()` while receiving silence: MAIN becomes inaudible.
- D / CUE receives an 880 Hz signal: CUE is audibly effective while MAIN remains inaudible.
- During C the framework still consumes MAIN at approximately real-time rate, reports MAIN routed to MK-300, keeps STREAM_MUSIC unmuted, and shows no GuitarLab route/drift/backpressure suppression.

This disproves the claim that selecting a communication device alone is sufficient to suppress MAIN. It also shows that a logically routed and advancing MAIN AudioTrack can become acoustically ineffective below the application-visible routing layer once the communication AudioTrack is active.

Earlier owner evidence remains important: GuitarLab has already physically produced the desired simultaneous state (reference guitars on wired CUE, remaining mix on MK-300 MAIN) for several seconds before an older false drift guard suppressed CUE. The target hardware is therefore not classified as absolutely incapable; deterministic state/order remains the open problem.

## Why RC35 exists

The rc34 recovery check was confounded: its mid-D callback called `setPreferredDevice(MAIN)` and immediately called `communicationSession.reassert()`, which invokes `setCommunicationDevice(CUE)` again. The result therefore proved only that the combined MAIN-then-CUE reassertion did not recover MAIN.

RC35 separates these operations into independent phases while keeping both test tones active:

- **A** — MAIN 440 Hz only.
- **B** — select CUE communication device; no CUE AudioTrack.
- **C** — open/play CUE communication AudioTrack with digital silence; no extra reassertion.
- **D** — enable CUE 880 Hz; no route reassertion.
- **E** — call **only** `mainTrack.setPreferredDevice(MAIN)`; do not touch communication selection.
- **F** — call **only** `communicationSession.reassert()`; do not touch MAIN preference.

A–D last about 1.5 s; E/F about 2.2 s to make recovery/reversal audible.

## Evidence captured

Per phase the probe records:
- effective MAIN/CUE physical route keys and communication device;
- playback-head progress and accepted samples;
- zero/short writes and underruns;
- sample rates;
- MUSIC/VOICE_CALL volume + mute state;
- whether the E MAIN reassertion was accepted;
- whether the F communication reassertion was accepted.

Owner observations are recorded separately:
1. first phase A–F where MAIN becomes inaudible (or never/uncertain);
2. audible pair during D;
3. audible pair during E after MAIN-only reassertion;
4. audible pair during F after communication-only reassertion.

The diagnostic export `audio-communication-probe.json` uses schemaVersion 2.

## Interpretation target

The high-value outcome is:

`D=CUE_ONLY → E=BOTH → F=CUE_ONLY`

That would show that MAIN can be recovered after the communication track is active by reaffirming only MAIN, and that reasserting communication again reverses that recovery. It would provide a concrete candidate startup sequence for the real Studio.

Other outcomes remain useful:
- E=MAIN_ONLY: MAIN recovery destroys CUE, so public routing remains mutually exclusive in that state.
- E=CUE_ONLY: MAIN-only reassertion is insufficient.
- E=BOTH and F=BOTH: once recovered, communication reassertion is harmless.
- D=BOTH: the simplified probe already reaches the desired physical state, shifting attention back to Studio startup sequencing.

## Safety

The probe remains diagnostic-only. Normal Studio playback, fail-closed physical-route rules, zero-duck policy, mixer semantics, drift policy and the accepted RC20 baseline are unchanged. No automatic production workaround is introduced until physical evidence proves the recovery sequence.

## Residual gate

RC35 must pass the signed Android pipeline and then be run with MAIN=MK-300 / CUE=wired headset. Export a fresh diagnostic ZIP after recording all four acoustic answers.

USB multichannel MAIN 1/2 + CUE 3/4 remains the deterministic professional fallback roadmap if the public Communication Split path cannot be made reproducible.
