# Implementation Roadmap

This roadmap defines delivery order and gates. Parallel development is allowed only when dependency risk is isolated and gate status remains explicit.

## M1 — Project/model foundation
Status: substantially implemented.

## M2 — Android audio path and hardware diagnostics
Status: **HOMOLOGATED / PASS** for Samsung SM-X230 (Android 16 / API 36) + Pocket Amp USB audio.
Evidence covers routing, Play, Record, Duplex, disconnect/reconnect, permission denial and repeated stability. See `M2_HOMOLOGATION_EVIDENCE.md`.

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
11. real managed-WAV playback engine, hardware-clock playhead, stop and loop — software CI green at prior checkpoint;
12. mustard trim marker heads bound to non-destructive metadata editing with draft/apply/cancel and immutable-source bounds — current software checkpoint;
13. physical tablet playback/loop/seek + trim ergonomics validation — next gate;
14. signed consolidated alpha05 candidate and short M4 homologation;
15. merge draft PR #1 to `main` after the M4 consolidation gate is green.

M4 playback/trim rules:
- source is always the immutable managed copy;
- hardware-presented frames drive playhead state;
- no UI timer may masquerade as the audio clock;
- marker/import/clip edits are STOPPED-only;
- an open trim draft temporarily blocks playback and unrelated edits until Apply/Cancel;
- moving T◀ updates timeline start and source offset together; moving T▶ updates visible length;
- trim may reveal only frames that already exist inside the immutable source;
- sample-rate mismatch remains explicitly unsupported until resampling passes its own gate;
- record stays disabled because recording belongs to M5.

M4 exit gate: supported import into immutable managed storage, persist/reopen, waveform render, reliable real playback/seek/loop, non-destructive clip edits through clear marker controls, and no project/source corruption.

## M5 — Recording workflow
Planned: arm tracks, record project-managed takes, monitoring, timeline placement, disconnect-safe finalization. Recording uses canonical red record semantics and the shared marker contract.

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
- `main` remains the stable baseline until the current consolidation gate is physically green.
- Routine development must not trigger signed homologation unnecessarily.
- Timeline UX follows `TIMELINE_INTERACTION_GUIDELINES.md`.
- Managed media follows `MANAGED_MEDIA_POLICY.md`.
