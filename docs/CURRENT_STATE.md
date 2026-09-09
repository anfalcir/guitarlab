# Current State — GuitarLab Studio

Updated: 2026-09-09

## Active branch and gate
- Repository: `anfalcir/guitarlab`
- Branch: `dev/parallel-m3-m5`
- Draft PR: #1
- Candidate: `0.2.0-alpha14`, versionCode 15
- M2: PASS/CLOSED on Samsung SM-X230 Android 16/API 36 + Pocket Amp USB
- M3/M4: absorbed into the active integration branch
- M5: implementation consolidated; **OPEN pending alpha14 physical homologation**
- M6: BLOCKED until explicit M5 closure

## What alpha14 consolidates
Alpha13 replaces alpha12 as the final M5 physical candidate. It contains the previously corrected M5 interaction/UX work plus the media I/O/persistence/export scope intentionally pulled forward before M6:

1. workspace-level track and clip drag coordinator, continuous overlay and edge autoscroll;
2. responsive `Configurar pista`, with source metadata, safe clear/delete semantics and edit pencil for tracks with content;
3. M5 capture/recording/countdown/managed-take/duplex behavior and established Mixer/Master/Trim/Loop/Undo/Redo contracts;
4. audio import detection for WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG/Vorbis and Opus;
5. immutable original managed source plus optional derived PCM WAV edit proxy;
6. portable `.guitarlab` package save and Home-screen restore as an independent project, with manifest/version checks, referenced-media checks, ZIP traversal defense and bounded extraction;
7. offline master render in floating point;
8. WAV 32-bit float output, FLAC output through Android encoder path, and MP3 320 kbps output through the available Android encoder path;
9. a single `Compartilhar` entry in the Studio top bar, immediately before Home, opening the `Salvar e exportar` modal. Output actions no longer live in Options.

## Media capability status
Code existence is not the same as final support claim. `docs/CODEC_SUPPORT_MATRIX.md` is authoritative.

- WAV import/decoder core already has prior partial Android evidence; alpha14 adds the Float32 master writer to the physical gate.
- FLAC/MP3/M4A/OGG/Opus compressed imports use Android media decoding to a managed PCM proxy and still require representative alpha14 target-device validation.
- AIFF/AIFC PCM has a dedicated import path; compressed AIFF-C is not claimed.
- FLAC export and MP3 export are physically gated. In particular, MP3 encoding is device-capability dependent in the current Android implementation and is not considered product-verified until it succeeds on the target Samsung.
- Source/project sample-rate mismatch remains explicit and blocked where transparent resampling has not been validated. M6 does not inherit an implicit resampler claim.

## Persistence contract
The authoritative edit state remains `project.json` inside managed project storage. Autosave continues normally. The portable `.guitarlab` file is a versioned ZIP package containing manifest, project metadata and referenced managed source/proxy media. Opening a package creates a new project identity rather than silently overwriting an existing project.

## Current software evidence
The media I/O + portable-project foundation immediately preceding alpha14 promotion passed unit tests, Android Lint and debug APK assembly. The alpha14 candidate itself must repeat that software gate and then pass the controlled signed-release identity gate.

## Physical closure rule
M5 closes only when `docs/M5_ALPHA13_FINAL_HOMOLOGATION_CHECKLIST.md` is completed on Samsung SM-X230 with no repeatable P0/P1 and the user explicitly approves closure. Until then PR #1 stays draft and M6 stays blocked.


## Alpha14 corrective scope
Physical alpha13 validation exposed two M5 media-ingress gaps: no visible progress after SAF selection and aggregate-only stereo waveform rendering. Alpha14 closes both and pulls the already-modeled guitar L/R semantics into import: every stereo clip gets a keep-vs-separate decision, guitar-role pairs can receive synchronized L/R mono derivatives, and the pencil menu exposes stereo separation separately from temporal split. M6 remains blocked.
