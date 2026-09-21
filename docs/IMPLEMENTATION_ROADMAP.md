# Implementation Roadmap

Updated: 2026-09-20

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; final physical latency closure still active.
- **M8 — Release hardening:** H29-H36c signed DIGITAL PASS at CI #663; H37b native Drive v3 backup transport is SOURCE PRE-GATE; the H28 SAF corrective remains physically accepted as the signed fallback baseline; hardware residual remains.

## Current signed authority
**CI #663** / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d` is the current signed DIGITAL PASS through H36c.

Signed APK SHA-256: `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

H28 backup/provider-consistency behavior has subsequently passed the user's target-device functional check and is now a protected regression contract.

## Current release closure
The signed RC3 baseline still has a physical recording latency/synchronization residual on the intended real USB route. H37b carries the separate source-candidate gate for the new backup transport: it must pass exact-source software/API36/signing CI and first real Google OAuth/Drive backup/restore acceptance before RC4 can supersede the signed RC3 candidate. All further hardening must preserve the H28 project/revision identity and the digitally approved recording/editing baseline.

## Approved post-H28 implementation path
Detailed authority: `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

H29-H36c are DIGITAL PASS at CI #663. H34-H36c are part of the signed exact-source baseline.

### H29 — Recording synchronization closure and session health
**Status: DIGITAL PASS at #663; target-device timing acceptance pending.**
- preserve H23b measured timing architecture;
- add bounded per-session timing/route health evidence and diagnostics;
- close physical latency/alignment on exact signed candidate.

### H30 — Interrupted recording recovery + USB resilience
**Status: DIGITAL PASS at #663; target USB interruption/reconnect acceptance pending.**
- user-facing recovery of preserved `.recording.part.wav` payloads;
- idempotent transactional recovery/discard semantics;
- robust USB loss/reconnect state machine;
- never silently fall back to tablet microphone;
- preserve valid captured audio on route loss.

### H31 — Takes management + diagnostics refinement
**Status: DIGITAL PASS at #663.**
- activate, rename, delete, note, favorite and rapid audition for takes;
- preserve active-take and shared-media invariants;
- extend the existing audio diagnostics/support report with recording-session health and sanitized failure context.

### H32 — 10-minute song-quality gate + backup regression + final UX/accessibility
**Status: DIGITAL PASS at #663; 10-minute target-device quality smoke pending.**
- release-quality target is representative songs/sessions up to **10 minutes**;
- no separate artificial mega-project performance program;
- long-recording waveform/timing/save/export quality through 10 minutes;
- H28 backup/restore idempotency/retention/provider-failure regression;
- final TalkBack/touch-target/font-scale/error/loading/help polish;
- exact signed final hardening gate.

### H34/H35/H35a — Latency-control refinement
**Status: DIGITAL PASS at #663; target-device behavior smoke pending.**
- global manual residual calibration remains route/rate-scoped, default zero and applies only to future takes;
- per-take synchronization is an explicit persistent edit in the take UI, independent from the current global value;
- take lineage movement is delta-based, non-destructive, Undo/Redo-aware and fails closed at timeline zero;
- silent digital verification validates exact routes and stable clocks with PCM zero but never stores a fake physical latency;
- physical round-trip calibration confirms exact live route IDs before emitting a short windowed adaptive chirp capped at 12% peak;
- no migration heuristics are required because no retained user takes depend on the pre-H35 take schema.

### H36/H36a/H36b/H36c — Settings UX Polish
**Status: DIGITAL PASS at #663.**
- reduce visual density in the main Settings screen, especially large full-width action buttons and nested card chrome;
- center/cap wide-tablet Settings content while remaining responsive on narrow screens;
- standardize compact secondary actions and preserve ≥48 dp touch targets;
- make external-control and diagnostics configuration easier to scan;
- replace calibration fine-adjustment horizontal scrolling with a fixed visible 2×3 adjustment grid plus reset;
- preserve all H35a behavior, semantics/test tags and calibration/routing contracts;
- #660/#661/#662 are retained as corrective evidence; #663 passes the complete exact-source software/API36/geometry/signing gate with 34/34 standard instrumentation.

### H37/H37a/H37b — Native Google Drive API v3 backup transport
**Status: H37b SOURCE PRE-GATE (`0.5.0-rc4` / versionCode `24`); H37 CI #664 failed at compile, H37a corrected compile, H37b hardens 401 token-cache recovery.**
- replace SAF as the primary backup transport with direct client-side Drive API v3 and OAuth `drive.file`;
- preserve H28 immutable `projectId`, deterministic `revisionId`, package SHA-256, deduplication, bounded history and restore-as-copy semantics;
- use resumable uploads with persisted session recovery, server-confirmed offsets, bounded retry/backoff and no blind byte replay;
- require Drive-reported size + SHA-256 before promoting a remote revision to committed;
- keep incomplete uploads out of the restore catalog and reconcile a completed-but-uncommitted upload without re-upload;
- keep Firebase/Cloud Run/Functions and long-lived backend credentials out of the backup data path;
- preserve legacy H26-H28 SAF history as a one-time migration source, never deleting remote legacy content and only releasing SAF access after full migration success;
- exclude Drive connection/resumable-session state from Android cloud backup/device transfer;
- H37a fixes the CI #664 Kotlin compile blockers without changing Drive semantics;
- H37b clears rejected 401 access tokens from the Google Identity Services cache before bounded reauthorization;
- require a fresh canonical manual CI pass and target OAuth/Drive acceptance before promotion.

### 1.0.0 — Stable release
Requires H29-H32 complete, exact-source digital PASS, target recording/USB/10-minute physical residual PASS, no repeatable P0/P1 and explicit approval of the exact signed APK.

### H33 / 1.1 — External MIDI/footswitch control
**Status: DIGITAL PASS at #663 and disabled by default; real-controller acceptance remains a separate 1.1 gate.** The source was staged early to share the digital integration gate, without changing the 1.0 H29-H32 acceptance contract.
Within the H29-H37/1.1 closure line, the approved added feature family is external control:
- Android MIDI (USB/Bluetooth when exposed by the platform) plus opt-in HID/keyboard-style footswitch mapping;
- Learn mode and persistent mappings;
- Play/Stop, REC, Return to start, Loop, Undo and Redo through existing guarded commands;
- debounce/Note On-Off normalization, reconnect safety and no coupling to audio-device selection;
- real-controller physical acceptance.

## Approved successor program — Unified GuitarLab + GBW

A separate successor program is now approved to absorb GBW Android into GuitarLab as one product with a unified project/asset model, Prepare → Studio zero-copy workflow, shared cloud separation, unified Drive backup, legacy migration, exhaustive digital hardening and a consolidated final physical campaign.

Authoritative plan: `UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`.

Current successor status: U0 PASS; U1 unified domain implemented as SOURCE PRE-GATE with the canonical software gate pending.

This successor program uses U0-U12 milestone numbering and is intentionally separate from the H29-H37/1.1 closure numbering. It does not retroactively alter the signed #663 evidence, H37b pre-gate state or historical 1.0/1.1 acceptance rules. Known physical residuals may be carried into the final unified-candidate campaign rather than forcing duplicate manual homologation, provided digital prerequisites remain objectively green.

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
Current source chain: `… → H25 → H26 → H26a → H26b → H26e → H27 → H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35 → H35a → H36 → H36a → H36b → H36c → H37 → H37a → H37b → U1`.

H28 is frozen in `materialize_ci_sources_through_h28.sh`; the existing H29-H36c tail remains independently verified by `materialize_ci_sources_h29_h33.sh`; H37 is layered by `materialize_ci_sources_h37.sh`; H37a is layered by `materialize_ci_sources_h37a.sh`; H37b is layered by `materialize_ci_sources_h37b.sh`; U1 is layered by `materialize_ci_sources_u1.sh`, which verifies its patch SHA-256 and terminal Git blobs. Future blocks must extend this chain deterministically and preserve the same fail-closed guarantees.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary source/docs commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction.
