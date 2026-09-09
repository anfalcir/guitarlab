# M5.C — Recording Coordinator + Studio UX Consolidation

Status: implementation plan for the alpha08 development line. Every sub-checkpoint must pass unit tests, Android Lint and debug APK assembly before the next write batch.

## Recording coordinator
- Record always starts with the mandatory visible/cancelable 5-second countdown (`5 → 4 → 3 → 2 → 1`). No capture occurs during the countdown.
- At zero, the Studio revalidates permission, armed tracks, input route and project state before opening `AudioRecord`.
- At least one armed track is required.
- Valid capture is written to an uncommitted project recording transaction, finalized and validated, then atomically promoted to `media/source/`.
- Armed tracks receive clip metadata referencing the same immutable managed source. The audio bytes are never duplicated merely because multiple tracks are armed.
- Backing playback may run concurrently with capture. Fine round-trip latency compensation remains M6.
- Route loss after valid frames produces a safe partial take; zero-frame/invalid capture produces no clip.
- Input Peak/RMS and clipping are surfaced during capture.

## History and rotation safety
- Undo/Redo are real project-history actions and live after Loop in the top transport.
- History stores bounded project metadata snapshots; undo never rewrites or deletes immutable managed media.
- Structural Undo/Redo are STOPPED-only and are blocked during countdown/record/import/trim transactions.
- App route and active project survive Android configuration changes such as landscape ↔ portrait rotation.

## Mixer refinement
- Arm uses the standard record-circle symbol instead of the letter R. Inactive is deliberately dim red; active is vivid red.
- Volume and Pan are named inside their controls with compact value overlays.
- Pan is rendered from the center: center has no directional fill; left/right movement fills only from center toward the selected side.
- 0 dB and center remain gentle snap/reference points without preventing fine adjustment.
- Mixer height grows modestly (~20%) while track strips remain horizontally scrollable and Master remains fixed at the right.
- Track Volume/Pan and Master gain remain editable during Play/Record.

## Timeline and track sidebar
- Timeline clip cards become waveform-first: filename and edit icons leave the waveform surface.
- Empty track: title + import (`+`) on the first row; role + settings on the second row.
- Populated track: title + overflow menu; role + settings on the second row.
- Overflow menu contains contextual clip actions such as Duplicate, Split at playhead, Trim and Delete using icon + pt-BR label.
- Track settings show managed source metadata: filename, format, sample rate, channels, bit depth and encoding.
- Long-press drag on the sidebar reorders the whole track and its content.
- Long-press drag on the waveform moves the clip to another track without copying source media.

## Marker geometry
- One compact marker rail is used; no visible empty stacked rails.
- Logical touch lanes inside that compact rail are: Trim (top), Loop (middle), Playhead (bottom), preventing overlapping heads.
- Playhead and Loop guide lines extend through all track lanes.
- Trim guides extend only through the track/clip currently being trimmed.
- Newly enabled Loop gets a useful non-zero range, with the right edge initialized around 10% of project duration subject to safe minimum/maximum bounds.
- Marker semantic colors and 48dp touch affordances remain unchanged.

## Gate discipline
No physical M5 support is claimed until the complete M5 software gate is green and a signed alpha08 candidate passes the Samsung SM-X230 + Pocket Amp recording checklist.