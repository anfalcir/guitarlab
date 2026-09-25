# Studio Options, Export and Mixer Contract

Updated: 2026-09-24

## Options
Options owns low-frequency setup and diagnostics: input route, main output, monitoring, Studio preferences, import capability information and diagnostic tools. These controls stay out of the timeline.

Project persistence and final-audio export are **not** Options actions.

## Studio global actions
Studio exposes Mixer, Help, Options, Export and Home using the shared project-shell navigation language. Exact placement follows the responsive shell rather than a frozen historical top-bar order.

`Ajuda` opens the same shared `StudioUserGuideDialog` implementation used by the Home entry point; there is no second guide implementation/copy.

## Project persistence and Export
Portable `.guitarlab` save remains semantically distinct from external Study Export and Studio Master delivery. The canonical Export workspace owns output/format selection; Studio/Home entry points navigate there and do not maintain a second modal chooser. Sample rate follows established project/render policy without unnecessary codec knobs.

## Mixer
Mixer is a bottom dock with horizontally scrollable track strips and fixed Master. Gain/pan may preview live and are persisted on completed gestures. Pan is bipolar. Mute/Solo/REC Arm remain distinct. REC Arm is the persisted recording-target state used by the recording coordinator.

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
Repeat focused Mixer/Options/Export regression when a candidate changes those surfaces or their domain commands. They do not create a global RC20 gate for unrelated worker/search changes.
