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

The muted mustard is intentional: trim remains visually distinct from recording/error semantics while still reading as an editing boundary. Record red is reserved for capture/recording meaning.

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

This rule prevents state races and accidental edits while the audio engine owns timeline progression.

## Canonical transport controls
The primary Studio transport bar uses familiar symbols rather than text labels:
1. return to timeline start (`|◀` or equivalent standard icon);
2. play (`▶`), which changes to stop (`■`) when transport becomes active;
3. record (`●`) using record red;
4. loop (`↻` or equivalent loop icon), with loop-green active indication.

There is no separate pause button in the GuitarLab transport model. The intended state model is STOPPED / PLAYING / RECORDING. Play transitions to Stop while active. Accessibility labels/tooltips remain present even though visible button text is not.

## Feature gating
No enabled transport control may pretend to work before its underlying engine is implemented. Play/record remain disabled until the production transport engine is software-gated. Visual shell, marker semantics and lock policy may be implemented earlier so the engine plugs into a stable UX contract.

## Editing semantics
Marker UX never changes the non-destructive media policy:
- trim changes clip metadata only;
- source audio in `media/source/` remains immutable;
- loop changes playback-range state only;
- playhead/record-head change transport position/state only;
- waveform/proxy/resampling/render outputs remain derived artifacts.

## Visual hierarchy
- transport has one clear primary location;
- marker heads sit above the timeline they control;
- semantic color is used in compact accents, not large permanent blocks;
- destructive-looking actions require explicit intent/confirmation where relevant;
- controls must remain readable and touch-friendly on tablet and larger UI scales.

## Current implementation checkpoint
M4 now has the marker-head contract, muted-mustard trim semantics, a canonical symbol-only transport bar, and a pure transport policy that locks timeline editing during PLAYING/RECORDING. Play/record are intentionally disabled until the production audio engine is wired and validated; return-to-start and loop configuration are real stopped-state controls.
