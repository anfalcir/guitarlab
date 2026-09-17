# Implementation Roadmap

Updated: 2026-09-17

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; final physical latency closure still active.
- **M8 — Release hardening:** H28 signed DIGITAL PASS at CI #653; target backup corrective physically accepted; recording-latency residual remains.

## Current signed authority
**CI #653** / run `35207902169` / producer `d09fc003e2ae2d699238eb39ba699f75a746fea4` is the current signed DIGITAL PASS through H28.

Signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`.

H28 backup/provider-consistency behavior has subsequently passed the user's target-device functional check and is now a protected regression contract.

## Current release closure
The remaining RC3 blocker is physical recording latency/synchronization acceptance on the intended real USB route. All further hardening must preserve the H28 backup fix and the digitally approved recording/editing baseline.

## Approved post-H28 implementation path
Detailed authority: `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

### H29 — Recording synchronization closure and session health
- preserve H23b measured timing architecture;
- add bounded per-session timing/route health evidence and diagnostics;
- close physical latency/alignment on exact signed candidate.

### H30 — Interrupted recording recovery + USB resilience
- user-facing recovery of preserved `.recording.part.wav` payloads;
- idempotent transactional recovery/discard semantics;
- robust USB loss/reconnect state machine;
- never silently fall back to tablet microphone;
- preserve valid captured audio on route loss.

### H31 — Takes management + diagnostics refinement
- activate, rename, delete, note, favorite and rapid audition for takes;
- preserve active-take and shared-media invariants;
- extend the existing audio diagnostics/support report with recording-session health and sanitized failure context.

### H32 — 10-minute song-quality gate + backup regression + final UX/accessibility
- release-quality target is representative songs/sessions up to **10 minutes**;
- no separate artificial mega-project performance program;
- long-recording waveform/timing/save/export quality through 10 minutes;
- H28 backup/restore idempotency/retention/provider-failure regression;
- final TalkBack/touch-target/font-scale/error/loading/help polish;
- exact signed final hardening gate.

### 1.0.0 — Stable release
Requires H29-H32 complete, exact-source digital PASS, target recording/USB/10-minute physical residual PASS, no repeatable P0/P1 and explicit approval of the exact signed APK.

### H33 / 1.1 — External MIDI/footswitch control
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
Current authority: `… → H25 → H26 → H26a → H26b → H26e → H27 → H28`.

Future implementation blocks must extend this chain deterministically, verify final hashes, be idempotent and fail closed on unexplained drift.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary source/docs commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction.
