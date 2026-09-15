# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-14

This is the only active residual physical checklist for the digitally homologated RC3 H0–H6 candidate.

## Candidate identity — LOCKED
- versionName: `0.5.0-rc3`
- versionCode: `23`
- package: `studio.guitarlab.app`
- exact application source SHA: `3051619c219e346daca00d2242f60ef03f2d80db`
- canonical workflow: CI #616 / run ID `34912716297`
- signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`
- signed APK SHA-256: `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`
- unsigned release SHA-256: `a5fd846bc2fb54a2995ab7fd48f3fcef0055991e6b9678b05f641e739981d0e2`
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`
- target: Samsung SM-X230 + M-VAVE MK-300 over USB
- MK-300 hardware USB loopback: disabled

CI #616 passed the complete software, API 36, isolated tablet-geometry and signing/provenance matrix for this exact application source. `BUILD_IDENTITY.txt` records `software+android-integration-passed;physical-validation-pending`.

Do not substitute another APK, source SHA, package, version, checksum or signer during this physical pass.

## A. Upgrade and project integrity
- Install this signed RC3 over the previous official candidate.
- Open one existing project and confirm tracks, clips, takes, markers and sections remain intact.
- Close the app completely, reopen the same project and confirm persistence.

## B. Trim-handle ergonomics
Use a recorded guitar clip long enough to make both trim boundaries obvious.
- Open `Cortar`.
- Grab and move the **left/start** handle several times.
- Grab and move the **right/end** handle several times.
- Put the handles relatively close and confirm each remains independently selectable without unexpected ownership swap.
- Apply the trim, Undo, Redo, save/reopen and confirm the same retained audio region.

PASS: both handles are reliably clickable/draggable on the tablet; bubbles remain readable and do not steal touch; no accidental clip drag/playhead seek occurs while manipulating a trim handle.

## C. Split, move, delete and drag-to-trash
Use a newly recorded guitar take.
- Split the clip near its end using `Dividir no cursor`.
- Drag only the right child to an empty compatible track.
- Save/reopen and confirm both children still exist in their intended tracks and playback is valid.
- Undo/Redo the movement.
- Use `Excluir clipe` on one disposable segment; confirm the warning and verify only that segment is removed.
- Repeat with another disposable segment by dragging it to the trash target; confirm the same deletion warning.
- Cancel one deletion and confirm project state remains unchanged.
- Save/reopen again.

PASS: no generic repository-validation toast/error, no dangling take, no sibling clip disappearance and no shared source corruption.

## D. MK-300 routing, isolation and recording synchronization
- Explicitly select MK-300 as recording input and intended output.
- Keep hardware loopback disabled.
- Play a backing track, arm exactly one My Guitar track and record a rhythm part with clearly audible attacks aligned to the backing.
- Stop and immediately replay backing + recorded guitar.
- Confirm the guitar is not audibly about ~0.5 s late as in the previous physical finding.
- Confirm the recorded take contains guitar input only, not backing playback.
- Repeat one short take with monitoring Off/Auto/On as practical; monitoring choice must not change recorded content.
- Disconnect the selected input during a disposable short take and confirm capture stops safely rather than silently falling back to the tablet microphone.

PASS: route remains fail-closed, backing is not printed into the guitar take, and guitar/backing alignment is musically coherent without a repeatable systematic late offset.

## E. Live REC waveform stability
Record continuously for at least 2–3 minutes on an armed guitar track.
- Watch the live waveform from start to finish.
- Confirm its right edge grows monotonically with recording time.
- Confirm the drawing does not progressively accelerate, compress/pile older material backward, jump ahead of the recorded duration or freeze and then catch up in a burst.
- Stop recording and confirm the finalized waveform remains aligned to the same clip start/end.

PASS: live waveform remains temporally stable for the whole take and finalized media-derived waveform does not visibly shift clip timing.

## F. Focused regression smoke
Do not manually re-prove deterministic behavior already established by CI #616; only confirm target-device integration.
- Run `Auto seções`: no region after song end; preview slot changes to `Aplicar` + red `X` without layout shift.
- Start REC and confirm centered translucent `3 → 2 → 1` does not move Comparação/Timeline/tracks.
- Exercise ordinary Play/live seek and one Loop pass; transport must remain responsive.
- Record at least two takes on one track and switch active take.
- Listen for repeatable pops, dropouts, wrong pitch/speed or unintended one-sided audio.
- Export one short WAV or FLAC master and confirm playability; test MP3 only if the target reports encoder capability.

## PASS criteria
- exact run #616 identity above remains intact;
- trim, split/move/delete/trash, recording sync and long live waveform pass on the real Samsung/MK-300 setup;
- no repeatable P0/P1;
- no GuitarLab-attributable backing leakage or unintended input fallback;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of this exact APK.

Deterministic model/editor/history/timing-policy/file/codec math, generic API 36 integration and target-geometry assertions are already automated and must not be manually duplicated here.
