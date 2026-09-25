# M4 Clip Management Checkpoint

This checkpoint introduces a pure, testable clip-edit layer before waveform and transport gestures are enabled.

## Scope
- remove an existing clip from a project;
- mute/unmute an existing clip;
- move an existing clip to a non-negative timeline frame in the core editor;
- persist edits through `FileProjectRepository`;
- expose mute/unmute and remove in Studio;
- keep frame movement UI gated until waveform scale and playhead semantics are stable.

## Gate
- all pure clip editor behavior must pass unit tests;
- project validation must still reject invalid clip state;
- Android Lint and debug APK assembly must pass;
- no physical-audio gate is closed by this checkpoint.
