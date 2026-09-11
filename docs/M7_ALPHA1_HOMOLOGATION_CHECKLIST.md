# M7 — Final Residual Physical Homologation Checklist

Updated: 2026-09-11

> Historical filename retained for compatibility. Homologate only `GuitarLabStudio-0.4.0-rc1-homologacao.apk` from commit `66108182d9a733930139a84e4f6b9525172bb9aa`. Its automated gates and signature verification passed in CI #590.

Target: Samsung SM-X230 Android 16/API36 + Pocket Amp USB audio + normal GuitarLab guitar workflow.

## Already removed from the user's physical burden
The following have objective automated evidence and must not be manually re-proven:
- project factory/templates/roles/names and save/load/history invariants;
- malformed/legacy `.guitarlab` migration, traversal/duplicate-path protection and rollback;
- managed source/proxy duplication and orphan/safe-derived policies;
- SRC mathematical duration/pitch/RMS/channel invariants and same-rate byte behavior;
- deterministic master placement/gain/pan/mute/solo/fade/crossfade/clipping logic;
- realtime/offline PCM-kernel parity;
- interrupted import staging cleanup;
- lossless interrupted-recording preservation and canonical Float32 WAV-header repair;
- Android encoder timestamp monotonicity;
- FLAC `fLaC`/STREAMINFO structure plus native API 36 extraction/decoding at 48 kHz stereo;
- Activity recreation route restoration;
- Mixer Mute/Solo/Arm semantics/callbacks and non-colliding instrumented control centers;
- structural Small/Medium/Large project/bundle/render regression.

## 1. Candidate identity and launch
- Install `GuitarLabStudio-0.4.0-rc1-homologacao.apk` only. Verify SHA-256 `604f13b83e27021201101fd663dad5c61bca600829ab9565109f48ae23f8e0ad` before installation.
- Confirm the displayed version/build identity matches the supplied candidate.
- Launch, open an existing representative project and create/open one normal Guitar template project.
- No startup crash, missing project, corrupted timeline or unexplained data loss.

## 2. Pocket Amp USB route and real guitar capture
- Connect Pocket Amp through the intended hub/cabling and select/confirm the normal GuitarLab input/output route.
- Confirm playback reaches the intended headphones/output correctly.
- Arm one `My Guitar` track, record a short real guitar take, stop, play it back and reopen the project.
- Confirm the take is present, correctly associated with the armed track, audible on both intended playback channels according to project routing, and remains after restart/reopen.
- Exercise monitoring in the normal playing workflow and judge whether latency/feel is acceptable.
- Disconnect/reconnect the Pocket Amp once and confirm the app fails/revalidates safely rather than silently recording through an unintended route.

## 3. Target-specific MP3 capability
- Export one short representative master as MP3 from the Samsung target.
- If MP3 is offered and the device encoder succeeds, confirm the output is nonempty and playable on the device.
- Any controlled message that the device lacks a compatible MP3 encoder must be recorded as a target capability result, not as silent corruption.
- WAV/FLAC need only a quick smoke/listen here; their structural/container mathematics are already digitally gated.

## 4. Sensory audio smoke
Using a project that contains normal clips plus at least one fade/crossfade and, if convenient, a converted-rate source:
- play through transitions and seek around the timeline;
- listen for repeatable pops, dropouts, wrong pitch/speed, one-sided unintended playback or obvious realtime/offline mismatch;
- export a short master and perform one subjective comparison with realtime playback.
This is a listening sanity check only; do not manually measure SRC ratios or sample-domain envelopes.

## 5. Real-tablet stress and ergonomics
Use a representative multi-track/multi-clip project:
- play, seek and loop;
- open Mixer and operate Mute/Solo/Arm/Pan/volume;
- perform representative drag, trim/split and one fade edit;
- scroll the timeline/mixer as normally used;
- confirm no repeatable ANR/crash, progressive slowdown, unusable touch interaction or layout obstruction on the Samsung tablet.

## 6. Final persistence/export smoke
- Rename a project, restart/reopen and confirm the name persists.
- Save one `.guitarlab` package and reopen it as an independent project.
- Confirm the original project/source media remain intact.
- Perform one final export from the normal user flow.

## PASS criteria
M7/final hardening may close only when:
- the exact final signed RC passed both automated gates before signing;
- this residual checklist has no repeatable P0/P1;
- any device-specific MP3 limitation is explicitly understood/accepted rather than hidden;
- the user explicitly approves the final candidate.

Do not merge PR #1 as part of homologation unless separately requested.

## Final signed RC evidence — 2026-09-11
- Candidate: `GuitarLabStudio-0.4.0-rc1-homologacao.apk` (`versionName 0.4.0-rc1`, `versionCode 19`).
- Exact source commit: `66108182d9a733930139a84e4f6b9525172bb9aa`.
- Canonical GitHub Actions run: [#590](https://github.com/anfalcir/guitarlab/actions/runs/34593159502).
- Result: software gate PASS; Android API 36 full regression PASS; isolated 1920×1200 landscape geometry PASS; signed homologation job PASS.
- APK SHA-256: `604f13b83e27021201101fd663dad5c61bca600829ab9565109f48ae23f8e0ad`.
- Signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Artifact integrity was independently rechecked against `SHA256SUMS.txt`; APK ZIP structure is valid.
- Digital hardening and RC production are complete. Only the residual Samsung SM-X230 + Pocket Amp physical homologation remains before explicit M7/M8 closure.
