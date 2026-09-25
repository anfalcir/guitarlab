# Implementation Roadmap

Updated: 2026-09-24

> **Current unified-line authority (2026-09-24):** historical H29-H37 remains architecture/evidence context. U10/C8 and U11 are CLOSED / DIGITAL PASS. RC19 is digitally qualified and signed at frozen source `e0a8a2218accbe9c8eec413a40f4fedf7d9da9bc`, but physical acceptance is blocked by a real Prepare audio-quality incident. `RC20_COMPLETION_PLAN.md` is the active corrective authority; RC14 remains the previous physically tested fallback.

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; final physical latency closure still active.
- **M8 — Release hardening / unified successor:** historical H29-H37 evidence is absorbed; unified U10/C8 is digitally closed and U11/U12 own final release freeze + physical residual.

## Current signed authority

**Android CI #852 / run `36026458960`** on frozen source `e0a8a2218accbe9c8eec413a40f4fedf7d9da9bc` is the latest signed digital candidate (`0.5.0-rc19` / versionCode `39`). U7 production #130 / run `36024185442` and independent post-cutover U4 #160 / run `36026359099` also passed.

Signed artifact: `GuitarLabStudio-0.5.0-rc19-homologacao` / artifact id `10820021943`.
Signed APK SHA-256: `20c6a49efa69828653862c29c7907cd734bd2e3f62235295cd53b7f7c571c50b`.

RC19 is **not physically accepted**. A target-device Prepare run for job `c3ba40ec-6153-483a-801f-9e838b3e3b60` completed but produced unusable audio despite valid v2 WAV/hash/container properties. The locked signer remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Current release closure

U10/C8 and U11 are digitally closed. U12 remains open. RC19 is the latest digitally qualified/signed candidate but is physically rejected pending resolution of the Prepare audio-quality incident. RC14 remains the previous physically tested fallback; RC9–RC18 are retained as corrective/history evidence as applicable.

F1/F2/F3 are complete and identify the `demucs.cpp`/GGML substitution as the audio root cause. Phase W in `RC20_COMPLETION_PLAN.md` owns reconstruction on official PyTorch `htdemucs_6s`. The release-critical order remains: close finalist shadow quality/performance → controlled production cutover with rollback target → close the primary search terminal-state defect → run relevant Android/U7/U4 qualification → sign the exact qualified RC20 unsigned artifact → perform physical homologation of that signed APK. **Do not expand this backend corrective with unrelated frontend work before cutover.** After backend/primary-flow homologation, complete the owner-requested frontend/support features already approved in the RC20 plan — Studio reference reinsertion, Prepare technical-detail grouping, consolidated Diagnostics, persistent sanitized journal and exportable diagnostic ZIP — then perform any affected focused Android/physical regression, record final identities and freeze. Extended cloud observability, DID_YOU_MEAN polish and exhaustive matrices remain optional unless an observed blocker needs them. Any RC20 source candidate uses `0.5.0-rc20` / versionCode `40`.
U12/RC20 Phase W has closed W1/W2/W3/W4 for the selected CPU8 shadow shape. U7 #158 / run `36078452293` PASS on producer `90428c81feca5eea4593ab384280846bd1175ccb` with finalist digest `sha256:6a6d5017e0e2c9bf3800bca3a3c3988ed9398add4ef2c3097241a7a260893dbe`. The historical W4 technical fixture completed in `280648 ms` (~4m41s), with warm inference previously measured at `89875 ms`, estimated cost ~$0.0494, zero quality rejects and a SHA-verified retained bundle. Under D-094, warm inference is retained as historical evidence and is not rerun in the normal gate. CPU4 remains optional. **W5 owner listening was explicitly waived by the owner under D-092. The only remaining backend prerequisite before W6 is successful proportional shadow requalification of the U12bj rebuilt digest; U12bk/U12bl change orchestration only and do not alter that worker image.**

## Approved post-H28 implementation path
Detailed authority: `history/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

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

Authoritative plan: `history/UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`.

Current successor status: U0-U8 are closed (U9 retired); U10/C8 and U11 are closed historical evidence; **U12 is active on the RC19→RC20 corrective line**. RC19 is the latest signed digital candidate but is physically rejected; RC20 is the only active successor target.

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
Current source chain is the fail-closed U-series tail ending at **U12bi**, reached through `scripts/materialize_ci_sources.sh`. U12bh makes stochastic Demucs numeric/hash differences evidence rather than vetoes while retaining exact contract/structure/finiteness/quality gates. U12bi corrects the W4 matrix-alias/canonical-engine-strategy validator mismatch. Historical intermediate H/U blocks remain traceable, but the entrypoint defines the active chain.

H28 is frozen in `materialize_ci_sources_through_h28.sh`; the existing H29-H36c tail remains independently verified by `materialize_ci_sources_h29_h33.sh`; H37/H37a/H37b and U1/U1a/U1b are layered by their correspondingly named materializers. Each verifies its patch SHA-256 and terminal Git blobs. Future blocks must extend this chain deterministically and preserve the same fail-closed guarantees.

## Gate discipline
`.github/workflows/android-ci.yml` accepts manual dispatch and controlled `main` commit triggers. `[run ci]` executes software + API 36 gates; `[run ci signed]` also signs the exact tested candidate. Ordinary source/docs commits use `[skip ci]`. The unified-roadmap workstream has explicit authorization to trigger and monitor required gates.


## Unified U3 source-acquisition checkpoint — 2026-09-21
U3 ports the proven GBW acquisition behavior without copying the GBW project/UI model. Provider/network code is isolated from project mutation; validated media is published only through GuitarLab's unified project domain as `SOURCE_ORIGINAL`. Local import, remote search/ranking/download, persistent progress/cancel/retry and stale-operation/idempotency guards are included. Demucs/separation remains explicitly U4 scope. Automatic CI #676 closed U3 at exact source `a7af51bf6622b4eecc32308c091fbb5020a1d54b`: 358 JVM/unit tests, Lint, debug APK assembly, 37 standard API36 tests and 1 target-tablet geometry test all passed. U4 separation integration is next.


## RC20 final Android completion status — U12bn
The committed pre-freeze frontend/support scope is implemented and protected by the U12bn materialization tail. The next blocking step is Android CI/API36 qualification of the complete RC20 source, followed by exact-artifact signing and one consolidated physical homologation campaign. No intermediate incomplete APK is eligible for freeze.
