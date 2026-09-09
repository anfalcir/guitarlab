# Implementation roadmap

Updated: 2026-09-09

## Status vocabulary
`PASS/CLOSED` means physical exit criteria were accepted. `IMPLEMENTED` means software exists but may still depend on a later physical gate. `OPEN` means the milestone cannot be closed yet.

## M1 — Foundation
Status: implemented/historical foundation.

## M2 — Hardware/audio baseline
Status: **PASS/CLOSED**. Samsung SM-X230 + Pocket Amp USB evidence is preserved in `M2_HOMOLOGATION_EVIDENCE.md`.

## M3 — Codec/import foundation
Status: **IMPLEMENTED**. WAV-first managed import/metadata/waveform foundation is complete; broader formats stay governed by `CODEC_SUPPORT_MATRIX.md`.

## M4 — Studio playback/mix foundation
Status: **IMPLEMENTED and absorbed by active branch**. Managed media, playback clock, timeline, loop/seek, non-destructive Trim, routing, Mixer/Master, metering and track management are established. Alpha05–alpha07 documents are historical, not active gates.

## M5 — Recording + Studio consolidation
Status: **OPEN, final physical gate pending**.

Implemented:
- recording transaction and safe managed media promotion;
- countdown, permission/route/arm revalidation;
- Android capture engine, monitoring/metering and managed takes;
- automatic clip/waveform integration and Undo/Redo;
- M5 Studio UX consolidation through alpha09/alpha10;
- alpha11 corrective shared drag coordinator/autoscroll + Track Settings layout.

Active exit gate:
1. alpha11 software gate green;
2. signed alpha11 identity/hash/certificate verified;
3. physical checklist on Samsung SM-X230, including Pocket Amp where relevant;
4. zero repeatable P0/P1;
5. explicit user approval;
6. only then mark M5 PASS/CLOSED and reassess draft PR #1.

## M6 — Latency/synchronization
Status: **BLOCKED by M5**. First work after M5 closure: real round-trip latency measurement, take placement compensation, jitter characterization and loopback tests. No M6 implementation before formal M5 closure.
