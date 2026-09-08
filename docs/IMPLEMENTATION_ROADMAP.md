# Implementation Roadmap

This roadmap defines delivery order and gates. Parallel development is allowed only when dependency risk is isolated and open hardware gates remain explicit.

## M1 — Project/model foundation
Status: substantially implemented.

## M2 — Android audio path and hardware diagnostics
Status: software implemented; Pocket Amp physical gate OPEN.
Exit gate: signed build plus full Pocket Amp physical checklist.

## M3 — Codec/import foundation
Status: WAV core + Android path implemented; broader format support remains planned.

## M4 — Studio timeline and import
Status: IN PROGRESS.
Sequence:
1. persisted project workspace and track lanes — done;
2. design system + launcher identity — done;
3. persistent non-destructive AudioClip model — done;
4. WAV import vertical slice — done;
5. immutable project-managed source ingestion — done;
6. source technical metadata — done;
7. clip-management basics + non-destructive trim core — done;
8. waveform envelope/cache/rendering — done;
9. marker-head interaction foundation + semantic colors — done;
10. transport state safety and STOPPED-only editing — done;
11. real managed-WAV playback engine, playhead clock, stop and loop — current checkpoint;
12. physical tablet playback/loop/seek validation — next gate;
13. bind trim marker heads to metadata-only trim mode;
14. timeline-scale movement/trim polish and tablet ergonomics;
15. M4 consolidation/homologation candidate after applicable gates.

M4 playback slice rules:
- source is always the immutable managed copy;
- hardware-presented frames drive playhead state;
- no UI timer may masquerade as the audio clock;
- sample-rate mismatch is an explicit unsupported condition until resampling passes its own gate;
- record stays disabled because recording belongs to M5.

M4 exit gate: supported import into immutable managed storage, persist/reopen, waveform render, reliable real playback/seek/loop, non-destructive clip edits through clear marker controls, and no project/source corruption.

## M5 — Recording workflow
Planned: arm tracks, record project-managed takes, monitoring, timeline placement, disconnect-safe finalization. Recording uses the canonical red record semantics and marker contract.

## M6 — Latency, synchronization and compensation
Planned: measured I/O/round-trip behavior, alignment and compensation.

## M7 — Mixing and editing maturity
Planned: gain/pan/mute/solo/meters, split/fades and larger-project performance.

## M8 — Multi-format interoperability and export
Planned and required: complete import matrix, validated resampling, WAV/FLAC and approved compressed export, mix/stems/ranges.

## M9 — Release hardening
Planned: migrations, performance, battery, accessibility, recovery and signed regression.

## Gate rules
- CI green means software gate only.
- Features are advertised only after applicable gates pass.
- `main` remains stable until required open gates are closed.
- Routine development must not trigger signed homologation unnecessarily.
- Timeline UX follows `TIMELINE_INTERACTION_GUIDELINES.md`.
- Managed media follows `MANAGED_MEDIA_POLICY.md`.
