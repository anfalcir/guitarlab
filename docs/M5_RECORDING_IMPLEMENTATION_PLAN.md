# M5 recording implementation plan

Updated: 2026-09-09
Status: implementation complete; final physical M5 gate OPEN.

## Implemented architecture
- M5.A: recording transactions, safe temp/final promotion, float WAV writer, countdown/state policies.
- M5.B: Android capture engine, input selection/revalidation, metering and monitoring.
- M5.C: Studio coordinator for permission, exact-one-arm policy, countdown, capture, backing playback, partial-take safety, managed clip insertion, waveform generation and history integration.

## Invariants
- exactly one armed target for REC;
- no capture during countdown;
- route/permission/arm are revalidated before capture;
- invalid/zero-frame capture does not create a clip;
- valid partial media can be preserved after route failure;
- recorded media is project-managed and source bytes remain immutable after promotion;
- edits are metadata-only unless a dedicated derived-media process says otherwise;
- fine latency compensation is M6, not M5.

## Final consolidation
Alpha09 validated the integrated Studio with minor UX findings. Alpha10 approved Trim and clear/delete semantics but failed the two drag flows and Track Settings layout. Alpha11 is the corrective final candidate. No recording architecture redesign is part of alpha11; recording/countdown/take/waveform/duplex are regression gates.

## Exit
Do not mark M5 PASS/CLOSED until alpha11 is signed, identity-verified and physically accepted.
