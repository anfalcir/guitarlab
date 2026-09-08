# Current State

Last updated: 2026-09-08

## Stable baseline
- `main`: signed/homologation baseline `0.2.0-alpha03`, versionCode 4.
- Stable main commit includes signed M2 software candidate; do not merge parallel work while required hardware gate remains open.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- current app version on branch: `0.2.0-alpha04`, versionCode 5.

## Gates
- M2 software diagnostics: implemented and CI-green.
- M2 Pocket Amp physical homologation: OPEN.
- M3 WAV codec core/Android path: software-green.
- M3 real tablet evidence: PASS for tested WAV PCM24, 44.1 kHz, stereo, direct seek and exact midpoint seek.
- M4: IN PROGRESS.

## M4 completed checkpoints
- persisted Studio project workspace and track lanes;
- clean adaptive design system and app launcher identity;
- non-destructive `AudioClip` timeline model and validation;
- timeline preview foundation;
- WAV import vertical slice into selected track;
- imported clip persistence through `FileProjectRepository`;
- technical source metadata persisted on clips;
- clip-management core with validated remove, mute/unmute and frame movement operations;
- explicit non-destructive trim model bounded by immutable source length;
- Studio controls for persisted mute/unmute and removal;
- project-managed source ingest: external files are copied into `media/source/` and no longer become the runtime dependency after successful import;
- managed source path + origin provenance metadata persisted separately;
- deterministic waveform-envelope builder;
- derived waveform cache under `media/derived/waveform/`;
- cached waveform rendering in Studio clip management;
- cache rebuild from managed source when a cache is missing/corrupt.

## Latest verified CI before this checkpoint
Run #67 completed successfully for the prior clip-management checkpoint: unit tests, Android Lint, debug APK assembly and artifact upload passed. Signed homologation was intentionally skipped for routine development.

## Current implementation checkpoint
The managed-media + waveform checkpoint is implemented and awaiting its own current-head CI result. New imports now use the external Android document only as read-only ingestion input. The project copies it into app-controlled project storage, validates/decodes the internal copy, and persists a clip that points to the managed source. The original external URI is provenance only.

Both layers are protected:
- external original: never written by GuitarLab;
- internal managed source: immutable after successful ingest.

Trim/move/gain/mute stay metadata-only. Waveforms are disposable derived cache data and may be regenerated from the managed source. See `MANAGED_MEDIA_POLICY.md`.

## Next checkpoints
1. current-head CI validation for managed-media + waveform implementation;
2. transport state/playhead foundation;
3. seek/playback over managed clips;
4. bind validated clip movement/trim to timeline scale and gestures;
5. tablet validation: import, rename/move/delete the original external file, reopen project, confirm waveform/source still work;
6. broader codec/import support only as each format passes its own software/Android gates.

## Important limitations that are intentional, not final scope
- user-facing Studio import currently begins with WAV only;
- legacy development projects may still contain direct external `sourceUri` references until migration is implemented;
- compressed formats are still planned/unverified and must not be advertised yet;
- production resampling is not yet enabled;
- transport/playback of Studio clips is the next M4 block;
- recording into Studio is a later milestone;
- M2 Pocket Amp gate remains physically open.

## Branch policy
`main` remains stable. Parallel work stays in the draft PR. Do not trigger signed homologation for routine development commits. Do not close physical gates based only on CI.
