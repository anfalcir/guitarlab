# M4 alpha06 consolidation homologation checklist

Target candidate: GuitarLab Studio `0.2.0-alpha06`, versionCode 7.
Target device baseline: Samsung SM-X230, Android 16 / API 36.
Pocket Amp M2 hardware gate is already closed separately; this checklist validates the current M4 Studio consolidation.

## Pre-conditions
- install alpha06 over the previous signed GuitarLab build without uninstalling when Android permits;
- create/open a test project;
- import at least one supported managed WAV through Android Files;
- where practical, rename/move/remove the original external file after import and confirm the project remains self-contained;
- test in landscape, the target Studio orientation.

## Workspace / fullscreen
1. Android status/navigation bars are hidden during normal use and reappear transiently only by the normal edge gesture.
2. Core Studio workflow fits in one screen; the page itself does not vertically scroll.
3. Timeline receives the dominant screen area; only the track body scrolls internally when track count requires it.
4. No duplicate permanent Clips/Track structure panels are shown.
5. Time ruler reflects actual project duration rather than fixed decorative labels.
6. Loop heads are absent while Loop is disabled and appear when Loop is enabled.
7. Mixer can be shown/hidden, pinned/unpinned and closed in temporary mode without corrupting selection/state.
8. Timeline track selection and Mixer focus remain synchronized.

## Playback / routing
1. Play from frame 0; Stop mid-song; playhead remains near the last hardware-presented frame.
2. Move playhead while STOPPED and Play; audio begins at the selected position.
3. Let playback reach project end; transport returns safely to STOPPED.
4. Repeat Play/Stop at least 10 times without crash/hang/leak symptoms.
5. Select a concrete main output in Options and confirm playback uses it.
6. Disconnect the preferred output and verify controlled fallback to Android Auto routing without project corruption or app crash.
7. Reconnect/refresh devices and confirm routing preference resolves again without relying on the previous raw Android device ID.
8. Matching-rate mono WAV is heard in both output channels; matching-rate stereo preserves L/R.
9. Sample-rate mismatch is rejected clearly and non-destructively until resampling is gated.

## Loop / trim
1. Enable Loop: L◀/L▶ appear; define a short audible region and confirm repeated exact wrapping.
2. Disable Loop: loop heads disappear.
3. Marker/clip edits remain locked during active playback.
4. Enter Trim while STOPPED: mustard T◀/T▶ are visible and direct-manipulation targets.
5. Apply button remains clearly visible without page scrolling; Cancel is equally reachable.
6. Trim-in changes timeline start/source offset consistently; trim-out changes visible end/length.
7. Cancel preserves previously committed clip metadata.
8. Source bounds cannot be exceeded.
9. Playback/import/unrelated edits cannot race an open trim draft.
10. Managed source bytes remain unchanged after trim.

## Mixer / Master
1. Track gain audibly changes level and persists after project reopen.
2. Pan audibly moves stereo placement and persists.
3. Mute silences the track; Solo restricts playback to soloed non-muted tracks; Mute wins over Solo.
4. Track meters respond only to each audible track bus and do not display fake activity for muted/non-audible tracks.
5. Peak/RMS meters remain visually stable enough to read; peak hold/decay behaves naturally rather than flickering chunk-by-chunk.
6. Master gain changes final output level and persists after project reopen.
7. Master Peak/RMS responds to the final summed signal; CLIP indication appears when pre-clamp peak exceeds 0 dBFS.
8. Legacy project created before persistent Master gain opens at 0 dB without migration failure.
9. Mixer controls remain locked while transport/edit transactions own the project state.

## Managed-media / persistence regression
- project reopens after app/process restart;
- waveform remains available or regenerates safely;
- playback works without the original external import URI;
- normal move/trim/mute/gain/waveform actions never rewrite managed source media;
- no malformed/missing managed-source reference or project JSON corruption;
- Master/track mix settings survive save/reopen.

## Result policy
PASS requires no P0/P1 issue in the scope above. Record exact app version, commit, device/API, source WAV characteristics, selected audio route and any observed deviation. CI-green remains software evidence; this physical checklist is required before PR #1 may be marked ready and merged to `main`.
