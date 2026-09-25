# M6 alpha1 — Physical Homologation Checklist

Target: Samsung SM-X230 Android 16/API36 + Pocket Amp USB route.
Candidate: `0.3.0-alpha1` / versionCode 16.

## A. Timeline/time UX
- Playhead marker displays exact `mm:ss` (or `h:mm:ss`) and updates while dragging/playing.
- Loop start/end markers display their own exact times.
- Top bar shows `Restante` and `Total` consistently with the same project frame clock.

## B. Live mixer
- Toggle Mute while PLAY: audible state changes without stopping/restarting transport.
- Toggle Solo while PLAY: solo arbitration updates immediately.
- Toggle Mute/Solo while REC with backing: backing mix changes while capture continues.
- REC Arm remains protected from structural changes during active capture.

## C. Project rename
- Rename from Studio title control.
- Name persists after Home/reopen/app restart.
- Home listing and suggested `.guitarlab`/master filenames use the new name.

## D. M6 latency calibration
- Select intended input/output route.
- Connect/enable a real output-to-input loopback/return path.
- Run `Opções > Áudio > Calibrar latência`.
- Calibration runs multiple passes and reports latency, jitter and confidence.
- Repeat at least twice; accepted results must be reasonably stable.
- Break/remove loopback and verify calibration fails/rejects instead of inventing a value.
- Change route and verify an unrelated stored calibration is not shown/applied.

## E. Take-placement compensation
- With an accepted calibration, record a sharp known transient against a backing/reference transient.
- Inspect waveforms: recorded transient should align materially closer after automatic compensation.
- Repeat take to assess jitter/repeatability.
- Start recording near timeline zero and confirm no negative clip position/corruption.
- Clear/use an uncalibrated route and confirm recording still works with zero automatic compensation.

## F. Regression
- Import stereo/multi-format representative files.
- Drag track/clip, Trim, split temporal, stereo L/R separation.
- Loop/Undo/Redo.
- Save/open `.guitarlab`.
- Export WAV32f/FLAC/MP3.

M6 closes only with zero repeatable P0/P1 and explicit user approval.
