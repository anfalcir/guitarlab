# M5 alpha14 final homologation checklist

Candidate: `0.2.0-alpha14` / versionCode 15. M5 remains OPEN until physical PASS.

## Import UX
- Select a representative FLAC/MP3/WAV stereo file through Android SAF.
- Immediately after returning to GuitarLab, verify a blocking `Preparando áudio` progress surface is visible until processing completes.
- Verify no duplicate import can be triggered while processing.

## Stereo integrity
- Confirm two visibly distinct channel envelopes labelled L and R are rendered for a stereo source.
- Base: choose `Manter estéreo`; confirm one stereo clip remains and playback preserves both channels.
- Pencil menu: confirm `Dividir no cursor` remains temporal split and `Separar estéreo em 2 pistas mono` appears separately only for stereo clips.
- Generic/stereo track: separate L/R and confirm synchronized mono tracks, left/right pan and source start/duration preservation.
- Guitar template: import stereo into Guitarra Ref. E or D; confirm dialog proposes distribution to Guitarra Ref. E/D and creates synchronized mono clips in both existing tracks.
- Repeat for Minhas Guitarras L/R role pair.

## Regression
- Drag track and clip, including edge autoscroll.
- Trim, temporal split, duplicate, clear/delete, Undo/Redo.
- Save `.guitarlab`, close/reopen, verify stereo or separated state persists.
- Export WAV 32f, FLAC and MP3 and verify audible result.
- Track Settings, Share modal, Mixer/Master, REC/Arm remain functional.

PASS requires zero repeatable P0/P1 and explicit user approval. M6 does not start before this gate closes.
