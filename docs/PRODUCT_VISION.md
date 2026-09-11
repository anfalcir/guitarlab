# Product Vision

## Mission
GuitarLab Studio is an Android-first guitar practice, recording, comparison and mixing workspace. The target product is a compact, dependable guitar-focused DAW that can move from study to recording without forcing the user into desktop workflows.

## Final-state product
The planned final product must support:
- project creation from Blank or Guitar templates;
- flexible track/group organization and built-in/custom roles;
- non-destructive audio clips on a timeline;
- import from multiple audio formats and sample rates;
- waveform visualization, trim, move, split and clip-level gain/mute;
- transport, seeking, looping and reliable playback;
- low-latency guitar capture with monitoring and robust Android/USB routing;
- multi-track recording, including double-tracked guitar workflows;
- track controls such as arm, mute, solo, gain and pan;
- mixing and export in multiple useful formats/qualities;
- project persistence, safe reopening and compatibility across app upgrades;
- practical Android UX for tablet use, with a clean modern visual system;
- diagnostics and homologation tooling that remain available without leaking into the normal creative workflow.

## Product principles
1. Guitar-first, not generic-DAW-first.
2. Non-destructive editing by default.
3. Reliable before feature-rich: every major capability passes software and device gates.
4. Flexible audio I/O and file interoperability.
5. Explicit capability status: planned features are never advertised as supported before validation.
6. Offline-capable core workflows.
7. Project files store structure/metadata; large audio assets remain external or app-managed references.
8. Modern, readable tablet UI with light/dark themes and restrained accent colors.

## Scope guardrail
WAV is the first physically validated codec path, not the final interoperability scope. Multi-format import/export remains part of the planned product and must not be removed merely because early milestones use WAV as the vertical slice.

## Canonical documentation
This file defines the destination. `PRODUCT_REQUIREMENTS.md` defines required behavior, `IMPLEMENTATION_ROADMAP.md` defines delivery order, `CURRENT_STATE.md` records where development currently stands, and `CODEC_SUPPORT_MATRIX.md` prevents temporary codec limitations from becoming accidental product scope.
