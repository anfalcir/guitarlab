# Timeline Interaction Guidelines

This document is a normative UX contract for GuitarLab Studio timeline controls.

## Core rule: interact with the marker head, not the line
Any timeline position that users are expected to change directly must expose a clear draggable marker head at the top of its vertical guide. The vertical guide is visual orientation only; it must not be the only or primary interaction target.

This applies to:
- playhead / current execution position;
- recording head when recording transport is active;
- loop in and loop out boundaries;
- trim start and trim end boundaries;
- future punch-in/out, range-selection, automation or similar timeline boundaries when direct manipulation is appropriate.

The intended visual metaphor is similar to a document-ruler marker: a compact head at the top, visually connected to a thin vertical line.

## Touch target and drag behavior
- marker heads must provide at least a 48 dp touch/drag target even if the visible glyph is smaller;
- dragging begins from the marker head region, not from the thin guide line;
- guide lines remain intentionally narrow and low-noise;
- marker movement is clamped to valid timeline/source bounds;
- paired markers cannot cross when the domain requires ordering, such as loop in/out;
- moving a marker must update the numeric/time position continuously enough to feel direct;
- later snapping must be optional/contextual and must never make coarse positioning difficult.

## Semantic colors
Color is restrained and never the only cue. Each semantic family also has a label/glyph and context.

- Playhead/navigation: blue `#3B82F6`.
- Loop in/out: green `#2E9D67`.
- Trim in/out: restrained coral/red `#D65A5A`.
- Active recording head/action: record red `#D94A4A`.

These colors are accents, not large-area fills. They must coexist with the neutral GuitarLab light/dark design system without making the Studio look game-like, neon, or visually noisy.

## Marker identity
Compact marker-head labels are permitted when they improve immediate recognition:
- `P` for playhead;
- `L◀` / `L▶` for loop in/out;
- `T◀` / `T▶` for trim start/end;
- `R` for record head.

Icons may replace these labels later, but accessibility text and semantic identity must remain explicit. Color alone is insufficient.

## Editing semantics
Marker UX never changes the non-destructive media policy:
- trim markers modify clip metadata only;
- source audio in `media/source/` remains immutable;
- loop markers change playback range state only;
- playhead/record-head markers change transport position/state only;
- derived waveform/proxy/render files may be regenerated without touching source media.

## Visual hierarchy and discoverability
The Studio should make primary controls visually obvious without becoming crowded:
- one clear primary location for transport controls;
- marker heads visually sit above the timeline they control;
- secondary configuration should use recognizable icon + label/tool-tip patterns rather than hidden line gestures;
- destructive-looking actions must have explicit labels/confirmation where data loss could occur;
- avoid relying on long-press, invisible edge zones or pixel-precise line grabbing for core workflows;
- avoid unnecessary permanent color blocks; semantic color appears only where it carries meaning.

## Accessibility and tablet ergonomics
- touch targets at least 48 dp for primary direct-manipulation controls;
- sufficient contrast in light and dark themes;
- do not encode state only by hue;
- marker heads must remain usable at tablet scaling and larger UI scale settings;
- controls should not require mouse-like precision.

## Current implementation checkpoint
M4 currently implements the shared marker-head interaction foundation for playhead and loop boundaries. The same component contract already defines trim and record-head semantics for later wiring. Audio playback/record movement is intentionally kept behind its own transport gate; no UI should claim live playback behavior before that engine is validated.
