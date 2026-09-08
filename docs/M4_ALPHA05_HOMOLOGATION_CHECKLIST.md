# M4 alpha05 consolidation homologation checklist

Target candidate: GuitarLab Studio `0.2.0-alpha05`, versionCode 6.
Target device baseline: Samsung SM-X230, Android 16 / API 36.
Pocket Amp M2 hardware gate is already closed separately; this checklist validates the M4 Studio consolidation only.

## Pre-conditions
- install alpha05 over the previous signed GuitarLab build without uninstalling;
- open an existing project or create a test project;
- import at least one supported WAV through Android Files;
- after import, the project must rely on the immutable managed copy under app-controlled storage;
- where possible, move/rename/remove access to the original external file and confirm the project still works.

## Playback
1. Play from frame 0: audio starts, UI enters PLAYING and Play changes to Stop.
2. Stop mid-song: playback stops cleanly and the playhead remains near the last hardware-presented frame.
3. With transport STOPPED, drag the blue playhead to a different position and Play: audio begins from that position.
4. Let playback reach the project end: transport returns to STOPPED without crash or runaway clock.
5. Repeat Play/Stop at least 10 times: no crash, hang, media corruption or progressively worsening behavior.
6. Confirm the visible playhead advances with playback and does not keep moving after Stop.

## Loop
1. While STOPPED, position green L◀ and L▶ markers around a short, audible region.
2. Enable Loop and start playback.
3. Confirm playback repeatedly wraps from Loop Out back to Loop In.
4. Confirm markers cannot cross.
5. Confirm loop-marker manipulation and other timeline edits are locked while playback is active.
6. Stop and verify controls unlock again.

## Trim
1. Enter Trim on a clip while STOPPED.
2. Confirm mustard T◀ / T▶ marker heads are clearly visible and are the direct manipulation targets.
3. Move T◀ inward and Apply: clip timeline start/source offset changes non-destructively; source audio remains unchanged.
4. Move T▶ inward and Apply: visible clip duration shortens without rewriting source media.
5. Enter Trim, alter markers, then Cancel: project clip metadata remains unchanged from the last committed state.
6. Attempt to extend beyond immutable source bounds: UI/policy must prevent invalid trim.
7. While a trim draft is open, playback/import/other clip edits remain unavailable until Apply or Cancel.
8. After applying trim, Play the affected area and confirm audible alignment matches the visible trimmed clip.

## Managed-media regression
- project reopens after process death/app restart;
- waveform remains available or regenerates safely;
- playback works without the original external import URI being available;
- trim, mute, move and waveform generation do not alter managed source bytes;
- no project JSON corruption or missing managed source reference.

## Result policy
PASS requires no P0/P1 issue in the scope above. Record exact app version, commit, device/API, source WAV characteristics and any observed deviation. CI-green remains software evidence; this checklist is the physical M4 consolidation gate before PR #1 may move to `main`.
