# Studio Workspace Guidelines

Updated: 2026-09-14

This document is a normative UI/UX contract for the GuitarLab Studio workspace on tablet.

## One-workspace rule
The Studio is a working surface, not a vertically stacked diagnostics page. Header, timeline, contextual editing and transport remain concentrated in one workspace. If tracks overflow, only the timeline body scrolls internally.

## Mutation/state consistency
Every committed project mutation must resynchronize persisted project state and all derived UI state that depends on it, including history availability, playback/mixer state and editing readiness. No feature may update only its local visual state and leave `canUndo`/`canRedo` or transport readiness stale.

Asynchronous operations that started for one project/session must verify that project/session is still current before publishing results.

## Timeline-first composition
Each track has a compact sidebar/control area and aligned audio lane. Clips use real timeline start/length and waveform data. Time labels derive from project duration/sample rate.

## Drag/reorder contract
- long press begins drag only while structural editing is permitted;
- track and waveform drag share one coordinator owned by the workspace;
- source clip/track identity is captured at drag start and revalidated at commit;
- clip drop resolves explicitly to `Move`, `Delete` or `NoOp`;
- cancel, invalid and same-origin drops perform no mutation/history entry;
- completed drop performs exactly one metadata transaction and Undo/Redo restores snapshots.

## Clip deletion and track clearing
- `Excluir clipe` deletes one selected clip/segment and always confirms;
- `Limpar toda a pista` is track-content scoped and belongs to track configuration, not as a second neighboring clip-trash action;
- `Excluir pista` remains structural and distinct from clearing content;
- shared managed media survives while referenced by any remaining clip/take.

## Configurar pista
Wide/landscape dialogs use balanced columns for track properties and source metadata. Track-wide destructive actions are clearly separated and explicitly scoped. Track names remain 1–24 characters.

## Trim contract
- one valid tap on `Cortar` opens Trim;
- if Trim cannot open because of current state, the user receives explicit feedback instead of a silent no-op;
- start/end are independent handles with ergonomic touch targets and independent semantic identities;
- gesture ownership remains with the handle acquired at gesture start;
- applying Trim records one metadata edit; cancel is a no-op; Undo/Redo and save/reopen preserve the retained region.

## Practice controls + Mixer
Comparison/Timeline is one reusable control surface, not two independent implementations.
- when Mixer is open on wide layouts, `Mixer` remains anchored left, practice controls occupy the available center, and Pin/Close remain anchored right;
- when Mixer closes, the same control state returns to workspace flow;
- controls must not overlap, shift Pin/Close off-screen or diverge in state between presentations;
- the arrangement should reclaim vertical space without reducing target size or readability.

## Recording stop semantics
During active capture, transport Stop and tapping REC again invoke the same successful, idempotent recording-finalization path. Stop during countdown cancels safely. Once finalization begins, duplicate stop/finalize input must not create duplicate takes or corrupt media.

## Level analysis
Level analysis reports/recommends against the effective audible signal after current track/clip gain. Applying a recommendation must update the project/mixer/history state, and a subsequent analysis must converge rather than repeatedly suggesting the exact correction already applied unless the actual effective level still requires it.

## Live recording waveform
Live waveform is a temporal visualization of captured frames, not callback cadence.
- historical and new material share one current temporal bucket resolution;
- rebucketing preserves contiguous represented time and maximum transient;
- visual geometry spans each bucket's represented interval rather than collapsing it to a midpoint stroke;
- old material must not progressively become sparse while recent material piles densely at the right edge;
- displayed right edge grows monotonically with recording frames;
- finalized waveform replacement must not move clip timeline start/end.

## Loop/playhead and overlays
Playhead crosses all lanes. Loop guides cross lanes only while Loop is active. REC countdown is a presentation-only centered translucent overlay and must not consume layout height.

## Status and diagnostics
Transient success/error feedback must not reserve permanent structural space. User-visible errors are safe and understandable; raw repository invariant text belongs in diagnostics, not normal UX.

## Tablet ergonomics
Primary controls remain readable/tappable without precision gestures. Landscape and portrait preserve the open project and use responsive layout. Exact geometry is automated; physical validation confirms tactile/visual behavior that an emulator cannot establish.