# Implementation Roadmap

This roadmap defines delivery order and gates. Parallel development is allowed only when dependency risk is isolated and gate status remains explicit.

## M1 — Project/model foundation
Status: substantially implemented.

## M2 — Android audio path and hardware diagnostics
Status: **HOMOLOGATED / PASS** for Samsung SM-X230 (Android 16 / API 36) + Pocket Amp USB audio. See `M2_HOMOLOGATION_EVIDENCE.md`.

## M3 — Codec/import foundation
Status: WAV core + Android path implemented; broader format support remains planned.

## M4 — Studio timeline, playback and core mixing
Status: IN PROGRESS.

Completed sequence:
1. persisted project workspace and track lanes;
2. design system + launcher identity;
3. persistent non-destructive `AudioClip` model;
4. WAV import vertical slice;
5. immutable project-managed source ingestion;
6. source technical metadata;
7. clip-management basics + non-destructive trim core;
8. waveform envelope/cache/rendering;
9. marker-head interaction foundation + semantic colors;
10. transport safety and STOPPED-only editing;
11. real managed-WAV playback engine with hardware-clock playhead, stop and loop;
12. mustard trim marker draft/apply/cancel with immutable-source bounds;
13. single-screen timeline-first Studio redesign, loop-marker visibility fix and explicit trim confirmation;
14. Graphite Studio visual refresh + immersive fullscreen;
15. Mixer Dock with shared track selection and persisted gain/pan/mute/solo;
16. Options Center with global input/output setup, export placement and diagnostics;
17. stable-signature Android audio route preferences and runtime re-resolution;
18. selected main output applied to playback with controlled Auto fallback;
19. real master gain and master peak/RMS metering — software green in CI run #203;
20. per-track meters + peak hold/decay + durable project master gain with legacy compatibility — current checkpoint.

Next M4 gates:
21. CI-green validation of the current mixer/meter/persistence checkpoint;
22. prepare a new signed consolidation build from current branch head (the older alpha05 artifact predates recent Studio/Mixer work);
23. physical Samsung SM-X230 validation of workspace/fullscreen, routing, playback/loop/seek, trim, mixer controls, track/master meters and stability;
24. close any P0/P1 regressions found in the physical pass;
25. mark PR #1 ready and merge to `main` only after the M4 consolidation gate is green.

M4 invariants:
- managed source media is immutable;
- hardware-presented frames own playhead progression;
- marker/import/clip/mix edits are STOPPED-only in the current engine;
- loop markers are visible only while loop is enabled;
- trim is an explicit draft transaction with Apply/Cancel;
- global input/output setup belongs in Options, not per-track routing;
- raw Android device IDs are not durable route identity;
- master gain is project metadata and defaults to 0 dB for legacy projects;
- sample-rate mismatch remains unsupported until derived-media resampling passes its own gate;
- recording remains disabled until M5.

M4 exit gate: supported managed import, persist/reopen, waveform render, reliable real playback/seek/loop, non-destructive trim, functional track/master mixing controls, stable output routing behavior, useful metering, and no project/source corruption.

## M5 — Recording workflow
Planned: arm tracks, record project-managed takes, monitoring, timeline placement, disconnect-safe finalization. Recording uses canonical red record semantics and the global Studio input selected in Options.

## M6 — Latency, synchronization and compensation
Planned: measured I/O/round-trip behavior, alignment and compensation.

## M7 — Mixing and editing maturity
Planned beyond the M4 core mixer: automation/live-safe parameter changes, advanced metering, fades/split, optional processing, larger-project performance and workflow refinement.

## M8 — Multi-format interoperability and export
Planned and required: complete import matrix, validated resampling, WAV/FLAC and approved compressed export, mix/stems/ranges.

## M9 — Release hardening
Planned: migrations, performance, battery, accessibility, recovery and signed regression.

## Gate rules
- CI green means software gate only.
- Features are advertised only after applicable gates pass.
- `main` remains the stable baseline until the current consolidation gate is physically green.
- Routine development must not trigger signed homologation unnecessarily.
- Timeline UX follows `TIMELINE_INTERACTION_GUIDELINES.md` and `STUDIO_WORKSPACE_GUIDELINES.md`.
- Managed media follows `MANAGED_MEDIA_POLICY.md`.
- Studio routing/mixer placement follows `STUDIO_OPTIONS_AND_MIXER.md`.
