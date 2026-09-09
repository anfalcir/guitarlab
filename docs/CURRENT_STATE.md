# Current state

Updated: 2026-09-09

## Repository truth
- stable baseline: `main`, signed `0.2.0-alpha03`;
- active branch: `dev/parallel-m3-m5` in draft PR #1;
- active candidate line: `0.2.0-alpha11`, versionCode 12;
- canonical CI: diff sanity, unit tests, Android Lint, debug APK; signed homologation only on explicit gate;
- certificate SHA-256 is locked to `0762D4F3ECB8E1A9AAA4BDB1E098A666C9BA9BF8B71C4F14B0E7A7065404E181`.

## Milestones
- M2: PASS/CLOSED physically on Samsung SM-X230 / Android 16/API 36 + Pocket Amp USB.
- M3: WAV codec/import foundation implemented.
- M4: managed media, waveform, playback, timeline, Mixer/Master, metering, routing/options and product polish implemented.
- M5.A/B/C: recording transaction, capture engine, routing enforcement, countdown, managed take finalization, waveform insertion, duplex/backing and coordinator implemented.
- M5 final status: **OPEN — awaiting physical alpha11 validation**. Do not mark PASS/CLOSED yet.
- M6: blocked until M5 closes; first scope is measured round-trip latency, synchronization, take-position compensation, jitter and loopback.

## Physical evidence carried forward
- alpha09: broadly approved, with minor UX corrections identified.
- alpha10: Trim approved; distinction `Limpar pista` vs `Excluir pista` approved; both drag flows rejected; `Configurar pista` layout rejected/incomplete.
- video evidence `125843.mp4` (2026-09-09 handoff) reproduces the alpha10 drag failure: item detaches, ghost shifts/snaps and does not follow the pointer continuously.

## Alpha11 corrective scope
- one shared workspace drag coordinator for tracks and clips;
- no Popup ghost; overlay remains in the Compose workspace above all lanes;
- pointer-follow ghost independent from destination snapping;
- real-bounds target calculation using `LazyListState.layoutInfo`;
- continuous coroutine/frame autoscroll with proportional edge speed;
- one atomic metadata mutation at drop; cancel/no-op creates no history;
- Track Settings: balanced landscape columns, stacked portrait layout, source metadata, safe delete guidance;
- preserve approved Trim, recording, mixer, loop/playhead and clear/delete behavior.

## Active gate
Use only `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md`. M5 closes only after green CI + signed identity verification + explicit physical approval with zero repeatable P0/P1.
