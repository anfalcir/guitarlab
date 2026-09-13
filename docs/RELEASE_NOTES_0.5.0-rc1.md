# GuitarLab Studio 0.5.0-rc1

This candidate consolidates the requested practice workflow and the first real MK-300 recording corrections.

## Added
- live bounded waveform during recording;
- take management with one active take per track;
- Reference, My Guitar and Both comparison modes;
- markers, sections, automatic suggestions and section loop;
- loop-based punch recording with pre/post-roll;
- RMS/peak analysis and conservative gain suggestion.

## Corrected and hardened
- explicit USB input is accepted only after Android confirms the effective route;
- selected-input loss stops capture instead of falling back silently;
- backing/playback has no software path into the recording writer;
- monitoring wording makes clear it affects return only, never recorded content;
- `|<` works while playing;
- Play/Stop is disabled during recording, where REC is the stop control;
- playback stop flushes and joins the previous session to avoid a stuck Play button;
- take references remain valid through remove, move, split, stereo separation and track deletion.

## Digital evidence
- JVM tests: PASS
- Android Lint: PASS
- debug APK: PASS
- Android test APK compilation: PASS
- signed release assembly: PASS
- APK v2 signature and certificate: PASS
- ZIP integrity and package identity: PASS

The exact RC1 Android instrumented suite was not executed because the local host has no KVM and automatic Actions are disabled. RC2 remains the latest full API 36 + 1920×1200 emulator baseline. Physical MK-300/Samsung checks are intentionally listed in the active checklist.
