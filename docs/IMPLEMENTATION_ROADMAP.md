# Implementation Roadmap

This roadmap defines delivery order and gates. A milestone can be developed in parallel only when its dependency risk is isolated and any open hardware gate remains explicitly open.

## M1 — Project/model foundation
Status: substantially implemented.
- project model, templates, roles, groups/tracks;
- file repository and validation;
- project creation/open/duplicate/delete foundations.
Gate: model/repository tests and persistence integrity.

## M2 — Android audio path and hardware diagnostics
Status: software implemented; Pocket Amp physical gate still OPEN.
- enumerate routes/devices;
- playback, record and duplex probes;
- permission/privacy handling;
- USB/hotplug resilience;
- startup prebuffer/underrun-delta fix.
Exit gate: signed build, physical Pocket Amp checklist, repeated no-crash/no-wrong-route runs.

## M3 — Codec/import foundation
Status: WAV core + Android SAF path implemented; tested real PCM24/44.1k stereo on tablet. Broader format support remains planned.
- codec contracts;
- WAV metadata/PCM decode/seek;
- sample-rate strategy planning;
- Android content URI direct seek/cache fallback;
- codec diagnostics.
Exit gate: codec unit tests plus representative real-device files.

## M4 — Studio timeline and import
Status: IN PROGRESS.
Sequence:
1. persisted project workspace and track lanes — done;
2. design system + launcher identity — done;
3. persistent non-destructive AudioClip model — done;
4. WAV import vertical slice into selected track — done;
5. persist source technical metadata needed by timeline/transport — done;
6. clip management basics (remove/mute/move + non-destructive trim model) — done;
7. immutable managed-media ingest + waveform cache/rendering — current checkpoint implemented, CI/device validation pending according to CURRENT_STATE;
8. transport/playhead/seeking over clips — next checkpoint;
9. tablet validation for import, reopen, waveform and transport.

### M4 managed-media invariant
A successful import must copy the external file into `projects/<project>/media/source/`. Both the user's external original and the managed internal source are immutable. Timeline edits alter metadata only. Resampling, waveform, proxy and render outputs are separate derived assets.

M4 exit gate: import a supported file, persist/reopen independently of the external original, render waveform, seek/play correctly, edit clip non-destructively, and show no project/source corruption.

## M5 — Recording workflow
Planned.
- arm tracks and record takes into project-managed audio assets;
- guitar-oriented mono capture and double-track workflows;
- explicit monitoring;
- recorded clips placed correctly on timeline;
- crash/disconnect-safe recording finalization.
Exit gate: repeated real-device takes with file integrity and stable routing.

## M6 — Latency, synchronization and compensation
Planned.
- measured input/output/round-trip behavior;
- latency compensation model;
- recording alignment tests;
- sample-accurate timeline expectations documented.
Exit gate: measurable repeatable alignment within defined tolerance.

## M7 — Mixing and editing maturity
Planned.
- track gain/pan/mute/solo and metering;
- clip gain/mute, move/trim/split, fades when validated;
- efficient rendering and larger-project performance.

## M8 — Multi-format interoperability and export
Planned as a product requirement, not optional polish.
- complete approved import matrix beyond WAV;
- resampling implementation/quality validation;
- export/render pipeline for WAV/FLAC and approved compressed formats;
- mix/stems/range outputs where implemented.
Exit gate: format-by-format software + Android validation matrix.

## M9 — Release hardening
Planned.
- migration compatibility;
- performance/memory/battery tests;
- accessibility/readability review;
- destructive-edge-case recovery;
- signed release/homologation regression suite.

## Gate rules
- CI green means software gate only, never automatic hardware homologation.
- A feature is user-visible as supported only after its applicable gate passes.
- `main` remains the stable signed baseline until the parallel development branch is ready and all required open gates are closed.
- Routine development commits must not trigger signed builds unnecessarily.
