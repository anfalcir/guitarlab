# Studio Workspace Guidelines

Updated: 2026-09-09

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
- edge autoscroll is continuous, proportional, capped and retargets after each scroll;
- cancel and same-origin drop perform no mutation/history entry;
- completed drop performs exactly one metadata mutation and Undo/Redo restores snapshots.

Drag must remain disabled during import, active Trim, history mutation and incompatible transport/recording state. Gesture closures must not depend on stale derived values; use central current state or `rememberUpdatedState` where needed.

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
Trim draft lives only in the active waveform. It starts around 35%/65%, uses safe non-destructive source clamps, readable time bubbles near markers and explicit Apply/Cancel. No Trim guide crosses unrelated lanes.

## Loop/playhead
Playhead crosses all lanes. Loop guides cross lanes only while Loop is active. Marker heads retain ergonomic touch targets and canonical semantic colors.

## Status and diagnostics
Transient success/error feedback must not reserve permanent structural space. Debug/checkpoint prose is never part of the production Studio workspace.

## Tablet ergonomics and orientation
Primary controls remain readable/tappable without precision gestures. Landscape and portrait preserve the open project and use responsive layout rather than separate product behavior.