# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This is the only active residual physical checklist. Execute it on the next signed H12–H15/H14a candidate **only after** its exact-source manual workflow passes. CI #620 remains the digitally homologated H0–H11 baseline and predates Physical Review IV. CI #621 was diagnostic; CI #622 failed in source materialization before build/test and is not a physical-homologation candidate.

## Candidate identity
- versionName: `0.5.0-rc3`
- versionCode: `23`
- package: `studio.guitarlab.app`
- target: Samsung SM-X230 + M-VAVE MK-300 over USB
- MK-300 hardware USB loopback: disabled
- expected signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`
- exact repaired H12–H15/H14a `main` source SHA: **fill from the next successful manual workflow**
- signed APK SHA-256: **fill from that run's `SHA256SUMS.txt`**
- signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`

Historical H11 baseline: CI #620 / source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` / signed SHA-256 `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`.

Before installing, verify the new workflow is fully green and its source SHA equals the final `main` SHA. Do not use #621 or #622 artifacts as the final candidate.

## A. Upgrade and project integrity
- Install the newly signed H12–H15/H14a RC3 over the previous official candidate.
- Open an existing project and confirm tracks, clips, takes, markers and sections remain intact.
- Close/reopen the app and project and confirm persistence.

## B. H12 all-track level analysis
- Open Mixer/practice controls and tap `Níveis`.
- Confirm the dedicated modal lists every track with current gain and audible-material status.
- Tap `Analisar todas` and verify useful PK/RMS/recommended-gain results appear per eligible track.
- Reanalyze one track independently.
- Apply one individual suggestion and verify only that track changes.
- Reanalyze as needed, then use `Aplicar sugestões` with two or more actionable tracks.
- Press Undo once.

PASS: the modal is practical/readable; busy state is clear; silent/no-audio tracks are handled safely; global application changes all actionable tracks as one project edit and one Undo restores that batch.

## C. H13 Trim markers on timeline ruler
- Enter `Cortar` on a clip.
- Confirm waveform handles remain clear and no time bubble obscures them.
- Confirm T1 and T2 appear on the fixed timeline time ruler above the tracks as yellow ticks with precise floating times.
- Drag T1/T2 across different parts of the project, including across/near the playhead and near one another.

PASS: marker ticks remain proportional to timeline position; time labels remain readable, stay visually above competing playhead drawing, separate when close and do not obscure the waveform handles.

## D. H14/H14a Mixer overflow with MASTER anchored
- Use a project with enough tracks that all Mixer strips cannot fit at once.
- Swipe horizontally across the track-strip region in both directions.
- Reach the first and last tracks and exercise one hidden strip's controls.
- Observe MASTER throughout the swipe.

PASS: track strips scroll naturally without fighting their controls; hidden tracks become accessible regardless of viewport width; MASTER remains fixed at the right edge without jitter or horizontal displacement.

## E. H15 Studio return without flash
- From Studio, open Config/Options and return to the same Studio repeatedly.
- Repeat Studio → Home → same project Studio.
- Before one round trip, make a reversible project edit so Undo is available.

PASS: the same project returns immediately without the prior visible double-load/flicker; resident state remains coherent; Undo history is not unexpectedly discarded.

## F. H11 retained Mixer/selection/REC metering
- Top-bar Mixer button toggles persistent visibility.
- Comparação/Timeline remains visually segmented and usable.
- Tap waveform/clip/live waveform to select tracks.
- During MK-300 REC, verify PK/RMS responds only on the recording target and resets cleanly next session.

## G. First-tap editing and trim ergonomics
- Open a clip pencil/actions menu and tap `Cortar` once several times across clips.
- Confirm first valid tap always opens Trim or explains a blocked state.
- Move both handles, Apply, Undo, Redo and save/reopen.

PASS: no missed first tap/silent no-op; retained region survives history/persistence.

## H. Level analysis convergence + history stress
- Analyze one guitar track, apply recommendation, analyze again.
- Confirm the identical correction is not repeatedly proposed solely because source PCM is unchanged.
- Perform multiple Undo/Redo operations and switch projects/back.

PASS: effective level converges; history and transport controls remain responsive and project-scoped.

## I. Stop during recording
- During countdown press Stop: countdown cancels safely.
- Start REC again, let capture begin, press Stop.
- Repeat using REC itself to stop.

PASS: Stop and REC finalize through equivalent successful behavior with no duplicate take/corrupt WAV/stuck state.

## J. Live REC waveform stability
Record continuously for at least 2–3 minutes.
- historical waveform does not become sparse while recent material becomes dense;
- right edge follows recorded duration monotonically;
- no backward piling, acceleration, freeze/catch-up burst or timing migration;
- finalized waveform remains aligned.

## K. MK-300 routing and synchronization
- explicitly select MK-300 input/output with hardware loopback disabled;
- record rhythm against backing;
- confirm backing is not printed into guitar take;
- confirm no repeatable systematic late placement;
- disconnect selected input during a disposable take and verify fail-closed behavior;
- listen for repeatable pops/dropouts/wrong speed/one-sided output.

## L. Focused smoke
- Auto seções preview/application;
- one Loop pass and live playhead seek;
- at least two takes and active-take switch;
- one short WAV or FLAC export and playback.

## PASS criteria
- next exact-source H12–H15/H14a workflow fully green;
- signer/package/version/source/checksum verified;
- no repeatable P0/P1 in sections B–L;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of that exact signed APK.

Do not manually duplicate deterministic model/file/math/API36 checks already covered by the automated gate.
