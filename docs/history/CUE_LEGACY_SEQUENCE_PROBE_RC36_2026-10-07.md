# Legacy ordering probe — RC36

Updated: 2026-10-07
Status: **IMPLEMENTED / SIGNED CI PENDING / PHYSICAL DIAGNOSTIC PENDING**

RC35 proves in both route orientations that MAIN becomes acoustically ineffective when the communication AudioTrack is active and that MAIN-only reassertion does not recover it.

RC36 isolates the two remaining rc31 ordering differences.

## G
Select communication/CUE before opening either test track; then open MAIN MEDIA + CUE VOICE_COMMUNICATION, silent-prime at volume 0, play, reassert routes, stabilize muted and expose 440/880 Hz tones.

## H
First reconstruct the rc31 dual-MEDIA silent attempt, release it, then execute the exact G sequence.

Owner reports MAIN only, CUE only, both, none or uncertain after each scenario.

G=BOTH points to ordering. G=CUE_ONLY/H=BOTH points to media-first preconditioning. G=CUE_ONLY/H=CUE_ONLY rejects both identified differences as sufficient explanations. Any BOTH result requires repetition and stability qualification before product code changes.
