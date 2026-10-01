# Studio Options, Export and Mixer Contract

Updated: 2026-10-01

## Options
Options owns low-frequency setup and diagnostics: input route, main output, secondary/CUE output, monitoring, Studio preferences, import capability information and diagnostic tools. These controls stay out of the timeline.

MAIN may remain Android-selected for ordinary single-output work. CUE is explicit-only: it can be selected only when MAIN is explicit, must resolve to a different live endpoint and has no automatic fallback. Synchronized CUE intentionally excludes Bluetooth because wireless presentation latency is too large/variable for the MAIN↔CUE alignment contract; use a wired/USB/other low-latency secondary output. Selection alone does not activate CUE: the playback engine first confirms distinct routes plus stable presentation clocks and admits CUE only when the initial MAIN↔CUE stream-origin offset is at most 12 ms. If that proof fails, or the live drift later exceeds the bounded envelope, the CUE bus is silenced while MAIN continues whenever safe.

Project persistence and final-audio export are **not** Options actions.

## Studio global actions
Studio exposes Mixer, Help, Options, Export and Home using the shared project-shell navigation language. Comparison and Timeline use fixed-size transport-navbar triggers. Equal reserved left/right groups keep transport centered on the viewport. A single fixed mode slot switches Mixer minimum/complete; it is disabled, not removed, when hidden. Menus are anchored vertical lists with their own bounded width (Comparison 240 dp, Timeline 320 dp), not reused scrolling toolbars. Comparison stays open for repeated A/B choices. Timeline keeps creation and destructive cleanup separate and explains the missing-loop prerequisite. Níveis belongs to the fixed Master. No redundant Adjustments or Pin trigger remains. Narrow windows scroll the same single navbar row.

`Ajuda` opens the same shared `StudioUserGuideDialog` implementation used by the Home entry point; there is no second guide implementation/copy.

## Project persistence and Export
Portable `.guitarlab` save remains semantically distinct from external Study Export and Studio Master delivery. The canonical Export workspace owns output/format selection; Studio/Home entry points navigate there and do not maintain a second modal chooser. Sample rate follows established project/render policy without unnecessary codec knobs.

## Mixer
Mixer always reserves its own bottom layout space: visibility and minimum/complete mode are durable, independent preferences. Default and upgrades start complete. Legacy `mixer_visible`/`mixer_pinned` visibility is retained; RC23 floating/expanded flags are not mapped to minimum because both old heights exposed complete controls. A mode write retires those obsolete flags.

At normal font scale channels are 200 dp (original 232 dp), with all four Mute/Solo/CUE/REC targets contiguous in one centered row: 48×48 dp touch regions and 46×46 dp visible faces. Channel padding is 4 dp per side. Master is a separate fixed 144 dp column; channels scroll horizontally without covering it. Font scaling grows channel width and header/meter height rather than overlapping touch regions.

Complete height is 252 dp, down from RC23's 352 dp (28.4%). All four state controls, PK/RMS/numeric readings/peak hold/CLIP reset, gain and bipolar pan are exposed. Gain and pan each use a single 48 dp row combining labels/readout with a thin Material Slider drawing; gesture, keyboard and accessibility behavior remains Material-owned. CLIP retains a reserved target so latching does not move meters.

Minimum height is 172 dp. It retains all four direct state controls, gain/readout, a compact peak/hold meter with accessible PK/RMS readings, pan value and clipping indication. Tapping the track name opens the same complete strip implementation in a details dialog, exposing pan and CLIP reset with the same domain callbacks/transport locks. Master retains Níveis, gain and compact metering; clipping is reset from its header. Mode changes never issue audio commands. Gains/pans preview live and commit on gesture completion; no separate mix model exists for minimum/details.

Each track persists an output-route value with backward-compatible default `MAIN`. The engine contract supports `MAIN`, `CUE` and `MAIN_AND_CUE`; the first user-facing headphone control intentionally toggles exclusive MAIN ↔ CUE. Route changes are accepted only while the transport is stopped, so Play/REC cannot leave track metadata and live-monitor routing out of sync. The route is restored with the project, applied to normal playback and backing playback during recording, and does not destructively change clips/takes.

CUE is a secondary monitoring/playback bus. The secondary sink is opened only when currently playable content belongs to a CUE-routed track; an empty CUE track does not create an unnecessary output stream. Its writer is non-blocking relative to MAIN: if the secondary sink cannot accept a complete render chunk, CUE is silenced rather than allowed to stall the primary output. CUE is not an export exclusion flag. Studio Master export continues to render the project timeline/mix semantics independently of the live physical CUE assignment.

## Timeline/track UI
- track names: canonical 1–24 chars;
- `Limpar pista`: track-content action; removes clips but preserves track identity/function/color/order/mix;
- `Excluir clipe`: clip-segment action; removes only the selected clip after confirmation and preserves valid sibling take/media references;
- drag-to-trash is a direct-manipulation shortcut into the same confirmed clip-deletion domain command;
- `Excluir pista`: structural action inside `Configurar pista`; safe/disabled as required for populated tracks;
- source metadata lives in `Configurar pista`, responsive to available width;
- a populated track uses a clear edit affordance for its content/configuration path;
- track ordering remains drag-only through the workspace coordinator;
- clip migration/deletion resolves through transaction-safe drag intent (`Move`, `Delete`, `NoOp`).

## Qualification
Repeat focused Mixer/Options/Export regression when a candidate changes those surfaces or their domain commands. They do not create a global release or maintenance gate for unrelated worker/search changes.
