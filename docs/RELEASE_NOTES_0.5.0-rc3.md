# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

RC3 consolidates the final practice, transport, editing, recording and release-hardening work.

## Digitally established through H10
CI #617 / run ID `34918430241` digitally homologated H0–H10 against source `abc0e2a9f8708dd141735915898b508ce0948f48`. Signed APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.

Established behavior includes loop-aware Play/seek, Auto seções preview, REC countdown, shared guide, independent trim handles, safe split/take lineage, clip deletion/drag-to-trash, measured recording startup synchronization, level-analysis convergence, Stop-during-REC, history/race hardening and stable long live waveform bucketing.

## Physical Review III — H11
After the #617 physical pass, the remaining requested refinements were consolidated into one bounded interaction/metering delta.

### Cleaner segmented Mixer bar
- removes redundant `Mixer` title from the dock header;
- removes Pin and in-dock X controls;
- the top-bar Mixer button is now the sole persistent visibility toggle: one tap opens, the next closes;
- visibility survives project/app return until explicitly toggled;
- the dock header becomes two equal horizontal segments, **Comparação** and **Timeline**;
- buttons use restrained, near-square 4–6 dp corner rounding instead of pill-like styling.

### Waveform selects its track
- tapping a track waveform/audio lane selects that track through the same canonical selection state as sidebar/Mixer;
- clip cards and the active live-recording waveform follow the same rule;
- semantic/test tags support deterministic regression.

### Real-time recording Peak/RMS
- active capture Peak/RMS is projected into the recording target's existing track-meter state;
- Mixer PK/RMS therefore responds live during REC;
- the recording waveform also shows a compact live `PK` / `RMS` overlay;
- unrelated tracks do not receive the raw capture meter;
- meter state resets on start/finalize/error to prevent stale carry-over.

### H11 validation before canonical CI
- pure `RecordingTrackMeterPolicy` harness — PASS;
- exact #617 materialized-source forward/reverse H11 patch validation — PASS;
- final application tree reproduction — PASS;
- `git diff --check` — PASS;
- added tests for equal segmented-bar widths, waveform selection, Mixer visibility persistence and recording-meter projection.

## Validation status
- H0–H10: DIGITAL PASS in CI #617.
- H11: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.
- A new manually dispatched exact-source workflow is required before the H11 APK is promoted to final physical homologation.
