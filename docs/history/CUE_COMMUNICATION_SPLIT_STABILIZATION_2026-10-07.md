# CUE Communication Split stabilization — RC32

Updated: 2026-10-07

## Evidence that changed the diagnosis

Physical RC31 testing on Samsung SM-X230 / Android 16 proved that the public communication-device path can produce the target topology:

- MAIN -> MK-300 USB;
- CUE -> wired jack;
- MAIN project/output rate 44.1 kHz;
- CUE physical rate 48 kHz with edge resampling;
- `AudioManager.mode` unchanged on successful qualifications;
- no GuitarLab ducking request.

The diagnostic bundle records several successful `COMMUNICATION_SPLIT` preflights. It also records intermittent `OFFSET_EXCEEDED` failures despite already-correct physical routes. Captured successful attempts use `communicationModeRequired=false`; captured final offset failures use `MODE_IN_COMMUNICATION`. The screen recording additionally proves that real project material reached the intended two destinations before runtime guards disabled CUE.

This changes the problem from "route unsupported" to "route supported but over-constrained by legacy dual-MEDIA timing/backpressure policy."

## RC31 over-constraints

1. The app probed MULTI_DEVICE first, spending up to the route settlement timeout on a topology already known to converge on this device and perturbing AudioPolicy before Communication Split.
2. Candidate B automatically entered `MODE_IN_COMMUNICATION`, a global policy change that produced no successful owner-device qualification in the captured evidence.
3. The 12 ms startup origin bound mixed route safety with static pipeline latency between MEDIA and COMMUNICATION strategies.
4. One partial/zero non-blocking CUE write was treated as fatal even though non-blocking sinks may transiently accept less than the requested chunk.
5. Runtime drift used a ~15.6 ms threshold at 44.1/48 kHz with only three consecutive render iterations before suppression.
6. Communication runtime repeated the complete silent preflight on the freshly opened tracks, then paused/flushed/restarted them before another route qualification, increasing state churn.

## RC32 correction

- prefer `COMMUNICATION_SPLIT` when the selected CUE endpoint is exposed by `getAvailableCommunicationDevices()`;
- retain `MULTI_DEVICE` only as compatibility fallback;
- remove automatic `MODE_IN_COMMUNICATION` escalation;
- retain exact canonical physical-route proof and immediate fail-closed behavior on actual convergence/wrong route;
- keep 12 ms for conventional dual-MEDIA, but allow up to 60 ms static startup offset for Communication Split and export the measured value;
- allow communication drift up to 60 ms only if it does not persist for 750 ms;
- introduce a bounded non-blocking FIFO with a 180 ms maximum backlog so partial/zero writes do not instantly kill CUE and never block MAIN;
- skip duplicate full silent clock preflight for a previously admitted Communication Split profile at runtime; actual playback still performs fresh physical route qualification before musical content proceeds;
- export `lastCueRuntime` with reason, pending samples, presented-frame delta, limit and routed physical keys;
- replace development-facing success/failure text with product language.

## Safety invariants unchanged

- CUE may never be accepted when MAIN/CUE effective physical routes are equal, missing, wrong or mirrored.
- CUE never blocks MAIN.
- Bounded FIFO overflow, negative write, sustained drift or route loss disables CUE.
- No automatic ducking or system-volume manipulation.
- Project/timeline sample rate is unchanged.
- Communication output rate adaptation remains a physical-edge concern only.
- USB multichannel remains the preferred professional architecture when available.

## Requalification target

RC32 must first pass JVM/unit, lint/build, API36 regression and signed homologation gates. Physical acceptance then requires repeated MK-300 + wired runs, long playback, seek/loop and disconnect/reconnect without false CUE suppression. The diagnostic bundle must show stable distinct routes and no unexpected backlog/drift shutdown.
