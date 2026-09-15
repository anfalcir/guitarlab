# Studio Workspace Guidelines

Updated: 2026-09-14

This document is a normative UI/UX contract for the GuitarLab Studio workspace on tablet.

## One-workspace rule
The Studio is a working surface, not a vertically stacked diagnostics page. Header, timeline, contextual editing and transport remain concentrated in one workspace. If tracks overflow, only the timeline body scrolls internally.

## No duplicated representations
Do not add permanent duplicate track/clip managers. Import belongs to the destination track, clip actions stay contextual, and development/debug prose belongs in diagnostics/documentation.

## Timeline-first composition
Each track has a compact sidebar/control area and an aligned audio lane. Clips use real timeline start/length and waveform data. Time labels derive from project duration/sample rate.

## Drag/reorder contract
- long press begins drag only while structural editing is permitted;
- track and waveform drag share one coordinator owned by the workspace;
- do not use `Popup` for drag ghosts;
- ghost follows the pointer continuously and is drawn above every lane;
- reorder destination uses a separate insertion line/gap and position label;
- clip migration highlights and names the target track;
- destination calculations use current measured lane bounds from `LazyListState.layoutInfo`;
- source clip/track identity is captured at drag start and revalidated at commit;
- clip drop resolves explicitly to `Move`, `Delete` or `NoOp` before any project mutation;
- edge autoscroll is continuous, proportional, capped and retargets after each scroll;
- cancel, invalid and same-origin drops perform no mutation/history entry;
- completed drop performs exactly one metadata transaction and Undo/Redo restores snapshots.

Drag must remain disabled during import, active Trim, history mutation and incompatible transport/recording state. Gesture closures must not depend on stale derived values; use central current state or `rememberUpdatedState` where needed.

## Clip deletion and trash target
- `Excluir clipe` is the explicit contextual command for deleting one clip/segment; it is distinct from `Limpar pista` and `Excluir pista`;
- deleting a clip always requires confirmation;
- while a clip is dragged, a trash target may appear as a transient workspace affordance;
- trash hover is visually distinct from lane migration;
- dropping on trash opens the same confirmation/domain-delete flow as `Excluir clipe`; it never deletes immediately on pointer release;
- cancel is a strict no-op;
- deleting one split segment must not delete siblings, invalidate their recording-take lineage or remove shared managed media still referenced elsewhere.

## Contextual track actions
- `+` imports audio into an empty destination track;
- the three-dot sidebar menu owns content actions, including `Limpar pista`;
- `Limpar pista` removes clips/content but preserves track identity, role, color, order and mix;
- `Configurar pista` owns track properties and structural `Excluir pista`;
- `Excluir pista` is disabled while clips exist and explains that the user must clear the track first;
- legacy ordering arrows are not allowed.

## Configurar pista
Wide/landscape dialogs use two balanced columns: name/function/compact color palette at left and `Áudio fonte` metadata at right. Narrow/portrait dialogs stack the same sections. Internal dialog scrolling is used only when content cannot fit.

Source metadata should show, when available: filename, format, sample rate, channels, bit depth, encoding and duration. An empty track shows an intentional compact empty state. Track names remain 1–24 characters; `Salvar` and `Cancelar` stay clear in pt-BR.

## Trim contract
Trim draft lives only in the active waveform and remains non-destructive.
- start and end are independent handles with ergonomic touch targets and independent semantics/test identity;
- pointer position is mapped deterministically to frames and clamped against immutable source bounds and the opposite handle;
- time bubbles are passive visuals and must never steal pointer input;
- when handles are close, the handle acquired at gesture start retains ownership instead of unexpectedly swapping;
- clip drag/playhead/loop marker gestures must not win inside an active trim-handle target;
- applying Trim records one metadata edit; cancelling is a no-op; Undo/Redo and save/reopen preserve the same retained region.

No Trim guide crosses unrelated lanes.

## Split-segment workflow
Temporal split creates independently editable clip segments even when they share one recording-take lineage. Moving/deleting one child must preserve valid siblings. If the canonical take clip leaves, one surviving sibling is promoted deterministically; a take is removed only when no segment remains. A valid user gesture must never surface a raw repository-validation failure caused by dangling take references.

## Loop/playhead
Playhead crosses all lanes. Loop guides cross lanes only while Loop is active. Marker heads retain ergonomic touch targets and canonical semantic colors.

## Practice-control geometry
`Comparação` and `Timeline` together consume the available row width on wide tablet layouts. `Limpar seções` stays in the natural Timeline flow; it is not artificially pinned in a way that fragments the group. `Auto seções` has one fixed-width slot and preview mode replaces that same slot with `Aplicar` + red `X`. On narrower layouts, groups stack/scroll deliberately rather than clipping actions.

## Recording countdown overlay
REC countdown is presentation-only overlay state: 3 seconds, centered, translucent and visually large. It must not be a child that consumes Column height or moves the practice controls, tracks or Mixer.

## Live recording waveform
The live waveform is a temporal visualization of captured frames, not callback cadence. Its displayed right edge must grow monotonically with `recordingFrames`; callback bursts/UI stalls must not make older material pile backward or visually accelerate. A finalized media-derived waveform may replace the transient live envelope only without changing the clip's timeline start/end.

## Status and diagnostics
Transient success/error feedback must not reserve permanent structural space. User-visible errors should be safe/understandable; raw repository invariant text belongs in diagnostic evidence, not normal UX. Debug/checkpoint prose is never part of the production Studio workspace.

## Tablet ergonomics and orientation
Primary controls remain readable/tappable without precision gestures. Landscape and portrait preserve the open project and use responsive layout rather than separate product behavior. Exact target-tablet geometry is covered by automation; final physical validation confirms only tactile acquisition/visual feel that an emulator cannot establish.
