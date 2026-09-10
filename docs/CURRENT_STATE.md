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
- M8.A: **IN PROGRESS**. Global digital regression started; compatibility, malformed packages, M7 editing-domain validation and duplicate-project media integrity have objective coverage.
- Digital regression checkpoint: CI run #452 passed 127 tests, Android Lint and debug APK assembly on commit `e686ccf62bdf79008e962a1a401ba69b46b3d5d9`. A final signed alpha2 identity supersedes this checkpoint only after its own green gate.

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

Canonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.
Active checklist: `docs/M7_ALPHA1_HOMOLOGATION_CHECKLIST.md`.
Global matrix: `docs/M8_GLOBAL_DIGITAL_REGRESSION.md`.
