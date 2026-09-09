# Studio Options and Mixer contract

Updated: 2026-09-09

## Options
Global Studio options own input route, main output, monitoring, export-related configuration and diagnostics. These controls stay out of the primary timeline to reduce clutter.

## Mixer
Mixer is a bottom dock with horizontally scrollable track strips and a fixed Master. Gain/pan changes may preview live and are persisted on completed gestures. Pan is bipolar. Mute/Solo/REC Arm remain distinct symbolic controls. REC Arm is no longer merely future M5 preparation: it is the persisted recording-target state used by the implemented M5 recording coordinator.

## Timeline/track UI
- track names: canonical 1–24 chars;
- `Limpar pista`: content action in the sidebar three-dot menu; removes clips only and preserves track identity/function/color/order/mix;
- `Excluir pista`: structural action in `Configurar pista`; only enabled for an empty track and explains that content must be cleared first;
- source metadata lives in `Configurar pista` beside the palette on wide layouts and below it on narrow layouts;
- ordering is drag-only; legacy arrow ordering is removed.

## Active gate
The Mixer/Options architecture is not pending M4 work. It is established behavior and a regression surface while alpha11 closes M5.
