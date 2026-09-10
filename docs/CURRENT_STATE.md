# Current State — GuitarLab Studio

Updated: 2026-09-10

## Active branch and gate
- Repository: `anfalcir/guitarlab`
- Branch: `dev/parallel-m3-m5` (historical branch name retained while PR #1 stays active)
- Draft PR: #1
- M2: PASS/CLOSED
- M3/M4: absorbed
- M5: PASS/CLOSED
- M6: **PASS/CLOSED by explicit user physical approval of 0.3.0-alpha1**
- M7: implementation candidate `0.4.0-alpha2`, versionCode 18; **OPEN pending physical homologation**
- M8.A: **IN PROGRESS**. Compatibility/migration fixtures, malformed packages, editing-domain validation, duplicate-project media integrity, deterministic stress/fuzz, shared PCM render and cancellation-safe managed-media publication now have objective coverage.
- Signed checkpoint: `0.4.0-alpha2` (versionCode 18), commit `ca5b9d57ed07bb9cbd27a5da61386cc764209fd0`, CI run #465. Latest completed software gate before the current lifecycle batch: CI run #493 on `3bb3c8f37a826230d4cf619299054b79779176fd` (tests, lint and debug assembly PASS). Do not physically homologate alpha2; wait for the consolidated signed RC candidate.

## M7 production audio polish
M7 closes the previously documented production-audio-polish block: mismatched sample-rate conversion, non-destructive fades/crossfades, render parity and larger-session memory discipline. It also incorporates the approved workflow polish of Rename + shared `Salvar e exportar` directly from each project row on Home.

### Sample-rate contract
The imported native source remains immutable. When the source rate differs from the project/editing rate, GuitarLab creates a project-managed 32-bit-float WAV proxy using bounded-memory windowed-sinc conversion. Timeline/sourceStart/length editing then operates in the editing-proxy frame domain, while native-source rate/format metadata remains provenance.

### Fade/crossfade contract
Fade in/out are clip metadata and do not rewrite source audio. Realtime playback and offline master rendering use the same deterministic envelope. Crossfade requires actual overlap between two clips on the same track and maps the overlap to left fade-out + right fade-in.

### Performance contract
The resampler is chunk-bounded and playback/master ClipReaders reuse scratch arrays rather than allocating a new decode buffer on every render chunk. M7 physical stress remains required before claiming large-session verification.

### Home project actions
The three-dot project menu now offers Rename, Salvar e exportar, Duplicate and Delete. Home invokes the same `RenameProjectDialog` and `SaveAndExportDialog` components as Studio. Project persistence and master export use the same managed project/media contracts.

### Digital hardening after alpha2
Realtime playback and offline render now share the same PCM reader/mix kernel, with bit-exact output across realtime/offline chunk sizes. Home and Studio also share one master-request factory and one export service. Project packages are fully staged before the selected destination is opened. Import, stereo split and recorded-take publication use a non-cancellable persistence boundary so rollback cannot delete media already referenced by a committed project; pre-commit failure removes temporary media and waveform cache.

Canonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.
Active checklist: `docs/M7_ALPHA1_HOMOLOGATION_CHECKLIST.md`.
Global matrix: `docs/M8_GLOBAL_DIGITAL_REGRESSION.md`.
