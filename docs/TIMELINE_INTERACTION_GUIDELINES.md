# Timeline interaction guidelines

Updated: 2026-09-09

## Global invariants
Structural timeline edits are allowed only in compatible stopped states and are blocked during import, Trim, history operations and incompatible transport/recording states. Managed source bytes remain immutable.

## Track reorder
- long press starts drag;
- coordinator lives at workspace level;
- ghost is an in-tree overlay above the LazyColumn, never a Popup;
- ghost follows pointer continuously pixel-by-pixel;
- insertion indicator is independent and snaps between/at tracks;
- target uses current measured lane bounds;
- release calls one `ProjectTrackEditor.reorderTrack` mutation;
- same-index drop and cancel are no-ops;
- stable track IDs/configuration and clip references are preserved.

## Clip/waveform migration
- long press on waveform starts clip drag;
- same shared coordinator/overlay/autoscroll policy;
- target lane is visibly highlighted and named;
- release on a different lane performs metadata-only `ProjectClipEditor.moveClipToTrack`;
- startFrame, source path, trim/sourceStartFrame, length, gain, mute and source metadata remain intact;
- same-track drop/cancel are no-ops; Undo returns the prior project snapshot.

## Autoscroll
Top/bottom edge zones continuously scroll while the pointer remains there. Speed grows with edge proximity and is capped. Scrolling stops immediately outside the zone or on end/cancel. Target is recalculated from fresh `LazyListState.layoutInfo` after movement.

## Trim regression contract
Trim is edited only inside the active waveform, opens around 35%/65%, clamps to immutable source bounds, shows readable time bubbles near handles and remains non-destructive.
