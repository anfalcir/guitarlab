# Test and Homologation Plan

Updated: 2026-09-09

## Canonical software gate
Every candidate commit on the active branch must pass:
1. `git diff --check` on the candidate delta;
2. source materialization;
3. unit tests;
4. Android Lint;
5. debug APK assembly;
6. artifact/diagnostic upload.

A failure blocks candidate promotion until corrected. Software-green is necessary but does not replace real-device validation.

## Signed homologation gate
Only an explicit `[sign-homologation]` candidate (or authorized manual dispatch) may build the homologation release. The job must:
- restore signing material only inside the runner;
- build release;
- verify APK signature with `apksigner`;
- compare signer SHA-256 against the locked certificate;
- generate `SHA256SUMS.txt` and `BUILD_IDENTITY.txt`;
- upload the homologation artifact;
- destroy restored signing material even after failure.

Expected certificate SHA-256 is maintained by the workflow and must match the established GuitarLab homologation identity.

## Active M5 gate
The only active physical checklist is `M5_ALPHA13_FINAL_HOMOLOGATION_CHECKLIST.md`. Alpha08–alpha12 documents are historical evidence.

Alpha13 physical validation covers four groups:

### A. Regression surface
- track reorder and clip migration with continuous ghost;
- edge autoscroll and no drag cancellation;
- Track Settings responsiveness and source metadata;
- edit pencil for populated track;
- Trim, split/duplicate/remove, clear vs delete;
- Undo/Redo, Loop/playhead, REC/Arm, Pan, Mute/Solo, Mixer/Master;
- recording/countdown/managed take/backing behaviors already in M5.

### B. Media import
Representative target-device files for WAV, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG/Vorbis and Opus. For each accepted file verify import completion, waveform, playback, metadata, Trim and project reopen. Unsupported/corrupt input must fail cleanly without creating a broken clip.

### C. Portable project persistence
- use Share > `Projeto GuitarLab` to create `.guitarlab`;
- return Home and `Abrir projeto`;
- restored package becomes an independent project;
- verify tracks/order/names/colors/roles/mix, clips/positions/trims, managed media and playback;
- malformed/incompatible packages must fail without overwriting an existing project.

### D. Master export
- WAV 32-bit float;
- FLAC lossless path;
- MP3 320 kbps path;
- exported files must be non-empty, playable and audibly reflect current timeline/mix state with expected duration/channels/sample rate;
- MP3 encoder availability is explicitly target-device gated.

## Closure severity
- P0: data loss/corruption, crash, invalid package overwrite, broken recording/project, unusable output — M5 BLOCKED.
- P1: repeatable major workflow failure such as drag cancellation, requested format unable to import/export, wrong master content or persistence failure — M5 BLOCKED.
- P2/P3: minor cosmetic/usability issue may be recorded for later only if it does not compromise the approved workflow.

## M5/M6 boundary
M5 remains OPEN until alpha13 is software-green, signed identity is verified, physical checklist passes with zero repeatable P0/P1, and the user explicitly approves closure. M6 must not begin before that event.
