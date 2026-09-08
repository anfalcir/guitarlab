# Timeline Interaction Guidelines

This document is a normative UX contract for GuitarLab Studio timeline and transport controls.

## Core rule: interact with the marker head, not the line
Any timeline position that users are expected to change directly must expose a clear draggable marker head at the top of its vertical guide. The guide line is visual orientation only; it is never the only or primary interaction target.

This applies to playhead/current execution position, recording head, loop in/out, trim start/end and future punch/range controls.

## Touch target and drag behavior
- marker heads must provide at least a 48 dp touch/drag target even if the visible glyph is smaller;
- dragging begins from the marker-head region, not from the thin guide line;
- marker movement is clamped to valid timeline/source bounds;
- paired markers cannot cross where ordering matters;
- color is never the only identity cue;
- no core workflow may depend on pixel-precise line grabbing, hidden edge zones or long-press discovery.

## Semantic colors
Colors are restrained accents designed to remain elegant in the neutral/teal GuitarLab theme.
- Playhead/navigation: blue `#3B82F6`.
- Loop in/out: green `#2E9D67`.
- Trim in/out: muted mustard `#9C741F`.
- Active recording head/action: record red `#D94A4A`.

## Marker visibility
Contextual markers must not remain on screen when their feature is inactive.
- Playhead remains visible as the primary timeline position.
- `L◀` / `L▶` are rendered only while loop is enabled.
- `T◀` / `T▶` are rendered only while a trim draft is active.
- recording markers are rendered only while relevant to recording state.

This prevents inactive controls from looking actionable and reduces timeline clutter.

## Marker identity
Compact marker labels may be used when they improve recognition:
- `P` playhead;
- `L◀` / `L▶` loop boundaries;
- `T◀` / `T▶` trim boundaries;
- `R` recording head.
Accessibility descriptions remain mandatory even if later replaced by icons.

## Transport/edit locking
Timeline editing is allowed only while transport is stopped.
- while PLAYING or RECORDING, playhead drag, loop-boundary drag, trim-boundary drag, clip move/trim and other timeline edits are locked;
- loop configuration cannot be changed during active transport;
- import/clip editing is also blocked while transport is active;
- the UI must visibly communicate the locked state instead of silently ignoring the user;
- transport-owned playhead/record-head movement is automatic while active and must not be confused with user editing.

## Canonical transport controls
The primary Studio transport bar uses familiar symbols rather than text labels:
1. return to timeline start (`|◀` or equivalent standard icon);
2. play (`▶`), which changes to stop (`■`) when transport becomes active;
3. record (`●`) using record red;
4. loop (`↻` or equivalent loop icon), with loop-green active indication.

There is no separate pause button in the current transport model. The intended state model is STOPPED / PLAYING / RECORDING.

## Trim commit affordance
Trim is a draft until explicitly committed. While a trim draft is active, the workspace must keep an obvious `Apply trim` action and separate `Cancel` action anchored on screen. The confirm action must look like a real button, not a low-emphasis text link.

## Editing semantics
Marker UX never changes the non-destructive media policy:
- trim changes clip metadata only;
- source audio in `media/source/` remains immutable;
- loop changes playback-range state only;
- playhead/record-head change transport position/state only;
- waveform/proxy/resampling/render outputs remain derived artifacts.

## Visual hierarchy
- transport has one clear primary location;
- marker heads sit directly above the timeline they control;
- inactive contextual markers disappear;
- semantic color is used in compact accents, not large permanent blocks;
- controls remain readable and touch-friendly on tablet;
- the Studio follows the one-workspace/no-duplication rules in `STUDIO_WORKSPACE_GUIDELINES.md`.
