# Current State — GuitarLab Studio

Updated: 2026-10-07

## Status

**RC33 RUNTIME ALIGNMENT — IMPLEMENTED / SIGNED CI PENDING / PHYSICAL REQUALIFICATION PENDING**

The current source candidate is `0.5.0-rc33` / versionCode `53`. RC32 / CI #972 remains the last signed digital PASS until this exact source completes the signed workflow.

## Evidence that opened RC33

Owner testing of the exact signed rc32 APK without MK-300 used both:
- MAIN = tablet speaker / CUE = wired headset;
- MAIN = wired headset / CUE = tablet speaker.

The new rc32 diagnostic bundle `GuitarLab-Diagnostics-1791375828493.zip` shows all six new selection-time preflights as `SUPPORTED` (four speaker→wired and two wired→speaker), all through `COMMUNICATION_SPLIT` without global `MODE_IN_COMMUNICATION`. This means rc32 materially stabilized device selection/admission.

The remaining failure moved to runtime. The retained rc32 runtime diagnostic reports `reason=DRIFT`, MAIN `37396` project-rate frames, CUE `41395`, absolute difference `3999` frames (~90.7 ms at 44.1 kHz), limit `2646` frames (60 ms) and GuitarLab queue backlog `0`.

Code audit found that runtime route qualification drained only the silent MAIN warm-up before capturing later playback-head baselines. Silent CUE frames already accepted by its native `AudioTrack` could still be pending outside the GuitarLab FIFO, making residual startup buffering look like clock drift.

## RC33 correction

- Runtime route qualification tracks **actual accepted frame counts independently for MAIN and CUE**.
- Non-blocking short/zero prime/feed writes are accepted and counted; only negative writes are hard write failures.
- Qualification keeps at most a small target amount of silent audio outstanding instead of continuously filling either sink.
- Before musical playback baselines are captured, both sinks must present every warm-up frame that they accepted.
- Communication Split drift is now relative: after both real streams advance at least the baseline arming window, the current CUE−MAIN separation becomes the fixed pipeline baseline.
- Only later movement away from that baseline is drift. The existing 60 ms communication envelope and 750 ms persistence window remain; the implementation does not simply raise thresholds.
- Seek clears the CUE queue/resampler and resets the relative-drift monitor.
- Exact physical route proof remains immediate and fail-closed. Wrong/missing/converged/mirrored routes are never excused as timing.
- Every CUE Play clears stale runtime evidence and starts a fresh diagnostic `sessionId` containing expected MAIN/CUE, strategy and rates.
- Runtime suppression evidence now includes warm-up accepted/presented counts, baseline/current delta, relative drift, queue backlog and actual routes.
- Obsolete rc31 escalation-policy helpers are removed from the active source.
- Zero ducking and mixer-owned content levels remain unchanged.

## Tests added

Focused JVM coverage proves:
- bounded warm-up feed decisions;
- drain is incomplete until all accepted frames are presented;
- a large but fixed pipeline separation becomes the baseline rather than false drift;
- transient relative movement recovers without suppression;
- only sustained relative drift fails;
- seek/reset requires a fresh baseline.

Existing CUE negotiation/resampling, route-safety, startup-probe and non-blocking FIFO tests remain in place. CI source guards require the new alignment implementation and prohibit reintroduction of the obsolete absolute Communication Split runtime timer.

## CI trigger note

The rc33 source commit `3cd4ca3ad75632255fd9b3ce983fa15195eb6bc1` was published through a low-level Git ref update that did not emit the repository's expected Actions `push` event. A documentation-only follow-up commit intentionally carries `[run ci signed]` so GitHub Actions qualifies the unchanged rc33 runtime source through the normal signed pipeline.

## Residual gate

The exact rc33 source must pass signed Android CI (unit tests, Lint/build, API36 regression, exact-artifact signing). After digital PASS, physical retest should begin with tablet speaker + wired headset if MK-300 is unavailable, then repeat with MK-300 MAIN + wired CUE when available. Acceptance remains based on repeated stable routing, long playback, seek/loop, representative Play/REC, zero automatic ducking and acceptable audible alignment/fidelity.

## Accepted baseline and future work

`RELEASE_BASELINE.md` remains authoritative for the accepted RC20 baseline until owner acceptance of an exact signed successor. The USB multichannel MAIN 1/2 + CUE 3/4 roadmap remains active future work under `docs/history/CUE_USB_MULTICHANNEL_IMPLEMENTATION_PLAN_2026-10-06.md`.
