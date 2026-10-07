# CUE runtime alignment — RC33

Updated: 2026-10-07
Status: **IMPLEMENTED / SIGNED CI PENDING / PHYSICAL REQUALIFICATION PENDING**

## Trigger evidence

The signed rc32 tablet-speaker/wired-headset campaign moved the problem from selection-time admission to runtime. Six new preflights in `GuitarLab-Diagnostics-1791375828493.zip` are `SUPPORTED` in both route orientations. The retained runtime failure reports an absolute 3999-frame (~90.7 ms at 44.1 kHz) MAIN↔CUE difference with zero GuitarLab FIFO backlog.

## Root cause

`qualifyRuntimeRoutes()` accumulated silent warm-up frames for both tracks but drained only MAIN before returning. CUE could therefore still own accepted silence inside its native AudioTrack when runtime playback-head bases were captured. The subsequent guard compared absolute progress from two bases with different residual pipeline state and labeled that difference as drift.

This is distinct from actual long-run clock drift and from GuitarLab FIFO backlog.

## Implementation

1. Account actual non-blocking prime/feed transfer counts independently for MAIN and CUE.
2. Bound route-qualification outstanding audio rather than continuously filling buffers.
3. Require both sinks to present all accepted warm-up frames before qualification returns.
4. Recheck exact physical routes after the drain.
5. Arm Communication Split drift only after both real streams have presented enough frames.
6. Store the stabilized `cuePresented-mainPresented` delta as baseline.
7. Supervise subsequent **change from that baseline** using the existing 60 ms / 750 ms communication policy.
8. Reset baseline on seek.
9. Start a fresh runtime diagnostic session for every CUE Play and clear the previous one.
10. Export session-bound expected routes, strategy/rates, warm-up counts, baseline/current delta, relative drift, queue state and actual route sets on suppression.
11. Remove dead rc31 escalation-policy helpers.

## Safety retained

- physical route identity is still exact and fail-closed;
- CUE never blocks MAIN;
- negative writes, bounded FIFO overflow, route loss and sustained true relative drift still disable CUE;
- no automatic ducking or system-volume balancing;
- project/timeline rate remains authoritative;
- Communication Split rate adaptation remains at the CUE physical edge.

## Qualification

Focused unit tests cover warm-up/drain and relative-drift semantics. Signed Android CI remains the digital authority; physical owner testing remains required because Android/OEM route behavior and acoustic usefulness cannot be proven digitally.
