# Implementation Roadmap

Updated: 2026-09-24

> **Current unified-line authority (2026-09-24):** historical H29-H37 remains architecture/evidence context. U10/C8 and U11 are CLOSED / DIGITAL PASS. RC14 is the latest signed U12 authority at `13c6f36e5e3c2bcf82cf21f885e8d7aa6c41ed34` / Android `35937621047`; RC16 is the active physical corrective and remains pending exact-source Android/U7, production worker deployment/U4 and target-device acceptance.

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; final physical latency closure still active.
- **M8 — Release hardening / unified successor:** historical H29-H37 evidence is absorbed; unified U10/C8 is digitally closed and U11/U12 own final release freeze + physical residual.

## Current signed authority

**Android CI run `35937621047`** / producer `13c6f36e5e3c2bcf82cf21f885e8d7aa6c41ed34` is the latest signed DIGITAL PASS (`0.5.0-rc14` / versionCode `34`). RC16 (`0.5.0-rc16` / 36) is the active source candidate pending exact-source Android/U7, production worker deployment/U4 and physical audio/synchronization acceptance.

Signed APK SHA-256: `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`.

The same producer passed U4 Cloud Integration Smoke #123 and U7 Cloud Backend #81. The U7 gate proves Email/Password Firebase authentication is enabled, anonymous auth is disabled, allowlisted users are backed by the password provider and backend source/container/security verification passes. The locked signer remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Current release closure

U10/C8 and U11 are digitally closed. U12 is active on RC16; RC14 remains the latest signed authority until RC16 qualification. RC9–RC15 remain historical corrective evidence.

The next required evidence is physical: install rc11 on the target tablet, authenticate once with the existing allowlisted Firebase Email/Password account, prove a fresh Prepare separation crosses the full Storage → Function → Cloud Run → import path, then complete the remaining USB/audio/10-minute/ergonomics acceptance residuals.

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
**Status: HISTORICAL FOUNDATION / CLOSED IN UNIFIED LINE.** H37 introduced the transport; CI #669 signed rc4, and U8/U8m later completed production cutover plus provider-real Drive acceptance. Historical #664/H37a/H37b corrective chronology is retained below.
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

Current successor status: U0-U8 are closed (U9 retired); U10/C8 is **CLOSED / DIGITAL PASS** on Android CI #781; U11 is **CLOSED / DIGITAL PASS** historical release evidence; **U12 is active on signed rc11** at exact producer `34cb60624b2fabf21cfe2c60003b04eac1597418`, Android CI #806, with U4 #123 and U7 #81 green.

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
Current source chain is the fail-closed U-series tail ending at **U12w**, reached through `scripts/materialize_ci_sources.sh`. U12w seals the rc11 cloud-auth UI instrumentation corrective after the stable Firebase account migration; historical intermediate H/U blocks remain traceable, but the entrypoint defines the active chain.

H28 is frozen in `materialize_ci_sources_through_h28.sh`; the existing H29-H36c tail remains independently verified by `materialize_ci_sources_h29_h33.sh`; H37/H37a/H37b and U1/U1a/U1b are layered by their correspondingly named materializers. Each verifies its patch SHA-256 and terminal Git blobs. Future blocks must extend this chain deterministically and preserve the same fail-closed guarantees.

## Gate discipline
`.github/workflows/android-ci.yml` accepts manual dispatch and controlled `main` commit triggers. `[run ci]` executes software + API 36 gates; `[run ci signed]` also signs the exact tested candidate. Ordinary source/docs commits use `[skip ci]`. The unified-roadmap workstream has explicit authorization to trigger and monitor required gates.


## Unified U3 source-acquisition checkpoint — 2026-09-21
U3 ports the proven GBW acquisition behavior without copying the GBW project/UI model. Provider/network code is isolated from project mutation; validated media is published only through GuitarLab's unified project domain as `SOURCE_ORIGINAL`. Local import, remote search/ranking/download, persistent progress/cancel/retry and stale-operation/idempotency guards are included. Demucs/separation remains explicitly U4 scope. Automatic CI #676 closed U3 at exact source `a7af51bf6622b4eecc32308c091fbb5020a1d54b`: 358 JVM/unit tests, Lint, debug APK assembly, 37 standard API36 tests and 1 target-tablet geometry test all passed. U4 separation integration is next.
