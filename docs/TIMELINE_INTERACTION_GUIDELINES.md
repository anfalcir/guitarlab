# Timeline interaction guidelines

Updated: 2026-09-14

## Global invariants
Structural timeline edits are allowed only in compatible stopped states and are blocked during import, active Trim/history operations and incompatible transport/recording states. Managed source bytes remain immutable. Every completed edit is a project/history transaction; preview motion and cancelled gestures are no-ops.

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

## Clip/waveform drag
- long press on waveform starts clip drag;
- same shared coordinator/overlay/autoscroll policy;
- source clip/track identity is snapshotted at drag start and revalidated before commit;
- drop intent is resolved explicitly as `Move`, `Delete` or `NoOp`;
- release on a different compatible lane performs metadata-only clip migration;
- same-track/invalid/cancelled drop is a no-op;
- startFrame, source path, trim/sourceStartFrame, length, gain, mute and source metadata remain intact unless the chosen edit explicitly changes them;
- stale or invalid drop must fail without partially mutating project/history/media state.

## Drag-to-trash
- trash target exists only while a clip is actively dragged;
- hover over trash is visually distinct from lane migration;
- dropping on trash does **not** delete immediately: it opens the same confirmation path used by `Excluir clipe`;
- confirmation deletes only the selected clip segment through the shared domain operation;
- cancellation is an exact no-op;
- shared managed source/proxy media must not be removed while another clip still references it.

## Split-take lineage
A temporal split may create multiple clip segments that share one `RecordingTake` lineage.
- moving/deleting one child detaches only that child;
- remaining siblings keep the take alive;
- if the take's canonical `clipId` leaves, a surviving sibling is promoted deterministically;
- the take is removed only when no clip still references it;
- one take lineage must never span incompatible tracks after a completed edit;
- save/reopen validation must not observe a dangling `takeId`/`clipId` reference.

## Autoscroll
Top/bottom edge zones continuously scroll while the pointer remains there. Speed grows with edge proximity and is capped. Scrolling stops immediately outside the zone or on end/cancel. Target is recalculated from fresh `LazyListState.layoutInfo` after movement.

## Trim interaction contract
Trim is non-destructive metadata editing local to the active clip.

- entering Trim creates independent **start** and **end** handles rather than relying on one opaque range-slider gesture surface;
- each handle has its own semantic/test identity and ergonomic touch target;
- pointer X converts through one pure policy into a timeline frame;
- start is clamped against immutable-source lower bound and `end - 1 frame`;
- end is clamped against `start + 1 frame` and the maximum source-backed end;
- local time bubbles are passive visuals and must not intercept handle gestures;
- close handles remain independently ownable; dragging one must not unexpectedly swap to the other;
- applying Trim changes timeline/source metadata only; source bytes remain unchanged;
- Undo/Redo and save/reopen must reproduce the same retained region.

## Interaction collision rules
- trim-handle acquisition has priority inside an active trim handle target;
- clip drag cannot start from a trim handle while Trim is active;
- playhead/loop markers do not own clip-handle gesture space;
- structural clip edits remain stopped-only even though playhead-only seek is intentionally allowed during ordinary Play;
- recording/countdown/finalization rejects structural timeline gestures.

## Required regression evidence
Automated coverage must include trim start/end handle movement, close-handle cases, `Move`/`Delete`/`NoOp` drag intent, split-child migration/deletion, save/reopen lineage integrity and target-geometry Compose interaction. Real-device homologation only confirms tactile acquisition/ergonomics that cannot be established in emulator tests.
