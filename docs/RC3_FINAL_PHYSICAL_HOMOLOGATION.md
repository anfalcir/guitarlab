# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-14

This is the active residual physical checklist for RC3. Do not execute it until the canonical manual GitHub Actions run has passed all automated gates and the signed artifact identity has been verified.

## Candidate identity
- versionName: `0.5.0-rc3`
- versionCode: `23`
- target: Samsung SM-X230 + M-VAVE MK-300 over USB
- MK-300 hardware USB loopback: disabled
- expected signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`
- exact source SHA: **fill from the next successful manual workflow `github.sha` for the post-#613 source**
- APK SHA-256: **fill from `SHA256SUMS.txt` after the successful run**
- signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`

Do not homologate an APK whose version, source SHA, checksum or signer differs from the successful run.

## A. Upgrade and project integrity
- Install RC3 over the previous official signed candidate.
- Open an existing project and confirm tracks, clips, takes, markers and sections remain intact.
- Close the app completely, reopen the same project and confirm persistence.

## B. Loop playback, natural completion and live seek
- Create a loop with clearly separated start/end markers.
- Put the playhead before loop start and press Play: playback must start at loop start.
- Put the playhead inside the loop and press Play: playback must start from that in-loop position.
- Put the playhead exactly at loop end and press Play: playback must restart at loop start.
- Put the playhead after loop end and press Play: playback must start at loop start.
- Let looped Play reach `L▶` naturally: it must STOP and leave the playhead at `L◀` rather than continue repeating.
- During looped Play, drag the playhead to several positions inside the interval: audio must continue from each selected position without an explicit Stop/Play cycle.
- While looped Play is active, try dragging toward/beyond either loop boundary: the effective playhead/seek must remain inside `[L◀, L▶)`.
- Disable Loop and start ordinary playback. Drag the playhead both backward and forward while Play remains active; audio must continue from the selected point without a manual restart.
- Let ordinary playback reach project end naturally: it must STOP and return the playhead to 00:00.
- Listen specifically for repeatable seek-related hangs, stale audio, large pops or transport desynchronization.

## C. Section detection preview and clearing
- Run `Auto seções` on a representative backing/project.
- Confirm detected regions become visible immediately as **Prévia** before persistence and no region appears beyond the actual song end.
- Confirm the original `Auto seções` slot is replaced in place by `Aplicar` + a red `X`, without pushing `Limpar seções` or changing the neighboring Timeline geometry.
- Press the red `X`: persisted sections must remain unchanged.
- Detect again and press `Aplicar`: the resulting persisted section boundaries must visually match the preview.
- Use a persisted section to define the loop and confirm alignment.
- Press `Limpar seções`, confirm the warning and verify that persisted sections and any preview disappear while clips, markers and loop bounds remain intact.
- Verify loop markers remain visually dominant but section labels stay usable/readable without an extra permanent rail.

## D. Recording countdown and Loop-enabled choice
- With Loop disabled, press REC and confirm the ordinary current-playhead recording path starts with no punch-choice modal.
- With Loop enabled, press REC and confirm the modal offers `Somente o loop`, `Desde o início` and `Cancelar`.
- Start REC and confirm a large translucent centered `3 → 2 → 1` overlay appears without pushing the Comparação/Timeline row or tracks.
- With Loop active, choose `Cancelar`: no recording/countdown/transport mutation should remain.
- Choose `Desde o início`: recording must start from 00:00 and the loop must no longer constrain that recording.
- Choose `Somente o loop`: capture may begin before loop start for pre-roll, but the retained take must align to the loop region according to punch semantics.
- During countdown/REC/finalization, attempt to drag the playhead: its timeline position must not be seekable by the user.
- Stop/repeat recording and reopen the project; an old persisted punch field must never silently arm a later recording.

## E. MK-300 capture isolation and live recording
- Explicitly select MK-300 as input and intended output.
- Play a backing track, arm exactly one My Guitar track and record.
- Confirm live waveform grows during capture.
- Confirm the recorded take contains guitar input only, not backing playback.
- Repeat a short take with monitoring Off, Auto and On; monitoring choice must not change recorded-file contents.
- Disconnect the selected input during a short take: capture must stop safely and must not fall back to the tablet microphone.

## F. Transport, takes and comparison smoke
- During ordinary playback press `|<`; transport must remain responsive and obey the active loop rule when Loop is enabled.
- Start/stop playback repeatedly, perform several live seeks, record, then return to playback.
- Confirm a manual Stop keeps the current playhead position; only **natural completion** performs the automatic return-to-start behavior.
- Record at least two takes on one track and switch the active take.
- Exercise Reference, My Guitar and Both comparison modes and confirm the intended tracks are audible/hidden consistently with the visual state.

## G. Real-device listening, export and stress
- Judge monitoring latency/feel and listen for repeatable pops, dropouts, wrong pitch/speed, unintended one-sided audio or loop/seek-boundary artifacts.
- Exercise a representative multi-track project with seeking, looping, Mixer and edits.
- Export short WAV and FLAC masters and confirm playability.
- If MP3 is available on the target Samsung, export and play one MP3; if unavailable, a controlled capability failure is acceptable and must not leave a corrupt file.

## PASS criteria
- successful canonical automated RC3 workflow for the exact source SHA;
- official signer and checksum verified;
- no repeatable P0/P1;
- no GuitarLab-attributable backing leakage or unintended input fallback;
- natural completion/reset, live playhead seek, loop playback, section preview/clear and transient punch choice behave as specified;
- explicit user approval of this exact APK.

Deterministic model/editor/file/codec math, API 36 generic integration and target-geometry checks are automated and should not be manually re-proved here.
