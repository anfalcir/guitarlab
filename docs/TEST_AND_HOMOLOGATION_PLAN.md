# Test and Homologation Plan

Updated: 2026-09-24

## Current evidence boundary

The newest unified **technical DIGITAL PASS** is U10/C8 on Android CI #781 / run `35791192802`, exact source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`.

U10 evidence:
- deterministic source materialization through U10zb PASS;
- software gate PASS;
- Android Lint PASS with 0 errors;
- debug/release build gate PASS;
- API 36 fail-closed classification 23/23 classes;
- 76 observed instrumented tests across 5/5 groups PASS;
- 1/1 isolated target-tablet 1920×1200 geometry PASS;
- 24/24 required cohesion screenshots retained;
- deterministic visual review PASS;
- signed homologation intentionally skipped because U11 owns final freeze/signing.

The latest digitally qualified physical-corrective candidate is RC18: Android CI signed run `35990311091`, producer `ee947c6fbbd8b24430d3a3faa04e27ef9a46856c`, package `studio.guitarlab.app`, `0.5.0-rc18` / versionCode `38`, signed artifact `GuitarLabStudio-0.5.0-rc18-homologacao` / artifact id `10804182567`. U7 verify `35990311054`, isolated shadow `35991484767`, controlled production deploy `35992709966`, and U4 production smoke `35994148711` passed. RC18 now requires target-tablet physical acceptance.

RC18 physical acceptance is deliberately end-to-end: install over RC15/RC16/RC17, run a new separation, confirm clean Base plus correctly panned Guitar L/R with no clipping/distortion, delete the three prepared clips, and verify **Sincronizar Studio** reinserts them without network or Demucs. Then run another separation with the lanes empty and confirm automatic population. Recordings, takes and mixer state must remain unchanged.

Rc8 is CLOSED / DIGITAL PASS but withdrawn from final U12 approval after physical testing exposed the second orphan class: a durable local `CANCEL_REQUESTED` job can exist with no Firestore/Cloud Run counterpart. Treating local durability as proof of remote existence left the UI indefinitely active.

Rc9 closes this corrective digitally on the exact producer:
- Android CI #794 / run `35856278980`: exact materialization, unit/performance, Lint, build, API36 and signing PASS;
- U4 Cloud Integration Smoke #111 / run `35856278976`: real six-stem Cloud Run smoke PASS;
- U7 Cloud Backend #69 / run `35856279117`: backend source/container/security verification PASS; deploy skipped;
- signed artifact id `10748475073`;
- APK SHA-256 `4fcf529b935a584217b3b882ca8dfa05e3c360f99cfe9d70071080175439dd6c`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Protected rc9 regression coverage:
- `CANCEL_REQUESTED + remote missing` terminalizes locally as `CANCELLED`;
- `RUNNING/COMPLETED/IMPORTING + remote missing` terminalizes as `EXPIRED`;
- pre-dispatch `UPLOADING/READY/QUEUED + remote missing` retains idempotent replay;
- cancellation and worker retry exhaustion cannot leave a durable nonterminal state forever;
- `SOURCE_READY` recovery requires the same source generation and absence of a newer active job;
- project snapshot selection prefers a newer/current active generation over a late terminal record from an older job;
- API36 verifies the recovered-orphan UI preserves the accepted source, hides raw backend error codes and exposes retry.

Physical retest is limited first to upgrading over the actual rc8 stuck state, confirming automatic convergence/source preservation/retry, then verifying the new job crosses the real cloud boundary across background/reboot/cancel. Unrelated accepted physical evidence remains reusable under the U12 invalidation rule.

Drive provider-real acceptance remains closed by U8m:
`U8m PASS · r_1790095960 · cleanup 8/8/14`;
sanitized report SHA-256 `84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`.

Historical rc5→rc8 evidence remains traceable but does not supersede rc9. U12 resumes on rc9; only the orphan-recovery/Prepare/background scenarios affected by U12k plus adjacent smoke must be repeated, while unrelated physical evidence may be reused according to the U12 invalidation rule.

## H29-H36c digital closure
CI #663 digitally closes H29-H36c on exact source. The H34/H35 take-synchronization, silent-verification and quieter calibration code is therefore covered by the canonical unit/Lint/build/API36/signing pipeline.

## H36/H36a/H36b/H36c Settings UX digital closure
H36 preserves all prior signed functional behavior while proving the presentation change:
- existing calibration-modal instrumentation still opens/closes the dedicated panel and sees silent/physical calibration actions;
- calibration instrumentation now verifies both extreme manual controls (−25 ms and +25 ms), inner ±1 ms controls and `Zerar ajuste` are visible without a horizontal adjustment carousel;
- new Settings hierarchy instrumentation verifies the centered Settings root and compact Audio/External Control actions remain discoverable through the vertical Settings flow;
- existing External Control settings instrumentation continues to exercise opt-in, HID and each Learn mapping tag;
- API36/isolated geometry must catch clipping/overflow regressions on the canonical Android gate.

CI #661 executed 34 app instrumented tests after H36a; 31 passed and 3 failed on viewport-only visibility assumptions. H36b now scrolls to the HID toggle, the relevant manual-calibration controls/reset, and the Diagnostics section before asserting `displayed`. No extra physical audio test is required solely because of H36/H36a/H36b; runtime audio/routing/DSP/calibration semantics are unchanged. CI #663 closes the H36 line at **34/34 app instrumentation PASS** plus isolated target geometry and signing. The H36c stable-tag assertion is therefore confirmed in the canonical emulator gate. Physical H35 behavior remains the existing target-device residual.

## Protected H28 regression contract
Automated or programmatic coverage must continue to prove:
- rename preserves `projectId`;
- duplicate project receives a new `projectId`;
- revision ID is deterministic for unchanged canonical state;
- revision ID changes when canonical state changes, even under equal timestamp;
- v2 dedup never collapses distinct same-timestamp states;
- v1 backup metadata remains readable;
- targeted revision discovery prevents duplicate commit under stale provider listing;
- exact remote write verification does not depend on immediate parent-list visibility;
- retention remains bounded per project and fail-safe on partial/total failure.

## H37/H37a/H37b Drive v3 historical acceptance contract
H37 introduced the direct Drive v3 transport and its historical corrective sequence. CI #669 closed the signed rc4 gate; U8/U8m later closed unified production cutover and provider-real acceptance. The detailed contract below remains a protected regression reference, not a pending U11 migration/pre-gate.

Canonical automated gate must additionally prove:
- H37 five-part archive reconstructs to the declared gzip/patch hashes and all terminal Git blobs;
- H37a corrective archive reconstructs to its declared gzip/patch hashes and terminal blobs;
- H37b corrective archive reconstructs to its declared gzip/patch hashes and terminal blobs;
- rejected-token recovery uses Google Identity Services `clearToken` before requesting a replacement token;
- Drive protocol/query/property encoding tests pass;
- remote-store tests cover committed-catalog filtering, exact revision lookup, idempotency and integrity mismatch rejection;
- deterministic project bundle tests prove the same persisted state regenerates byte-identical package bytes;
- authorization state and resumable-session metadata are excluded from Android cloud backup/device transfer;
- all pre-existing H28 coordinator/domain regressions still pass;
- Android Lint, debug/release build, API36 instrumentation/geometry and signing remain green on the exact H37 producer.

Historical first target OAuth/Drive acceptance contract covered:
- Google Drive API enabled and an Android OAuth client registered for package `studio.guitarlab.app` and signer SHA-1 `42:C7:0C:79:D3:B5:CB:2C:FE:BD:F7:FF:80:93:1B:BB:F4:D1:00:37`;
- consent requests only `drive.file`;
- first manual full backup creates committed revisions visible in Drive;
- repeating an unchanged backup creates no duplicate logical revision;
- changed project creates exactly one new revision and retention remains bounded;
- interrupt a representative larger resumable upload, then confirm resume from the server-confirmed offset rather than restart/blind replay;
- restore validates package integrity and creates an independent local project copy;
- disconnect/reconnect revokes/reobtains authorization without deleting remote backups;
- historical SAF/H37 migration is not a unified-product acceptance requirement after the clean-cutover decision; retain this item only as historical H37 evidence.

A failed/partial upload must never appear in the restore catalog, and a completed upload whose final commit response was lost must be reconciled without duplicating package content.

## Remaining target-device residual
For H35, physical validation should additionally confirm that:
- changing the global Settings value does not move an already recorded take;
- changing the take-specific value in the take editor moves that take/splits coherently and survives save/reopen;
- silent digital verification produces no intentional non-zero test signal;
- physical calibration no longer emits the previous harsh burst and aborts silently if the selected output/input are not the actual routed devices.

Keep manual work limited to what software/emulator cannot prove for the exact future signed candidate:
- intended USB input/output is the effective route during REC;
- hardware loopback remains off and backing is not printed into the guitar take;
- live waveform/meters remain temporally coherent;
- no silent microphone fallback occurs;
- no repeatable systematic late/early placement remains;
- USB hot-unplug/reconnect preserves valid capture and does not auto-resume REC;
- one continuous 10-minute recording/playback quality smoke shows no growing offset/dropout/wrong speed and survives save/reopen/export;
- representative transport/edit/save/reopen/export smoke remains healthy.

H33 real-controller connect/map/reconnect/tactile double-trigger acceptance remains the separate 1.1 physical residual and is not a stable-1.0 completion requirement.

Everything objectively established by #663 does not need to be repeated manually unless a later source change invalidates that evidence.

## Post-H28 forward quality plan
The post-H28 audio/external-control plan remains `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`; H37 backup transport is additionally governed by `H37_DRIVE_V3_BACKUP.md`.

Key additions:
- H29 recording session-health evidence and latency closure;
- H30 interrupted-recording recovery + USB resilience;
- H31 takes + diagnostic report refinement;
- H32 quality/stress target for songs up to 10 minutes + backup regression + UX/accessibility;
- H33 external MIDI/HID footswitch control after stable 1.0;
- H37 direct Drive v3 backup transport with exact-source digital gate and first OAuth/Drive target acceptance.

For the 10-minute quality line, stress must model realistic song projects rather than create a separate artificial mega-project product requirement.

## Unified successor program — homologation strategy

The approved unified integration program is governed by `UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`.

**Clean-cutover override (2026-09-21):** automatic migration/import compatibility for standalone GBW, H37/pre-unification GuitarLab and historical project corpora is no longer a release requirement. U9 is retired. Historical H37/migration evidence below remains historical evidence only and must not create new future gates.

Its evidence strategy is intentionally stricter digitally and smaller manually:
- unit, property/randomized, fuzz/malformed, fake-network, process-death, Android instrumentation, accessibility, geometry, audio golden, Drive transaction and cloud contract suites close all objectively testable behavior for the unified line;
- a controlled real-cloud Demucs smoke is a digital integration gate before final candidate freeze;
- known current GuitarLab hardware residuals may be carried forward instead of forcing an intermediate manual campaign, provided later source changes preserve/strengthen their digital contracts;
- U11 freezes one exact signed candidate after complete digital closure;
- U12 performs one consolidated target-device campaign for real MK-300 routing/capture/isolation, timing/listening, 10-minute stability, real Prepare flow, Drive account/backup/restore and tablet ergonomics;
- if U12 exposes a source defect, only physical evidence invalidated by the corrective change plus adjacent risk smoke must be repeated.

This successor strategy does not retroactively change the evidence status or release identity of CI #663, H37b or older candidates.

### U1 source pre-gate coverage

The U1 source block adds tests for schema-1 migration, optional empty preparation, asset/provenance validation, canonical revision determinism, rename identity, reachability-safe cleanup, portable asset round trip and 100 reproducible randomized inventory orderings. Clean materialization, idempotent rerun and corrupted-patch fail-closed behavior pass locally. CI #667 exposed compile defects corrected by U1a. CI #668 then compiled production source and ran 191 tests, with one fixture failure corrected by U1b. CI #669 closed the corrected canonical gate.

CI #669 closed U1 with unit, Lint, debug/release build, API 36 instrumentation and signed identity PASS. U2 adds route-codec coverage for Prepare/Export, pure status/legacy-entry policy tests, and Compose instrumentation for safe acquisition placeholders, template preservation and empty Prepare behavior. Existing Home search/filter/sort tests remain mandatory regression coverage.

Automatic CI #670 validated the new commit trigger and deterministic U2 materialization, then stopped at Kotlin compilation because of one trailing comma after the final `when` branch expression. U2a removes that token without changing behavior; all U2 tests remain pending the corrected automatic run.

Automatic CI #671 compiled the U2a production source and stopped while compiling the new Android-module unit test because it used the multiplatform `kotlin.test.Test` import. U2b switches to the module-standard `org.junit.Test`; test behavior and assertions are unchanged.

Automatic CI #672 passed unit, Lint and APK build. Its API 36 suite failed after adding the new scrollable creation screen test; U2c removes viewport dependence by using `performScrollTo()` before selecting the blank template and submitting creation, preserving the assertions on safe acquisition states and template output.

Automatic CI #673 stopped in diff sanity before materialization because the generated U2c patch ended with a blank context line. The normalized patch has the same source delta, a refreshed SHA-256 lock and no blank-at-EOF condition; the full gate remains pending.

Automatic CI #674 passed unit, Lint and APK build and ran all 36 API 36 tests. The sole failure showed that `new-project-name` resolved to the non-focusable heading row, so text input could not request focus. U2d moves that test tag to the existing `OutlinedTextField`; behavior and assertions remain unchanged.

Automatic CI #675 / run `35596671163` closed U2 at exact source `665c292931c0f2ed9fa1e7145818101c5d017222`: deterministic U2d materialization, unit tests, Lint, debug/release builds and all 36 API 36 tests passed. No signed candidate was requested for this intermediate milestone.

C2 product-cohesion closure is proven by Android CI #713 / run `35660979142` at exact source `66cedad38875d6f7284180ef46d25d4e9049d287`: the shared project shell, remembered/relevant Home workspace, exact Settings-origin return, missing-project fallback and workspace state retention passed JVM/unit, Android Lint, APK build and the complete API 36 regression. No signed candidate was requested for this intermediate cohesion gate.

C3 product-cohesion closure is proven by Android CI #720 / run `35669035762` at exact source `f730e08cc242ed590c333727cde4c96883f7df07`: deterministic materialization through C3g, JVM/unit tests, performance evidence, Android Lint, APK build and all 5/5 API 36 regression groups passed. The project/navigation group observed 9 tests, including New Project Activity recreation with preserved name/import intent. Source snapshot artifact `guitarlab-source-f730e08cc242ed590c333727cde4c96883f7df07` is the exact compiled source. No signed candidate was requested for this intermediate cohesion gate.

## Final rule
Any promoted release requires exact-source digital PASS, only the genuinely hardware-dependent residual applicable to that source, no repeatable P0/P1 and explicit approval of the exact signed APK SHA-256.


## Unified U3 source-acquisition digital gate
U3 requires the controlled canonical Android CI before promotion. The digital gate covers:
- deterministic U2d→U3 materialization and terminal hashes;
- source normalization/ranking, duplicate filtering, preview demotion/rejection and duration-consensus behavior;
- malformed/empty provider outcomes and bounded retry classification;
- authoritative SOURCE_ORIGINAL publication only after validation;
- operation-id idempotency, project rename preservation and stale-source conflict rejection;
- local Import and remote Search/selection UI entry points;
- persistent acquisition activity/progress/cancel state across view-model recreation boundaries represented by the durable operation store;
- API36 Compose regression for U3 entry points/activity state;
- existing GuitarLab unit/Lint/build/API36 regression unchanged.

No physical listening/hardware gate is required to close U3. Real provider/network credentials are an external dependency only if a provider demands them; deterministic product correctness must not depend on secret credentials. A real-source smoke may be deferred to the consolidated U12 campaign unless the canonical environment can perform it without introducing nondeterministic CI dependency.


### U3 canonical closure — CI #676
Exact source: `a7af51bf6622b4eecc32308c091fbb5020a1d54b`; run: `35606018087`.
- deterministic U3 materialization: PASS in both canonical jobs;
- JVM/unit: 358/358 PASS, 0 failures/errors/skips;
- Android Lint: PASS, 0 errors;
- debug APK assembly: PASS;
- standard API36 instrumentation: 37/37 PASS;
- isolated target-tablet geometry: 1/1 PASS;
- signed homologation: intentionally not requested for intermediate U3.

U3 therefore requires no separate physical homologation. Provider/media realism remains part of the final consolidated Prepare smoke together with U4+ integration; it does not reopen the digitally proven U3 publication/ownership contracts.


## Unified clean-cutover compatibility boundary — 2026-09-21

Future unified release gates must test compatibility and resilience only for the supported unified product line. Required persistence coverage remains:
- current unified project save/reopen;
- duplicate/new projectId semantics;
- supported schema evolution inside the unified line;
- backup/restore round trip for the new unified Drive model;
- malformed/current-schema fail-closed behavior;
- project identity, asset integrity and transactional publication;
- process-death/cancellation/network-failure recovery.

Not required:
- pre-unification GuitarLab corpus migration;
- H37 backup migration;
- standalone GBW `.gbwbackup` import;
- GBW Share/Open bridge;
- legacy migration corpus in U10/U11;
- physical verification of legacy-project upgrade.

Historical tests already executed are retained as evidence but are not repeated solely to preserve obsolete compatibility.


## Unified product-cohesion digital gate — 2026-09-21

The detailed contract is `UNIFIED_PRODUCT_COHESION_AUDIT.md`. Product cohesion is release-blocking, not optional visual polish.

### U6 boundary — CLOSED
Android CI #694 / run `35630563575` at exact source `364a8ddb1904003af150c9162fed6aa6a4106d8e` remains the technical U6 export baseline. C1 convergence is closed by Android CI #711 / run `35658467705` at exact source `6ff1988a8309b7174eeb328e0044d6b1d3167ff6`: software gate and the complete API 36 instrumented regression passed, the exact materialized source contains no independent Home/Studio format chooser or obsolete Studio-local export state/entry point, and all project export entry points converge on the canonical Export workspace. Primary actions use concise labels while zero-transcode details remain secondary explanatory copy. U6 is DIGITAL PASS. #711 did not produce a signed homologation APK because signed homologation was not requested.

### C4 boundary — CLOSED
Android CI #723 / run `35673758282` at exact source `0d7b05d0192cc415d328e415c517b3cad40dc209` passed deterministic source materialization, JVM/unit and performance gates, Android Lint, debug/release assembly and all five API 36 regression groups. The covered Prepare contract includes progressive active/completed/future presentation, accepted-source collapse and replacement, accessible score semantics, user-safe primary status, diagnostics isolation, automatic deterministic reference generation after six validated stems, retry-only manual recovery, lifecycle-aware durable observation without Compose polling, and coherent reopen/background ownership. C4 is DIGITAL PASS; U7 is unblocked.

### C5 boundary — CLOSED

C5 is closed by Android CI run `35717888405` at exact materialized source `03735dabd3f4cbfc5a7180633884c85ce5540adb`. The gate passed exact C5 source materialization/blob verification, JVM/unit tests, reproducible performance evidence, Android Lint, debug/release assembly and all five API 36 regression groups, including the isolated tablet geometry check. C5 covers durable prepared-reference acknowledgement by revision, non-destructive reference rebinding, project-owned media inspection from Studio, no automatic stem timeline insertion, creative-state preservation and the direct Prepare → Studio handoff. Signed homologation was not requested for this intermediate cohesion milestone.

### Required navigation/project-shell tests
- Home project tap resolves to last/relevant workspace deterministically;
- Prepare ↔ Studio ↔ Export preserves project identity and durable creative state;
- process recreation restores the same valid project/workspace;
- deleted/missing project route fails to a user-safe library recovery state;
- Settings returns to its exact originating project/workspace;
- phone/tablet adaptive navigation exposes the same capabilities.

### Required Prepare journey tests
- Search and local Import converge to the same downstream preparation state;
- source replacement is explicit and does not mutate Studio recordings/edits silently;
- six validated stems trigger deterministic reference generation automatically;
- reference-generation failure exposes retry without losing accepted stems;
- normal Prepare completion requires no redundant “create references” confirmation;
- raw remote-job/asset-role identifiers never appear in primary semantics;
- background/reopen/notification preserves operation ownership.

### Required operation/Activity tests
For source acquisition, separation, reference generation, study export, master export, backup and restore:
- operationId/projectId ownership;
- running/retry/cancel/terminal state mapping;
- process recreation;
- leaving the owning screen does not cancel valid work;
- explicit project deletion cancels/reconciles owned work safely;
- notification/deep-link routes to the owning project or Activity item;
- stale result cannot publish into a newer project revision.

### Required export-cohesion tests
- all Home/Studio export/share entry points navigate to the canonical Export workspace;
- no independent encoder/format chooser remains outside Export;
- study WAV remains direct publication of the canonical reference when compatible;
- FLAC/MP3 remain single-encode paths;
- unavailable codecs show an accessible reason/next action;
- cancel/failure leaves project state untouched.

### Required lifecycle/destructive matrix
Exercise rename, duplicate and delete while each relevant background operation is:
- idle;
- running;
- retrying;
- terminal but not yet acknowledged.

Duplicate must never copy transient operation identity. Delete must not orphan WorkManager/cloud/temp ownership.

### Visual/copy evidence
CI/U10 must retain a screenshot artifact set for:
- Home empty + populated/active-job states;
- New Project;
- Prepare search/results/separating/ready/error;
- Studio normal + prepared-reference update;
- Export ready/running/error;
- Activity;
- Settings;
- Backup/restore;
- destructive confirmation.

Matrix:
- target-tablet class 1920×1200;
- representative phone portrait;
- compact-width/landscape stress where supported;
- dark/light;
- normal and enlarged representative font scale.

Pixel-perfect golden gating is optional if too brittle. Screenshot generation, semantic geometry checks and a deterministic review checklist are mandatory.

### Terminology/static UX audit
Before U11 freeze, scan user-facing resources/source for:
- orphan “GBW” product branding;
- “Study Exports” / untranslated section headings;
- raw `AssetRole` names;
- raw remote-job enums;
- internal IDs/SHA text outside Details/Diagnostics;
- duplicate “Salvar e exportar” format chooser;
- user-facing “export from GBW/import into Studio” language.

### Accessibility
- minimum touch targets;
- meaningful TalkBack order;
- progress/state announcements;
- no state conveyed by color alone;
- enlarged-font layout remains operable;
- focus traversal does not expose hidden/duplicate controls.

### Physical boundary
U12 must not be used to discover basic information-architecture, duplicate-flow or copy-consistency problems. Those are digital U10/U11 gates. U12 UX work is limited to real-device ergonomics, tactile behavior, viewing-distance readability and hardware-specific focus/system-bar anomalies.


## U8i / C6 digital closure

Android CI #744 / run `35729709715`, exact source
`89a7cca5acd2d5a1c9aabbb5f72e028347dd00db`, closes C6 digitally.

Evidence:
- deterministic U8i materialization and source snapshot PASS;
- JVM/unit and reproducible performance evidence PASS;
- Android Lint PASS;
- debug/release assembly PASS;
- complete grouped API 36 regression PASS;
- confirmed-revision persistence does not advance from failed/unconfirmed attempts;
- project sync presentation requires the current local revision to equal the
  durable server-confirmed revision before showing synchronized;
- notification deep-link navigation to the owning Activity operation is
  instrumented with lifecycle-safe ActivityScenario coverage.

This does **not** close U8. Production vNext backup/restore cutover, fault
recovery and real Google Drive acceptance remain mandatory before U8 DIGITAL
PASS.
