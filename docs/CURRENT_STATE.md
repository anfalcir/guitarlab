# Current state

Updated: 2026-09-09

## Repository truth

- stable baseline: `main`, signed `0.2.0-alpha03`;
- active integration: `dev/parallel-m3-m5`, draft PR #1;
- active app line: `0.2.0-alpha10`, versionCode 11;
- alpha09 consolidates the Studio UX gaps confirmed in the alpha08 tablet screenshot: overlay Snackbar feedback, clean full-height waveforms, contextual sidebar actions, source metadata, drag reorder/move and full-lane playhead/loop guides;
- CI is the canonical executor: source materialization, Unit Tests, Android Lint, Debug APK and artifacts;
- signing secrets exist only in CI and the certificate fingerprint is locked.

## Milestones

- M2: physically homologated on Samsung SM-X230 / Android 16/API 36 with Pocket Amp USB. PASS/CLOSED.
- M3: WAV codec/import foundation implemented; broader formats remain governed by `CODEC_SUPPORT_MATRIX.md`.
- M4: managed media, waveform cache, playback, timeline, Mixer, Master, meters, Options/routing and commercial polish implemented. Remaining visual validation is bundled into the next physical candidate.
- M5.A: recording transaction, streaming float-WAV writer, countdown policy and state machine software-green.
- M5.B: Android capture engine, input preference enforcement, input metering and monitoring implemented.
- M5.C: Studio coordinator integrated for permission, countdown, revalidation, capture, finalization, automatic clip/waveform, safe partial takes and backing playback. Awaiting final CI and signed physical gate.

## M5 invariants

- one global input targets exactly one armed track;
- REC is rejected with zero or multiple armed tracks;
- capture never begins during the five-second countdown;
- project, permission, arm and selected route are revalidated at zero;
- zero-frame/invalid capture creates no clip;
- route loss after valid frames may preserve a clearly identified partial take;
- media is promoted atomically to immutable `media/source/` before metadata references it;
- recorded source is WAV, 32-bit float, mono/stereo at the negotiated/required rate;
- backing may run alongside capture; fine round-trip latency compensation remains M6.

## Studio contract

- canonical transport: return, play/stop, REC, loop, undo, redo;
- structural edits are STOPPED-only; live gain/pan/Master remain available during transport;
- track names are 1–24 characters, fully shown in up to two lines, without marquee/ellipsis;
- upper marker rail contains only Playhead and Loop;
- Trim exists only inside the active waveform, opens at 35%/65%, shows both precise times and colors only the selected interval;
- Trim, Split, Duplicate, Delete and Undo/Redo preserve managed source bytes;
- Options owns global input, main output, monitoring, export placement and diagnostics;
- Mixer is a bottom dock with horizontally scrolling tracks and fixed Master.

## Next gate

1. complete CI for the final M5 integration HEAD;
2. produce an explicit signed homologation APK from that exact green HEAD;
3. execute `M5_ALPHA08_HOMOLOGATION_CHECKLIST.md` on Samsung SM-X230 + Pocket Amp;
4. close M5 only with zero P0 and zero repeatable P1;
5. retain diagnostics, APK identity, signer fingerprint and SHA-256 as evidence.
