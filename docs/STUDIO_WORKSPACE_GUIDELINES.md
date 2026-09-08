# Studio Workspace Guidelines

This document is a normative UI/UX contract for the GuitarLab Studio workspace on tablet.

## One-workspace rule
The Studio is a working surface, not a vertically stacked diagnostics page. On the target tablet, the normal editing workflow must remain concentrated in one workspace without requiring page-level vertical scrolling.

The fixed hierarchy is:
1. compact project header;
2. one central timeline surface containing marker heads, time ruler, tracks, clips and waveforms;
3. contextual trim confirmation bar only while trim is active;
4. one transport bar.

If a project grows beyond the available screen height, only the track/timeline body may scroll internally. Header, contextual edit confirmation and transport remain anchored.

## No duplicated representations
A track or clip must not be represented in a second permanent section merely to repeat information already visible in the timeline.

Therefore:
- there is no separate permanent `Track structure` section below the timeline;
- there is no separate permanent `Clips` manager duplicating timeline clips/waveforms;
- import belongs to the destination track row;
- clip actions belong to the clip itself;
- technical/debug checkpoint prose is not part of the normal Studio workspace.

## Timeline-first composition
The timeline is the primary content area and must consume most of the screen. Each track has a compact control/header lane at left and its audio lane at right. Clips are positioned using their timeline start/length and show their waveform when available.

Time labels must derive from project duration/sample rate. Hard-coded decorative time labels that do not correspond to the project are not allowed.

## Contextual actions
Actions should appear where they are relevant and should not require the user to search another section of the screen.
- `+` on a track imports audio into that track.
- Trim/Mute/Remove are attached to the clip.
- Trim confirmation appears in a dedicated contextual action bar while trim is active.

## Trim confirmation
Entering trim creates a draft only. The user must always have an obvious, persistent confirmation surface while the draft exists:
- `✓ Apply trim` is a filled, high-affordance button;
- `Cancel` is a separate secondary button;
- both remain visible without scrolling;
- playback stays disabled until Apply or Cancel;
- applying trim changes clip metadata only and never rewrites managed source audio.

A small text link is not sufficient affordance for committing an edit.

## Loop visibility
Loop boundary markers are contextual controls. `L◀` and `L▶` are visible only when loop is enabled. When loop is disabled, both markers and loop-specific visual clutter disappear. Loop state remains available from the transport control.

## Marker rules
The canonical marker rules from `TIMELINE_INTERACTION_GUIDELINES.md` remain binding:
- playhead blue;
- loop green;
- trim muted mustard;
- record red;
- >=48 dp marker interaction target;
- editing only while transport is stopped.

## Status and diagnostics
Normal success/debug prose must not occupy permanent Studio real estate. Only actionable/transient status is shown compactly, such as import progress or an operation error. Development checkpoint text belongs in repository documentation and diagnostics, not in the production workspace.

## Tablet ergonomics
Primary controls must remain readable and tappable without precision gestures. The design favors compact density, not tiny controls: fewer sections, less duplicated text, larger functional canvas and clear contextual actions.
