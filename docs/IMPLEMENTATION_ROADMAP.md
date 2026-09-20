# Implementation Roadmap

Updated: 2026-09-20

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; final physical latency closure still active.
- **M8 — Release hardening:** H29-H33/H33a/H33b signed DIGITAL PASS at CI #657; target backup corrective physically accepted; hardware residual remains.

## Current signed authority
**CI #657** / run `35290128876` / producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6` is the current signed DIGITAL PASS through H33b.

Signed APK SHA-256: `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.

H28 backup/provider-consistency behavior has subsequently passed the user's target-device functional check and is now a protected regression contract.

## Current release closure
The remaining RC3 blocker is physical recording latency/synchronization acceptance on the intended real USB route. All further hardening must preserve the H28 backup fix and the digitally approved recording/editing baseline.

## Approved post-H28 implementation path
Detailed authority: `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

H29-H33/H33a/H33b are DIGITAL PASS at CI #657. **H34/H35 are SOURCE PRE-GATE READY**: H34 expands the global future-recording residual adjustment to ±500 ms; H35 adds independent take-specific synchronization plus silent digital verification and quieter fail-closed physical calibration.

### H29 — Recording synchronization closure and session health
**Status: DIGITAL PASS at #657; target-device timing acceptance pending. H34/H35 source delta PRE-GATE.**
- preserve H23b measured timing architecture;
- add bounded per-session timing/route health evidence and diagnostics;
- close physical latency/alignment on exact signed candidate.

### H30 — Interrupted recording recovery + USB resilience
**Status: DIGITAL PASS at #657; target USB interruption/reconnect acceptance pending.**
- user-facing recovery of preserved `.recording.part.wav` payloads;
- idempotent transactional recovery/discard semantics;
- robust USB loss/reconnect state machine;
- never silently fall back to tablet microphone;
- preserve valid captured audio on route loss.

### H31 — Takes management + diagnostics refinement
**Status: DIGITAL PASS at #657.**
- activate, rename, delete, note, favorite and rapid audition for takes;
- preserve active-take and shared-media invariants;
- extend the existing audio diagnostics/support report with recording-session health and sanitized failure context.

### H32 — 10-minute song-quality gate + backup regression + final UX/accessibility
**Status: DIGITAL PASS at #657; 10-minute target-device quality smoke pending.**
- release-quality target is representative songs/sessions up to **10 minutes**;
- no separate artificial mega-project performance program;
- long-recording waveform/timing/save/export quality through 10 minutes;
- H28 backup/restore idempotency/retention/provider-failure regression;
- final TalkBack/touch-target/font-scale/error/loading/help polish;
- exact signed final hardening gate.

### H34/H35 — Latency-control refinement
**Status: SOURCE PRE-GATE READY.**
- global manual residual calibration remains route/rate-scoped, default zero and applies only to future takes;
- per-take synchronization is an explicit persistent edit in the take UI, independent from the current global value;
- take lineage movement is delta-based, non-destructive, Undo/Redo-aware and fails closed at timeline zero;
- silent digital verification validates exact routes and stable clocks with PCM zero but never stores a fake physical latency;
- physical round-trip calibration confirms exact live route IDs before emitting a short windowed adaptive chirp capped at 12% peak;
- no migration heuristics are required because no retained user takes depend on the pre-H35 take schema.

### 1.0.0 — Stable release
Requires H29-H32 complete, exact-source digital PASS, target recording/USB/10-minute physical residual PASS, no repeatable P0/P1 and explicit approval of the exact signed APK.

### H33 / 1.1 — External MIDI/footswitch control
**Status: DIGITAL PASS at #657 and disabled by default; real-controller acceptance remains a separate 1.1 gate.** The source was staged early to share the digital integration gate, without changing the 1.0 H29-H32 acceptance contract.
Only approved new feature family after stable hardening:
- Android MIDI (USB/Bluetooth when exposed by the platform) plus opt-in HID/keyboard-style footswitch mapping;
- Learn mode and persistent mappings;
- Play/Stop, REC, Return to start, Loop, Undo and Redo through existing guarded commands;
- debounce/Note On-Off normalization, reconnect safety and no coupling to audio-device selection;
- real-controller physical acceptance.

## Explicit roadmap exclusions
Do not add to this line:
- marker/section enhancements;
- clip-gain UI;
- Reference × My Guitar comparison enhancements;
- separate large-project performance scope beyond the 10-minute song-quality target;
- any other new feature family before/alongside H33;
- tuner;
- per-track independent physical output routing.

## Canonical materialization tail
Current source chain: `… → H25 → H26 → H26a → H26b → H26e → H27 → H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35`.

H28 is frozen in `materialize_ci_sources_through_h28.sh`; the post-H28 tail through H35 is independently hash-verified, idempotent and fail-closed. Future implementation blocks must extend this chain deterministically and preserve the same guarantees.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary source/docs commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction.
