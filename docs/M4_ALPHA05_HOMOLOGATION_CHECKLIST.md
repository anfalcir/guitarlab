# M4 alpha05 consolidation homologation checklist — historical

> Status: retained as historical evidence for the signed `0.2.0-alpha05` playback/trim candidate. The current development branch contains newer Studio, fullscreen, Options, routing, Mixer and metering work. This checklist alone is no longer sufficient to close the current M4 branch gate. Use `TEST_AND_HOMOLOGATION_PLAN.md` for the current physical consolidation scope and build a newer signed candidate first.

Target candidate: GuitarLab Studio `0.2.0-alpha05`, versionCode 6.
Target device baseline: Samsung SM-X230, Android 16 / API 36.
Pocket Amp M2 hardware gate is already closed separately; this checklist validates only the historical alpha05 M4 playback/trim scope.

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
4. Let playback reach project end: transport returns to STOPPED without crash or runaway clock.
5. Repeat Play/Stop at least 10 times: no crash, hang, media corruption or progressively worsening behavior.
6. Confirm the visible playhead advances with playback and does not keep moving after Stop.

## Loop
1. While STOPPED, position green L◀ and L▶ markers around a short, audible region.
2. Enable Loop and start playback.
3. Confirm playback repeatedly wraps from Loop Out back to Loop In.
4. Confirm markers cannot cross.
5. Confirm marker manipulation and other timeline edits are locked while playback is active.
6. Stop and verify controls unlock again.

## Trim
1. Enter Trim on a clip while STOPPED.
2. Confirm mustard T◀ / T▶ marker heads are clearly visible and direct manipulation targets.
3. Move T◀ inward and Apply: timeline start/source offset changes non-destructively; source audio remains unchanged.
4. Move T▶ inward and Apply: visible duration shortens without rewriting source media.
5. Alter markers then Cancel: project metadata remains unchanged from the last committed state.
6. Attempt to extend beyond immutable source bounds: policy must prevent invalid trim.
7. While a trim draft is open, playback/import/other clip edits remain unavailable until Apply or Cancel.
8. After applying trim, Play the affected area and confirm audible alignment matches the visible clip.

## Managed-media regression
- project reopens after process death/app restart;
- waveform remains available or regenerates safely;
- playback works without the original external URI;
- trim/mute/move/waveform generation do not alter managed source bytes;
- no project JSON corruption or missing managed source reference.

## Historical result policy
PASS for this file means no P0/P1 issue in the alpha05 scope above. It does **not** close the current branch M4 gate because the branch has materially expanded since alpha05.
