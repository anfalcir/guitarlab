# M7 — Final Residual Physical Homologation Checklist

Updated: 2026-09-10

> Historical filename retained for compatibility. **Do not homologate alpha1 or the old alpha2 checkpoint.** Use only the final signed RC intentionally produced after the consolidated software + Android integration gates are green.

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
- Install only the final signed RC supplied after the dual automated gate.
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
