# Studio Options, Export and Mixer Contract

Updated: 2026-10-01

## Options
Options owns low-frequency setup and diagnostics: input route, main output, secondary/CUE output, monitoring, Studio preferences, import capability information and diagnostic tools. These controls stay out of the timeline.

MAIN may remain Android-selected for ordinary single-output work. CUE is explicit-only: it can be selected only when MAIN is explicit, must resolve to a different live endpoint and has no automatic fallback. Synchronized CUE intentionally excludes Bluetooth because wireless presentation latency is too large/variable for the MAIN↔CUE alignment contract; use a wired/USB/other low-latency secondary output. Selection alone does not activate CUE: the playback engine first confirms distinct routes plus stable presentation clocks and admits CUE only when the initial MAIN↔CUE stream-origin offset is at most 12 ms. If that proof fails, or the live drift later exceeds the bounded envelope, the CUE bus is silenced while MAIN continues whenever safe.

Project persistence and final-audio export are **not** Options actions.

## Studio global actions
Studio exposes Mixer, Help, Options, Export and Home using the shared project-shell navigation language. Project-level navigation remains in the shared shell. Studio transport has a single fixed-slot navbar: Comparison, Adjustments and Timeline open anchored overlays; transport commands stay centered; Mixer Pin and Height retain reserved slots. Enabled/selected/mixer state never adds, removes, resizes or wraps these controls. Narrow windows scroll the same row horizontally.

`Ajuda` opens the same shared `StudioUserGuideDialog` implementation used by the Home entry point; there is no second guide implementation/copy.

## Project persistence and Export
Portable `.guitarlab` save remains semantically distinct from external Study Export and Studio Master delivery. The canonical Export workspace owns output/format selection; Studio/Home entry points navigate there and do not maintain a second modal chooser. Sample rate follows established project/render policy without unnecessary codec knobs.

## Mixer
Mixer is a bottom dock with horizontally scrollable track strips and a structurally separate fixed Master. Visibility, pinning and height are independent durable workspace preferences. Existing `mixer_visible` and legacy visibility fallback remain compatible; new pinning defaults to fixed/docked. A pinned Mixer reserves layout space; floating is explicit and overlays the bottom of the workspace.

The owner requested **lateral density**, not primarily a shorter dock. At default font scale track strips are 168 dp (previously 232 dp), with Mute/Solo and CUE/REC in a 2×2 grid of real 48×48 dp targets. PK, RMS, peak hold, numeric readings, CLIP reset, gain and bipolar pan remain visible. Gain/pan labels sit above full-width sliders with 48 dp gesture regions. CLIP has a permanently reserved 48 dp column, so latching it does not displace meter geometry. Long track names have a fixed two-line header. Master is 156 dp and never covers the scrolling strips. Font scaling increases channel width and dock height instead of shrinking targets.

Default dock height is 352 dp and optional expanded height is 400 dp at normal font scale. The old embedded practice header is removed; all three action families now live in navbar overlays regardless of Mixer visibility. Height follows the space needed to preserve the narrower complete channels; it is not the primary density metric. Gain/pan may preview live and are persisted on completed gestures. Pan is bipolar. Mute/Solo/CUE/REC Arm remain distinct. REC Arm is the persisted recording-target state used by the recording coordinator.

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
