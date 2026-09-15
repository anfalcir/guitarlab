# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This is the only active residual physical checklist. Execute it on the next signed H11 candidate **after** its exact-source manual workflow passes. CI #617 remains the digitally homologated H0–H10 baseline but predates H11.

## Candidate identity
- versionName: `0.5.0-rc3`
- versionCode: `23`
- package: `studio.guitarlab.app`
- target: Samsung SM-X230 + M-VAVE MK-300 over USB
- MK-300 hardware USB loopback: disabled
- expected signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`
- exact H11 source SHA: **fill from the next successful manual workflow**
- signed APK SHA-256: **fill from that run's `SHA256SUMS.txt`**
- signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`

Historical H10 baseline: CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48` / signed SHA-256 `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.

## A. Upgrade and project integrity
- Install the newly signed H11 RC3 over the previous official candidate.
- Open an existing project and confirm tracks, clips, takes, markers and sections remain intact.
- Close/reopen the app and project and confirm persistence.

## B. H11 Mixer segmented bar and persistence
- Tap the top-bar `Mixer` button once: Mixer must open.
- Confirm the dock header has no redundant `Mixer` title, Pin or X.
- Confirm the header is visually one bar split into two equal horizontal blocks: **Comparação** and **Timeline**.
- Confirm buttons have restrained, slightly rounded near-square geometry rather than pill styling.
- Exercise controls in both blocks and check for overlap or clipping.
- Tap the same top-bar `Mixer` button again: Mixer must close.
- Open it again, leave it visible, change project and return; then restart the app/project.

PASS: one top-bar toggle is the sole open/close control, the two segments are visually balanced, and visibility persists until the user explicitly changes it.

## C. H11 waveform track selection
- With at least two tracks visible, select track A through sidebar/Mixer.
- Tap directly on track B's empty waveform/audio lane.
- Tap one of track A's clip/waveform cards.
- During a disposable REC, tap the active live waveform surface if ergonomically practical.

PASS: each waveform/audio-area tap selects the corresponding track exactly as sidebar/Mixer selection does, without unintended edit or transport action.

## D. H11 real-time REC Peak/RMS
- Select MK-300 input and arm one `My Guitar` track.
- Start REC and play from silence through soft and strong attacks.
- Observe that the recording target shows live **PK** and **RMS** in the timeline overlay and in the track's Mixer meters.
- Confirm unrelated tracks do not mirror the raw input meter.
- Stop, start another take and verify stale values do not carry over before new signal arrives.

PASS: Peak responds quickly to attacks, RMS follows average energy, both are live only for the recording target, and meters reset cleanly between sessions.

## E. First-tap editing and trim ergonomics
- Open a clip pencil/actions menu and tap `Cortar` once several times across clips.
- Confirm first valid tap always opens Trim or explains a blocked state.
- Move both handles, Apply, Undo, Redo and save/reopen.

PASS: no missed first tap/silent no-op; retained region survives history/persistence.

## F. Level analysis convergence + history stress
- Analyze one guitar track, apply recommendation, analyze again.
- Confirm the identical correction is not repeatedly proposed solely because source PCM is unchanged.
- Perform multiple Undo/Redo operations and switch projects/back.

PASS: effective level converges; history and transport controls remain responsive and project-scoped.

## G. Stop during recording
- During countdown press Stop: countdown cancels safely.
- Start REC again, let capture begin, press Stop.
- Repeat using REC itself to stop.

PASS: Stop and REC finalize through equivalent successful behavior with no duplicate take/corrupt WAV/stuck state.

## H. Live REC waveform stability
Record continuously for at least 2–3 minutes.
- confirm historical waveform does not become sparse while recent material becomes dense;
- right edge follows recorded duration monotonically;
- no backward piling, acceleration, freeze/catch-up burst or timing migration;
- finalized waveform remains aligned.

## I. MK-300 routing and synchronization
- explicitly select MK-300 input/output with hardware loopback disabled;
- record rhythm against backing;
- confirm backing is not printed into guitar take;
- confirm no repeatable systematic late placement;
- disconnect selected input during a disposable take and verify fail-closed behavior;
- listen for repeatable pops/dropouts/wrong speed/one-sided output.

## J. Focused smoke
- Auto seções preview/application;
- one Loop pass and live playhead seek;
- at least two takes and active-take switch;
- one short WAV or FLAC export and playback.

## PASS criteria
- next exact-source H11 workflow fully green;
- signer/package/version/source/checksum verified;
- no repeatable P0/P1 in sections B–J;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of that exact signed APK.

Do not manually duplicate deterministic model/file/math/API36 checks already covered by the automated gate.
