# Current State — GuitarLab Studio

Updated: 2026-09-20

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Source candidate: `0.5.0-rc4`, versionCode `24`, H37b + H37c provenance corrective — **SOURCE PRE-GATE**.
- Current signed version remains `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`, until H37c passes the manual canonical gate.
- Current signed source line: CI #663 producer `51d4098fa7b1b44a9fa315e939541020f594654d`.
- Latest signed DIGITAL PASS: **CI #663** / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d`.
- #663 scope: H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35 → H35a → H36 → H36a → H36b → H36c.
- Signed APK SHA-256: `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.
- Signed APK size: `13,835,802` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- CI remains manual-only (`workflow_dispatch`).

## #663 digital evidence
All three canonical jobs passed on the exact producer SHA:
- deterministic materialization through H36c PASS;
- **330/330 JVM/unit tests PASS**, 0 failures/errors/skips;
- Android Lint PASS with **0 errors, 50 warnings and 4 hints**;
- debug/release assembly and unsigned provenance PASS;
- unsigned tested APK SHA-256 `215315f8b943704b9b820f98b4b7747df2fbbc62b862ab08e6c14d4dea9e89ad`;
- **34/34 standard API36 instrumented tests PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing, zipalign, package/version and certificate verification PASS;
- signed APK SHA-256 `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

Locked signer SHA-256 remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H28 physical backup result
Target-device testing after #653 confirmed the corrected backup behavior is working correctly. In particular, the false commit-failure/selective-duplicate behavior that triggered H28 is no longer reproduced in the accepted backup workflow.

H28 remains a protected regression contract covering immutable `projectId`, deterministic `revisionId`, package SHA-256 integrity, provider-lag tolerance, idempotent unchanged backup and bounded retention.

## H29-H36c digital closure
H29-H36c are digitally closed by CI #663.

H34/H35/H35a are now part of the exact signed candidate:
- global route/rate manual residual range is ±500 ms and applies only to future recordings;
- persistent take-specific synchronization is delta-based, non-destructive and Undo/Redo-aware;
- silent digital verification uses PCM zero and never stores fake physical round-trip latency;
- physical calibration validates the exact live input/output IDs before emitting a deterministic 32 ms windowed chirp capped at 12% peak;
- the H35a explicit RECORD_AUDIO guard passed Android Lint without suppression.

## H36/H36a/H36b/H36c — Settings UX Polish — DIGITAL PASS
H36 is now part of the exact signed #663 candidate.

Closed behavior:
- main Settings content is centered/capped on wide tablet layouts;
- section chrome and secondary actions use the compact responsive hierarchy;
- the calibration modal exposes all ±25/±5/±1 ms controls without horizontal scrolling;
- Settings/External Control/diagnostics/calibration instrumented coverage passes on API36;
- H36a/H36b/H36c remain traceable test-only correctives; runtime `SettingsScreen.kt` is unchanged from H36.

CI progression is retained as evidence: #660 exposed a test-import issue, #661 exposed three viewport assumptions, #662 reached 33/34 instrumentation PASS, and #663 closed the complete canonical gate at 34/34 + geometry + signing.

## CI #664 — H37 compile failure and H37a corrective
Manual CI #664 / run `35544867278` executed the merged H37 producer `abecc73e4eab181a7776d98cc731758b17c64b06` and failed before Lint/build/signing because both parallel Android jobs reached the same Kotlin compile failure.

The failure was deterministic and limited to H37 code:
- `DriveAuthorization.kt` used `tryResume/completeResume` and `tryResumeWithException/completeResume`, which Kotlin/coroutines 1.11.0 rejects as internal API usage;
- `DriveV3Protocol.kt` used an expression-bodied `withContext<DriveHttpResponse>` loop whose lambda terminal type was inferred as `Unit`.

H37a is a minimal compile corrective:
- switches the Google Task bridge to public stable `Continuation.resume` / `resumeWithException` and handles Task cancellation explicitly;
- rewrites the authorized HTTP loop around an explicit nullable completed response and `checkNotNull`, preserving the one-refresh-on-401 semantics;
- keeps all H37 Drive domain, OAuth, integrity, resumable-upload and migration behavior unchanged;
- adds a deterministic H37a source part and materializer on top of H37.

Pre-publication H37a evidence:
- targeted Kotlin/coroutines compile probe PASS;
- exact H37 → H37a patch apply PASS;
- terminal Git blob verification PASS;
- actual H37a materializer first run PASS;
- second materializer run idempotent PASS;
- corrupted H37a archive rejected fail-closed before source mutation PASS.

CI #664 is retained as failed compile evidence only. CI #663 remains the signed DIGITAL PASS authority until H37b completes the full manual canonical gate.

## CI #665 — H37b software/API36 PASS, signing provenance corrective H37c
Manual CI #665 / run `35546264450` executed producer `01b2371310fb872eb1583231728941beb93c1a8e`.

Digital result:
- Unit tests + Lint + APK build: **PASS**;
- API 36 emulator regression: **PASS**;
- unsigned artifact checksum: **PASS**;
- unsigned APK SHA-256: `343bd7423275481039fc0475527bcb6a22c259a69136a5e052b6adbe5b0421f2`;
- artifact identity: package `studio.guitarlab.app`, versionName `0.5.0-rc4`, versionCode `24`, source SHA `01b2371310fb872eb1583231728941beb93c1a8e`.

The signed job failed before the private signing bundle was restored. Root cause: the provenance step compared the tested RC4 artifact against the raw, unmaterialized repository base `app/build.gradle.kts`, which still carries RC3/23. The upstream build jobs had correctly materialized H37b before building.

H37c is a workflow-only corrective: the signed job now runs the canonical source materializer plus `git diff --check` before comparing package/version/source identity. It does not rebuild or modify the tested unsigned APK.

CI #665 therefore provides positive H37b software/API36 evidence but is not a signed DIGITAL PASS. CI #663 remains the signed authority until a fresh manual H37c run completes signing/provenance.

## H37b — OAuth token-cache hardening — SOURCE PRE-GATE
H37b is the final pre-CI hardening on top of H37a. Google documents `AuthorizationClient.clearToken(ClearTokenRequest)` as the API that removes a rejected access token from the local Google Identity Services cache. H37b uses it whenever Drive returns HTTP 401, then reacquires authorization through the existing `drive.file` flow instead of risking reuse of the same rejected token.

H37b changes no backup identity, upload, retention, restore or SAF-migration semantics. Pre-publication evidence:
- official Google Identity Services API signatures verified for `clearToken` / `ClearTokenRequest`;
- targeted authorization/coroutines compile probe PASS;
- clean H37a → H37b apply PASS;
- terminal Git blob verification PASS;
- actual H37b materializer first run PASS;
- second run idempotent PASS;
- corrupted H37b archive rejected before H37b source mutation PASS.

Infrastructure decision: GuitarLab may share the same Google Cloud/Firebase project used by GBW, but `studio.guitarlab.app` remains a distinct Android app/OAuth client identity. Firebase SDK is not required by the Drive backup transport.

## H37 — Native Drive v3 backup transport — SOURCE PRE-GATE
H37 is implemented as the next source block on top of the signed H36c baseline. It changes the primary backup transport from SAF to direct Google Drive API v3 while preserving the H28 domain identity and retention contract.

Implemented source behavior:
- OAuth uses only `https://www.googleapis.com/auth/drive.file`;
- no Firebase/Cloud Run/Functions, service account, client secret or refresh token is in the backup data path;
- regular `.guitarlab` files are stored in the user's Drive with private `appProperties` for project/revision/commit identity;
- resumable 8 MiB uploads persist the session URL locally and always query the server-confirmed offset before retransmission;
- completed uploads are accepted only after Drive size + SHA-256 match and the remote state is promoted to `committed`;
- 401, 429, transient 5xx, I/O interruption, process death and a lost final commit response have bounded/fail-closed recovery paths;
- package ZIP entry timestamps are deterministic so a persisted revision regenerates byte-identical package content for safe resume;
- existing H26-H28 SAF history can be migrated without deleting remote legacy content, and the old SAF permission is released only after zero migration failures;
- Drive account/session recovery state is excluded from Android cloud backup/device transfer;
- automatic WorkManager backup remains constrained by the existing scheduling policy and is enabled only when Drive is connected.

Source integrity evidence before publication:
- clean H36c → H37 materialization PASS;
- second materialization idempotent PASS;
- every H37 terminal Git blob matched its declared hash;
- deliberately corrupted H37 source archive failed closed before H37 source mutation;
- shell syntax, `git diff --check` and static credential/scope audits PASS.

H37 reached canonical CI #664 but failed at Kotlin compilation before Lint/build/signing. H37a corrects the compile boundary; H37b additionally clears rejected access tokens from the Google Identity Services cache before reauthorization. H37b is **not DIGITAL PASS yet** until a new manual canonical run passes. CI #663 therefore remains the signed authority.

## Remaining RC3 physical blocker
The remaining release-critical target-only item is **recording latency/synchronization acceptance** on the intended real USB route. The recording timing architecture is already implemented and digitally covered; the unresolved boundary is physical driver/hardware behavior.

Final stable promotion now has two independent boundaries: the existing RC3 hardware residual still requires no repeatable P0/P1 and explicit approval of an exact signed candidate; H37b additionally requires its manual exact-source digital gate plus first real OAuth/Drive backup/restore acceptance before RC4 can supersede RC3.

## Approved next-development scope
The authoritative plan is `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Before stable 1.0.0, the #663 baseline is digitally closed; only the remaining hardware-only H29/H30/H32/H35 residual must now be physically closed:
- recording synchronization/session-health hardening;
- interrupted-recording recovery UX;
- USB audio disconnect/reconnect resilience;
- takes-management refinement;
- song/session quality and stress coverage through 10 minutes;
- diagnostics refinement;
- H28 backup/restore regression hardening;
- final UX/accessibility polish.

The only approved new feature after the stable hardening line is external MIDI/footswitch control. Marker/section enhancements, clip-gain UI, Reference × My Guitar comparison enhancements and a separate large-project performance program are explicitly excluded from this roadmap.
