# In-app User Guide Policy

Updated: 2026-09-14

## Purpose
The top-bar `Ajuda` dialog is part of the GuitarLab product contract, not optional marketing copy. It must give a novice user a short, accurate explanation of the controls and workflows that are actually present in the current app.

## Mandatory synchronization rule
Any change that adds, removes, renames or materially changes a user-visible control, configuration or workflow must review `StudioUserGuideDialog.kt` in the same development block. Update the guide whenever the change affects:
- tracks or track functions;
- Play/Stop, playhead, Loop or timeline behavior;
- markers, sections or automatic section detection;
- recording, armed tracks or punch/loop recording;
- comparison modes or mixer controls;
- import, export, monitoring, input/output routing or Studio options.

A release candidate is not documentation-complete when its in-app guide describes stale behavior or uses names that no longer match the UI.

## Writing standard
The guide must:
- use the same labels the user sees in the app;
- explain what to do and what result to expect;
- stay concise and understandable without audio-engineering knowledge;
- avoid internal architecture, implementation jargon and developer-only details;
- mention safety/non-destructive behavior when that helps avoid user mistakes.

## Release gate
Before an RC is promoted, automated/source review must verify that user-visible wording introduced by the change is reflected in the guide when relevant. The residual physical checklist should only test guide readability/ergonomics when that cannot be established digitally; it must not duplicate deterministic behavior already covered by tests.
