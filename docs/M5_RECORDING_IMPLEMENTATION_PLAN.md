# M5 Recording Implementation Plan

M5 turns the already-homologated M2 input/duplex path plus the M4 managed-media/timeline/mixer foundation into a real Studio recording workflow.

## Product rule: 5-second start delay
Pressing Record does not begin capture immediately. The Studio runs a deterministic 5-second countdown (`5 → 4 → 3 → 2 → 1`) before opening the recording session. During the countdown structural edits are locked and the user may cancel. At zero the Studio revalidates permission, armed tracks, input route and project state before capture starts.

## M5.A — recording media/core — SOFTWARE GREEN
- streaming 32-bit IEEE-float WAV writer;
- project-owned temporary recording transaction;
- atomic promotion into `media/source/` only after a valid take exists;
- stale `.recording.part.wav` cleanup after interrupted/crashed sessions;
- unit tests for WAV metadata/decoder round-trip and transaction cleanup;
- branch moved to `0.2.0-alpha08` / versionCode 9.

Gate evidence: CI run #220 completed Unit Tests, Android Lint and Debug APK successfully.

## M5.B — Android capture engine — IMPLEMENTED, CI PENDING
- `AudioRecord` with mono-first negotiation and FLOAT32→PCM16 fallback;
- preferred global input from Options; a missing explicitly selected input is a hard failure, not a silent route substitution;
- exact project sample rate when an existing project rate is established; negotiated 48/44.1 kHz and other advertised candidates for an empty/Auto project;
- live input Peak/RMS;
- user Stop, route-loss and read-error handling;
- valid partial take result when frames already exist;
- zero-frame invalid take rejection;
- software monitoring routed to the selected/main output when policy requests it;
- monitoring preference `Desligado / Automático / Ligado` persisted in Options;
- AUTO policy avoids USB double monitoring and Bluetooth live-monitor latency.

See `M5_CAPTURE_ENGINE_CHECKPOINT.md`.

## M5.C — Studio recording coordinator
- Record permission request from Studio;
- at least one armed track required;
- 5-second countdown/cancel then actual `RECORDING` transport state;
- backing playback and capture run concurrently when playable backing exists;
- recording can also run in an empty project;
- captured take is finalized, validated and atomically promoted to project-managed source;
- take is converted into persistent `AudioClip` metadata and waveform cache;
- multiple armed tracks may reference the same immutable captured source, avoiding byte duplication;
- recorded clip begins at the timeline frame at which recording was requested; fine latency compensation belongs to M6;
- Play/Stop acts as Stop while recording;
- loop recording remains gated in M5 to avoid ambiguous linear-take vs looping-playhead semantics.

## M5.D — recovery / duplex / physical gate
Recovery and safety:
- route loss never writes to a different explicitly selected input silently;
- a valid partial take is finalized and may be recovered with an interruption status;
- an empty/invalid partial take is discarded;
- stale crash leftovers are removed safely;
- finalized managed source remains immutable;
- repeated Record/Stop must not leak `AudioRecord`, `AudioTrack`, worker threads or partial files.

## M5 exit gate
Software gate:
- unit tests + Lint + debug APK green;
- capture/coordinator errors are bounded and deterministic;
- recording transaction cannot corrupt an existing project/source;
- 5-second countdown is tested as product policy;
- no ghost clips on failed/empty recording.

Physical gate on Samsung SM-X230 + Pocket Amp:
- permission flow;
- 5-second Record countdown/cancel;
- isolated recording;
- backing + recording duplex;
- input routing and reconnect;
- monitoring OFF/ON/AUTO;
- track input meter/clip latch;
- Stop/finalize/reopen;
- hot unplug during recording with safe partial recovery;
- at least 10 repeated Record/Stop cycles;
- no P0/P1 crash, hang, corrupted WAV, ghost clip or managed-media loss.

M6 remains responsible for measured round-trip latency and final placement compensation.
