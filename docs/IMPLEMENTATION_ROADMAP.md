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
10. transport safety and STOPPED-owned structural editing;
11. real managed-WAV playback engine with hardware-clock playhead, stop and loop;
12. mustard trim marker draft/apply/cancel with immutable-source bounds;
13. single-screen timeline-first Studio redesign and loop-marker visibility fix;
14. Graphite Studio visual refresh + immersive fullscreen;
15. Mixer Dock with shared track selection and persisted gain/pan/mute/solo;
16. Options Center with global input/output setup, export placement and diagnostics;
17. stable-signature Android audio route preferences and runtime re-resolution;
18. selected main output applied to playback with controlled Auto fallback;
19. real Master gain and Master peak/RMS metering;
20. per-track meters + peak hold/decay + durable project Master gain with legacy compatibility;
21. alpha06 software/signing candidate produced through controlled CI;
22. commercial-polish/Mixer V3 checkpoint: pt-BR product UI, icon-first navigation, top-bar transport, persistent mixer pinning, fixed Master + scrolling tracks, live track gain/pan + Master during playback, latched clip warnings, 20-color synchronized track identity, track management and new-track creation.

Next M4 gates:
23. branch-head alpha07 CI: unit tests + Android Lint + debug APK;
24. close any software regression before signing;
25. prepare signed alpha07 consolidation artifact through the controlled signing path;
26. inspect artifact version/identity/hash/signer evidence;
27. physically validate `M4_ALPHA07_HOMOLOGATION_CHECKLIST.md` on Samsung SM-X230;
28. close any P0/P1 regression found in the physical pass;
29. persist evidence, mark PR #1 ready and merge to `main` only after the M4 consolidation gate is green.

M4 invariants:
- managed source media is immutable;
- hardware-presented frames own playhead progression;
- structural edits (timeline/trim/clip/track structure, M/S/R) remain STOPPED-only;
- track gain/pan and Master gain are explicit live-safe exceptions and may change during PLAYING/RECORDING UI state, with engine preview + persistence at gesture completion;
- loop markers are visible only while loop is enabled;
- trim is an explicit draft transaction with Apply/Cancel;
- global input/output setup belongs in Options, not per-track routing;
- raw Android device IDs are not durable route identity;
- Master gain is project metadata and defaults to 0 dB for legacy projects;
- track color metadata is additive and defaults to a stable visual fallback for legacy projects;
- sample-rate mismatch remains unsupported until derived-media resampling passes its own gate;
- real recording remains disabled until M5.

M4 exit gate: supported managed import, persist/reopen, waveform render, reliable playback/seek/loop, non-destructive trim, functional live-safe core mixing, stable output routing, useful metering/clip-latch behavior, track management, coherent commercial UI and no project/source corruption.

## M5 — Recording workflow
Planned: armed tracks become actual capture targets, record project-managed takes, monitoring, timeline placement and disconnect-safe finalization. Recording uses canonical red record semantics and the global Studio input selected in Options. M4 Arm state is metadata preparation only.

## M6 — Latency, synchronization and compensation
Planned: measured I/O/round-trip behavior, alignment and compensation.

## M7 — Mixing and editing maturity
Planned beyond the M4 core mixer: automation, advanced meters, fades/split, optional processing, larger-project performance and workflow refinement.

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
