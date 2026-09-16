# In-app User Guide Synchronization Policy

Updated: 2026-09-16

## Contract
GuitarLab exposes one shared `StudioUserGuideDialog` reachable from Home and Studio. It is a product surface, not optional documentation.

Whenever a development block changes a user-visible workflow, the same block must update the shared guide when the behavior is not self-evident. Source implementation, accessibility semantics, tests and guide wording must describe the same behavior before the block is considered complete.

## Current mandatory guide coverage
The shared guide must remain synchronized with:
- Home project creation/open/import/export actions;
- **H24 Home project-library search, filters and sorting**;
- the fact that project search ignores case and accents;
- the fact that Home filters are combinable and non-destructive;
- the fact that sorting changes presentation only and does not mutate project content;
- project rename/duplicate/delete where exposed;
- Studio navigation/transport/loop behavior;
- track controls and Mixer basics;
- Trim/CUT interaction and clip deletion scope;
- recording countdown, route selection, waveform/meters and stop behavior;
- practice/sections workflow;
- Save/Export distinctions where user decisions matter.

## H24 synchronization rule
H24 is not complete if Home gains search/filter/sort controls but `StudioUserGuideDialog` omits them. The H24 source patch therefore updates the shared guide in the same materialized block.

The guide does not need to expose internal implementation terms such as `ProjectLibraryIndex`, normalized cache keys, Git blob hashes or CI mechanics. Those belong in engineering documentation, not creative-flow help.

## Copy principles
- Use concise Brazilian Portuguese user-facing language.
- Describe what the user can do and what state changes, not internal class names.
- Avoid low-level Android route identifiers and diagnostic terminology in normal help.
- Do not advertise unimplemented features.
- Keep destructive actions and their scope explicit.
- Prefer one source of truth over separate Home/Studio help implementations.

## Verification
Any user-visible feature block that changes the guide should retain source-level review plus Android semantic/UI coverage where practical. Final target-device review checks readability/touch ergonomics only when those qualities cannot be established reliably in automation.
