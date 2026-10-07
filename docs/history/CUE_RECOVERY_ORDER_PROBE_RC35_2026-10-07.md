# CUE recovery-order probe — RC35

Updated: 2026-10-07
Status: **IMPLEMENTED / SIGNED CI PENDING / PHYSICAL DIAGNOSTIC PENDING**

## Evidence opening RC35

The signed rc34 probe on SM-X230 / Android 16 with MK-300 MAIN + wired CUE isolates MAIN acoustic loss at phase C: selecting the communication device in B leaves MAIN audible, while playing a silent `USAGE_VOICE_COMMUNICATION` AudioTrack in C makes MAIN inaudible. Framework evidence still shows MAIN routed to MK-300, advancing near real time and unmuted.

The rc34 recovery result is not definitive because the probe reaffirmed MAIN and then immediately reaffirmed communication. Those actions must be separated.

## A–F contract

- A: MAIN 440 Hz only.
- B: select communication/CUE only.
- C: play silent CUE communication track; no reassertion.
- D: turn on CUE 880 Hz; no reassertion.
- E: reaffirm only MAIN via `setPreferredDevice(MAIN)`.
- F: reaffirm only communication/CUE via `setCommunicationDevice(CUE)`.

Both tones remain active through D/E/F. E and F are long enough for an owner to hear recovery/reversal.

## Owner observations

Record:
- first MAIN-loss phase A–F / never / uncertain;
- audible pair in D;
- audible pair in E;
- audible pair in F.

Pair values are MAIN only, CUE only, both, none or uncertain.

## Decisive pattern

`D=CUE_ONLY → E=BOTH → F=CUE_ONLY` would prove a reproducible order-sensitive recovery candidate: communication track active, then MAIN-only preference reassertion. A later Studio change could test that exact startup order, but rc35 itself remains diagnostic-only.

`E=MAIN_ONLY` indicates mutual exclusion after MAIN recovery; `E=CUE_ONLY` means MAIN-only reassertion is insufficient; `E=BOTH/F=BOTH` means recovered concurrency survives communication reassertion.

## Safety

No production routing, mixer, drift, focus, ducking or fallback behavior changes. All probe resources remain bounded and released, and the communication session is restored on exit.
