# RC20 Final Physical Homologation Record

Date: 2026-09-25  
Result: **PASS / OWNER ACCEPTED / RELEASE FROZEN**

This is the final acceptance record for the exact signed RC20 Android artifact. It is historical release evidence; current exact identities are also summarized in `../RELEASE_BASELINE.md`.

## Accepted artifact

- package: `studio.guitarlab.app`;
- version: `0.5.0-rc20` / versionCode `40`;
- producer commit: `76a832afdd8045ac944046dae2c31d8f6ec00716`;
- Android CI: #904 / run `36144821352` — PASS;
- unsigned APK SHA-256: `eb607853860c6c18508a0b401dcc0ac103a14d52cd9cbad52c827572e3682c29`;
- signed APK SHA-256: `0d5832666a00484635ef37daecb9bead021ed771ddd053b88d88191a5f029fd2`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Reference environment

- Samsung SM-X230;
- Android 16 / API 36;
- M-VAVE MK-300 over USB for the hardware-audio path;
- MK-300 hardware loopback OFF.

## Final residual under test

U12bx changed playback presentation timing only: visible playhead position now prefers stable AudioTimestamp-derived sink presentation timing, remains anchored before presentation begins, re-anchors after seek/flush and never advances beyond frames accepted by AudioTrack. Recording latency compensation was intentionally unchanged.

Owner result on 2026-09-25: **homologated**. No consistent perceptible offset between audible playback, waveform and playhead remained as a release blocker.

## Reused accepted evidence

Under the evidence-reuse policy, unaffected previously accepted physical evidence remains valid. In particular, U12bw waveform fidelity and selective “Recolocar referências no Studio” behavior had already been accepted by the owner before U12bx. The final candidate changed playback presentation-clock behavior without reopening unrelated product areas.

## Closure

This acceptance closes the final RC20 physical gate. No runtime rebuild is authorized by documentation finalization alone. Future runtime changes create a new candidate and follow `../TEST_AND_HOMOLOGATION_POLICY.md`.
