# M7 alpha1 — Physical Homologation Checklist

Target: Samsung SM-X230 Android 16/API36 + normal GuitarLab/Pocket Amp workflow.
Original candidate: `0.4.0-alpha1` / versionCode 17.
Superseding regression candidate: `0.4.0-alpha2` / versionCode 18.

## Digital regression already removed from the physical checklist
- SRC matrix 44.1→48, 48→44.1, 88.2→48, 96→48 and 44.1→96: duration, pitch, RMS, stereo independence and immutable input PASS.
- same-rate conversion: byte-exact independent copy PASS.
- deterministic Float32 master: timeline placement, gain, hard pan, clipping, fades, overlap/crossfade and stereo routing PASS.
- model/persistence: M7 editing-domain bounds, frame overflow, fade bounds, Undo/Redo sequence and malformed package cleanup PASS.
- duplicate project: managed source and proxy copying plus rollback on missing media PASS.

Only the sensory/hardware portions below remain for the user; file/model mathematics must not be re-delegated to physical homologation.

## A. Home project actions
- Three-dot menu shows Renomear, Salvar e exportar, Duplicar, Excluir.
- Rename persists after reopen/app restart and is reflected in Studio/export filenames.
- Salvar e exportar opens the same visual/semantic modal as Studio.
- From Home, save `.guitarlab`, export WAV32f, FLAC, MP3; verify nonempty/playable outputs and reopen the project package.

## B. Sample-rate conversion
- Import 44.1 kHz into a 48 kHz project and 48 kHz into a 44.1 kHz project where practical.
- Import must complete with processing feedback, then playback at correct pitch and duration.
- Verify imported provenance still reports native source rate and editing path reports converted rate.
- Export master and confirm duration/pitch remain correct.
- Regression: same-rate import remains passthrough/no unnecessary conversion.

## C. Fades/crossfades
- Open clip menu > Fades, apply audible fade-in and fade-out; reopen and confirm persistence.
- Export master and confirm fades match realtime playback.
- Place two clips on same track with overlap and apply Crossfade with next.
- Verify smooth transition and Undo/Redo behavior.
- Attempt crossfade without overlap: app should reject safely, not corrupt clips.

## D. Larger-session/performance
- Use a representative larger session with multiple tracks/clips.
- Play, seek, loop, open mixer, manipulate Mute/Solo and edit fades.
- No repeatable ANR/crash or progressive memory/performance collapse.

## E. Regression
- recording + M6 calibrated take placement;
- drag/trim/split/stereo separation;
- `.guitarlab` save/open;
- WAV/FLAC/MP3 export;
- project rename and live Mute/Solo.

M7 closes only with zero repeatable P0/P1 and explicit user approval.
