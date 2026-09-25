# Current State — GuitarLab Studio

Updated: 2026-09-24

- RC20 CI optimization (D-093): U7 shadow now uses one representative CPU8 Cloud Run benchmark as the combined W3/W4 gate; separate W3 Cloud Run parity and standalone model-probe executions are removed from the normal path, CPU4 remains opt-in, full U4 runs only after production cutover so automatic rollback remains available, and the waived W5 workflow is removed. This changes workflow orchestration only; it does not mutate the worker image or Demucs recipe.

- RC20 CI integrity review (D-094): the consolidated CPU8 W3/W4 shadow gate now performs exactly one Cloud Run execution and one Demucs inference; warm/model probes default off, CPU4 remains explicit opt-in, and standalone U4 is aligned to official RC20 model SHA `34c22ccb…`. U4 remains separate only post-cutover because it validates transactional cloud lifecycle and preserves automatic rollback. Source materialization tail is U12bl.

- U7 #164 / run `36085821470` PASS on producer `aab87776f314fddc20028432371a9a5d268079c1`; all verify/auth/shadow/runtime/SBOM-Trivy/U4/W3/W4 gates completed green under the pre-U12bk workflow. Newly qualified RC20 worker digest: `sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698`. Previous production remains `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`. U12bm now makes W6 promote that exact qualified digest without rebuild; current source-tail changes U12bk/U12bl/U12bm are workflow/release-orchestration only and do not mutate worker image contents.

- W6 controlled production cutover dispatched from this source state: promote exact prequalified digest `sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698`, verify runtime contract, run full U4 transaction/recovery/ACK-purge gate, and restore prior production `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550` automatically on failure.

- RC20 Android completion implementation is now staged under U12bn: persistent source-search terminal outcomes with bounded “Você quis dizer…”, local/idempotent Studio reference reinsertion, grouped Prepare technical details, Configurações → Diagnóstico, sanitized 14-day/8-MiB JSONL journal, offline diagnostic ZIP with SHA256SUMS and no song audio, and durable sanitized accepted-manifest retention before remote ACK/purge. Android identity is now `0.5.0-rc20` / versionCode `40`. This scope is pending Android CI/API36 qualification before signing/final physical homologation.

- U12bn complete RC20 Android qualification dispatched: exact source includes final search UX, local reference repair, grouped Prepare diagnostics, consolidated Diagnostics/journal/ZIP, accepted-manifest retention and Android identity `0.5.0-rc20` / versionCode `40`. Signing remains intentionally deferred until this exact unsigned source passes Android CI/API36.

- CI #886 exposed a Kotlin compile-only defect in `AcceptedRemoteManifestStore`: sanitized JSON was inferred as `Any?`. U12bo narrows it fail-closed to `JSONObject`; the parallel API36 job failed earlier in emulator/ADB startup and therefore produced no functional regression evidence. RC20 remains unsigned pending rerun.

- U12bo corrective qualification dispatched after CI #886 compile failure; signing remains deferred until the exact corrected source passes unit/lint/build and API36 regression.

- CI #887 exposed four local compile defects in the new diagnostics/Prepare UI (`JSONObject` narrowing, nullable sanitizer accumulator, Compose weight import resolution, cross-module provenance smart-cast) plus a repeated pre-test API36 ADB bootstrap failure. U12bp fixes all four compile defects and hardens API36 startup by excluding ADB state from the AVD cache, resetting the ADB server, waiting for a real `device` + `sys.boot_completed=1`, then disabling animations explicitly before the regression groups.

- U12bp corrective qualification dispatched after CI #887. Signing remains deferred until unit/lint/build and API36 both pass on this exact source.

- CI #888 API36 failure was workflow-shell compatibility, not app/test logic: the emulator booted and ADB became healthy, but `android-emulator-runner` executes its `script` via `/usr/bin/sh`; the injected block used Bash-only `[[ ... ]]` and `{1..30}`, causing exit code 2 before instrumented tests. U12bq converts that block to POSIX `sh` while preserving the explicit ADB/device/boot guards.

- U12bq corrective qualification dispatched after CI #888 shell-compatibility failure. Signing remains deferred until the exact source passes Android CI/API36.

- CI #889 did not reach build or API36: the U12bq source-part itself was syntactically corrupt, and the fail-closed materializer correctly stopped both jobs. The patch was regenerated from the exact prior→current workflow diff and its blob hash realigned; no app/runtime behavior changed in this correction.

- U12bq materializer repair requalification dispatched after CI #889; signing remains deferred until software gate and API36 both pass on the exact source.

- CI #890: software gate PASS; API36 still failed before tests because `android-emulator-runner` executes each `script:` line as an independent `/usr/bin/sh -c`, so the multiline `while ... done` was split and failed with `expecting done`. U12br moves all readiness logic into the repository-owned Bash regression script and leaves the runner with one command only. The new `DiagnosticSupportInstrumentedTest` was also added to the fail-closed API36 grouping so coverage remains complete once tests start.

- U12br corrective qualification dispatched after CI #890. Software gate was already green; signing remains deferred until API36 passes on the exact U12br source.

- CI #891: software gate PASS; API36 runner/ADB/readiness finally operated correctly and reached `compileDebugAndroidTestKotlin`. The only blocker was missing imports for `SourceSearchOutcome` and `SourceSearchTerminalState` in `UnifiedProjectShellInstrumentedTest`. U12bs adds those imports only; no production app behavior changes.

- Autonomous RC20 qualification loop requested by owner: continue fixing/monitoring Android CI until software gate + API36 are green, then run signed homologation on the exact qualified source and deliver that signed APK for physical validation.

## Unified program progress
- Stable product identity is governed by `PROJECT_IDENTITY.md` and D-090: private single-owner appliance, proportional gates, exact signed APK + digest-pinned worker freeze after physical acceptance, and maintenance only for a real trigger.
- Product-scope override (2026-09-21): legacy project/backup migration is retired. The owner has only two legacy projects and accepts recreating them manually. U9 is retired; U8/U10/U11 no longer carry H37/GBW/pre-unification migration obligations. Current unified-line save/reopen, schema evolution, backup/restore and integrity guarantees remain mandatory.
- U0 baseline/inventory is **PASS** at repository HEAD `dc3cb95093110270c494804aa03ef625dc3ceef2` with GBW reference HEAD `4724b030eabe289a5c2645e379181c0f9602d25d`.
- Evidence: `history/U0_BASELINE_INVENTORY.md` plus the machine-readable `integration/u0/inventory.json` and hash-locked synthetic fixtures.
- CI #669 supersedes the earlier H37 corrective sequence and is the latest signed DIGITAL PASS for the exact U1 source.
- U1 unified project/asset domain is **DIGITAL PASS** on canonical CI #669 at exact source `14b69271f2ae04529fa14475e1a34f2fdd864553`: unit, Lint, build, API 36 regression and signed-candidate identity all passed.
- Canonical CI #667 reached Kotlin compilation and exposed three U1 source errors: a non-`Nothing` unsupported-schema branch and two cross-module nullable smart casts. U1a corrects those compile boundaries without changing the domain contract.
- U1a/U1b compile and test-fixture correctives are absorbed into the #669 authority.
- Canonical CI #668 confirmed the U1a production source compiles, then found one invalid hexadecimal SHA-256 generated by the U1 portable-package test fixture. U1b replaced it with a deterministic real SHA-256 and CI #669 closed the gate.
- U2 unified shell/Home/navigation is **DIGITAL PASS** on automatic CI #675 at exact source `665c292931c0f2ed9fa1e7145818101c5d017222`, with old projects still entering Studio directly, explicit Prepare/Studio/Export surfaces, domain-derived badges and safe disabled U3 acquisition states.
- Automatic CI #670 proved the controlled push trigger and U2 materialization, then found one trailing comma in a Kotlin `when` branch before tests. U2a removes only that syntax defect; the corrected automatic gate is pending.
- Automatic CI #671 confirmed production compilation after U2a and found the new Android-module unit test using `kotlin.test.Test` instead of the module's JUnit 4 annotation. U2b corrects only that test import.
- Automatic CI #672 passed unit, Lint and build, then exposed viewport-dependent interaction in the new scrollable-project-creation instrumentation. U2c scrolls to each target through Compose semantics before interaction.
- Automatic CI #673 stopped at the diff-sanity preflight because the first U2c patch artifact ended with a blank context line. The normalized U2c package preserves the exact source delta and removes that packaging-only defect.
- Automatic CI #674 passed unit, Lint and APK build and executed 36 API 36 tests; its only failure identified `new-project-name` on the heading row rather than the editable field. U2d moves the semantic tag to the `OutlinedTextField` without changing product behavior.
- Automatic CI #675 closed U2: deterministic U2d materialization, unit, Lint, debug/release build and all 36 API 36 tests passed.
- U3 implementation ports GBW ranking/provider semantics behind `core:source` / `platform:source-android`; local SAF import and remote YouTube/SoundCloud/Bandcamp acquisition stage and validate media before the sole project-mutation boundary publishes an immutable managed `SOURCE_ORIGINAL`. WorkManager + persistent operation state provide progress/cancel/retry/process-recreation semantics; no separation is invoked in U3.
- U3 source acquisition is **DIGITAL PASS** on automatic CI #676 / run `35606018087` at exact source `a7af51bf6622b4eecc32308c091fbb5020a1d54b`.
- U4 separation integration is **DIGITAL PASS** at exact source `55ae7a14d99b710367ea7650cbf9f48298b90eba`: Android CI #686 / run `35620101527` PASS and U4 Cloud Integration Smoke #8 / run `35620101684` PASS. The real-cloud gate exercised the production `gbw-demucs` path and closed the previously pending cloud-auth/config + lost-response/process-death recovery boundary.
- U5 prepared backing/reference + zero-copy Studio integration is **DIGITAL PASS** at exact source `4bada624e68f8a55ff22830e1dc276c8d51af223`: Android CI #690 / run `35625349001` PASS. The gate covers deterministic five-stem backing render, shared-gain/alignment invariants, managed guitar + L/R references, explicit keep/update binding, idempotent reopen/retry, fail-closed corrupt stems, rebind Undo/Redo, recording preservation, save/reopen, master render after rebind, and API 36 Prepare → Studio handoff.
- U6 unified exports is **DIGITAL PASS** on Android CI #711 / run `35658467705` at exact source `6ff1988a8309b7174eeb328e0044d6b1d3167ff6`. The gate passed deterministic materialization, JVM/unit, performance evidence, Android Lint, APK build and the complete API 36 instrumented regression. C1 export convergence is closed: the exact source snapshot has no independent `SaveAndExportDialog`/parallel format chooser, no obsolete Studio-local export state or export entry points, and Home/Studio export actions route through `ExportEntryPointPolicy` to the canonical Export workspace. Primary WAV actions no longer expose implementation-detail wording; zero-transcode information remains secondary explanatory copy. The run was unsigned because signed homologation was not requested.
- C2 unified project shell/navigation is **CLOSED / DIGITAL PASS** on Android CI #713 / run `35660979142` at exact source `66cedad38875d6f7284180ef46d25d4e9049d287`. Prepare, Studio and Export share one adaptive `ProjectShellScaffold`; the bespoke Studio project top bar is removed; Studio transport remains directly beneath the shared shell; saveable workspace state is preserved across workspace switches; Home reopens the last/relevant project workspace; project-scoped Settings returns to its exact originating workspace across Activity recreation; and missing-project routes fall back safely to Home.
- C3 new-project/project-lifecycle coherence is **CLOSED / DIGITAL PASS** on Android CI #720 / run `35669035762` at exact source `f730e08cc242ed590c333727cde4c96883f7df07`. New Project exposes Search song / Import audio / Start in Studio as explicit intents; Search/Import create the guitar-study model without a template chooser, Studio-only creation retains Guitar/Blank templates, and the selected intent/name/template survive Activity recreation. Source replacement publishes only a validated new source, resets active Prepare-derived pointers without deleting protected prior media or Studio creative state, duplicate normalizes transient preparation ownership, rename preserves immutable project identity, and delete closes source/separation/background ownership before repository removal with explicit user-facing cancellation consequences.
- C4 progressive Prepare journey is **CLOSED / DIGITAL PASS** on Android CI #723 / run `35673758282` at exact source `0d7b05d0192cc415d328e415c517b3cad40dc209`. Source, separation, automatic reference preparation and Studio readiness form one progressive journey; accepted acquisition controls collapse behind “Trocar fonte”; accessible ranking retains its numeric score; raw job/asset details stay in diagnostics; six validated stems automatically start deterministic references; lifecycle-aware durable observation replaces Compose polling; and all five API 36 groups passed after the guide assertion was made viewport-independent.
- Product-cohesion audit `docs/history/UNIFIED_PRODUCT_COHESION_AUDIT.md` is **CLOSED / DIGITAL PASS through C8**. Android CI #781 / run `35791192802` on exact technical source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63` proved the terminal U10/C8 gate with 23/23 instrumented classes, 76 observed API 36 tests and 24/24 retained cohesion screenshots; the post-hardening visual review passed without ANR/system-overlay contamination. Full evidence: `docs/history/U10_FINAL_DIGITAL_COHESION_GATE.md`.

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Release profile: private single-owner personal use. RC20 gates are proportional to credible impact on data/media integrity, musical quality, primary acquisition, quota/cost, credentials, recovery and signing. After acceptance, the signed APK and digest-pinned worker are frozen; optional diagnostics/matrices and scanner-database churn do not trigger maintenance by themselves (D-090).
- **RC19 is DIGITAL PASS / SIGNED but PHYSICAL ACCEPTANCE BLOCKED by a release-blocking Prepare audio-quality incident.** The authoritative incident/investigation plan is `RC20_COMPLETION_PLAN.md`. No separation-algorithm correction is authorized before read-only job forensics and local audio forensics identify the failing boundary.
- RC19 target identity: `0.5.0-rc19` / versionCode `39` / `studio.guitarlab.app`. Scope: authoritative same-generation discovery/adoption, explicit effective remote job identity, typed active-job conflict vs monthly quota, safe cleanup of unregistered rejected uploads, process/reboot recovery probe and U4 real-cloud recovery/idempotency proof.
- Current source-materialization tail is **U12bi** via `scripts/materialize_ci_sources_u12bi.py`. RC19's U12an recovery chain remains historical evidence inside that protected chain; U12bh makes stochastic Demucs deltas diagnostic, and U12bi corrects only the W4 matrix-alias versus canonical-engine-strategy validator contract.
- RC19 branch preflight: U7 Cloud Backend #121 / run `36014205313` PASS on exact branch source before this documentation-only CI trigger; Functions/worker tests, schemas/scripts/secret hygiene and non-publishing worker-container build passed. This is pre-merge evidence only, not final main authority.
- RC19 Android branch gate: Android CI #842 / run `36014520133` PASS at exact source `07decfcbde8ced31d15ed31811fcc2e234eaf245`: deterministic U12al materialization, unit tests, Android Lint, debug/release build and API 36 instrumented regression passed; signing was intentionally skipped. A docs-only exact-source re-gate now triggers Android CI and U7 verify together so both authorities bind to one final branch SHA before merge.
- RC19 branch exact-source re-gate: Android CI #843 / run `36016258580` PASS and U7 Cloud Backend #123 / run `36016258158` PASS on exact SHA `f0cf15a6e07aa1db1e617e239996af8302372f05`. That SHA was then fast-forwarded unchanged into canonical `main`; the next docs-only commit exists solely to bind canonical-main CI/U7/auth evidence.
- RC19 canonical-main U7 verify/auth gate: U7 Cloud Backend #125 / run `36018323409` PASS on `441be74ea31e6c7ecec4ec49151eaba024a08edb`; Android CI #845 is the paired canonical-main gate. A separate U12an operational branch adds fail-closed commit-marker dispatch for shadow/production because the connected GitHub tool does not expose workflow-dispatch inputs; deploy remains restricted to canonical `main`, production additionally requires `[confirm production]`, and the legacy manual dispatch contract remains available. U12an also covers the preserved-fixture case where same-generation recovery must remain non-destructive even if unrelated active work exists for the same UID.
- U12an operational preflight U7 #127 / run `36019723579` failed before any deploy at shell syntax validation because the first U12am wrapper indented Bash heredoc terminators. The corrective keeps heredoc delimiters at column zero, updates the U12am patch/hash lock, and retains the same shadow/production semantics. No production resource was mutated by the failed preflight.
- U12an corrected branch gates: U7 Cloud Backend #128 / run `36019921423` PASS and Android CI #848 / run `36019921739` PASS on exact SHA `96c27a03b838878a14fc767a05811e95300ab074`; the same SHA was promoted to canonical `main`, where Android CI #849 and U7 #129 both PASS. U7 #129 executed **shadow** only: digest-pinned `guitarlab-demucs-shadow`, runtime contract PASS, real prepared-reference v2 smoke PASS (`guitarlab-demucs-shadow-7v74l`), source/ACK/PURGED cleanup PASS; production Functions/rules were skipped and the pre-cutover production image remained `sha256:fb6cd3e5a97e858184058bdcbaaa9676e631935137c3c0424c409f5ab3ba5172`.
- RC19 production cutover: U7 Cloud Backend #130 / run `36024185442` PASS on canonical `main` SHA `f0f743c3c311a4332175b15737cf6a661be07f55`; production worker digest deployment, Firebase Functions/rules deployment, runtime verification and real U4 smoke all passed. `findRecoverableRemoteSeparation` was created successfully. The production recovery gate proved same-generation COMPLETED adoption with **quota unchanged**, **no second Cloud Run execution**, retry-upload cleanup, and then the normal ACK/PURGED path; real production smoke execution `gbw-demucs-lcslf` passed prepared-reference v2 integrity/cleanup. Deploy evidence artifact: `u7-cloud-deploy-f0f743c3c311a4332175b15737cf6a661be07f55-production`, artifact id `10819698734`, artifact digest `sha256:783af8ec21c201ac7b388c61bf08711454cd00c29edefdcff28866522984d2a4`. No rollback was required.
- RC19 candidate freeze: production qualification is green and no further source/backend mutation is authorized before physical homologation evidence. The following docs-only commit triggers the final signed Android gate for `0.5.0-rc19` / versionCode `39`.
- RC19 signed homologation candidate: Android CI #852 / run `36026458960` PASS on frozen source `e0a8a2218accbe9c8eec413a40f4fedf7d9da9bc`; unit tests, Lint, release build, API 36 instrumented regression and signing all passed. Signed artifact `GuitarLabStudio-0.5.0-rc19-homologacao`, artifact id `10820021943`; package `studio.guitarlab.app`; versionCode `39`; signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`; signed APK SHA-256 `20c6a49efa69828653862c29c7907cd734bd2e3f62235295cd53b7f7c571c50b`. Physical homologation remains pending and must install over RC18 without clearing app data to exercise preserved remote-job recovery.
- RC19 post-cutover independent U4 qualification: U4 Cloud Integration Smoke #160 / run `36026359099` PASS on `5be450f514f19d37d9c9e8ebe7902bbdf478eb49`; prepared-reference v2 manifest/integrity PASS; same-generation COMPLETED adoption PASS with quota unchanged, no second Cloud Run execution and retry upload cleanup; production execution `gbw-demucs-mb8ww` PASS with source cleanup, ACK, Firestore IMPORTED/PURGED and whole-prefix purge idempotency. The freeze-triggered U4 #161 was correctly skipped.
- RC19 physical incident root cause (2026-09-24): F1 proved fresh Cloud Run execution `gbw-demucs-gbrt9` on immutable image `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`; F2 reproduced unusable output with a second song; F3 retained canonical input and all six stems. The canonical source is good, while every `demucs.cpp`/GGML stem is musically unusable, acquires systematic negative DC and concentrates most content incorrectly in `other`. A per-stem 10 Hz DC blocker removed the offset but did not repair separation by listening. Android publication, Firebase transport and the v2 renderer are not the originating defect. The consolidated GBW Linux reference proves its known-good Demucs path uses the official PyTorch `htdemucs_6s` CLI (`float32`, clip none, shifts 1, overlap 0.5), not `demucs.cpp`. Phase W in `RC20_COMPLETION_PLAN.md` now owns worker reconstruction. Performance tiers are <=5 min optimal, >5–15 min acceptable and >15 min alert/review. D-092 explicitly waives the additional RC20 W5 owner-listening run while preserving all hard technical audio-quality and integrity gates.

- U12/RC20 Phase W0 baseline capture (2026-09-24): worker reconstruction starts from official Demucs/PyTorch, not `demucs.cpp`. The GBW v5.23 archive was inspected without persistent extraction and confirms `htdemucs_6s --float32 --clip-mode none --shifts 1 --overlap 0.5 -d <cpu|cuda>`. The official checkpoint is `5c90dfd2-34c22ccb.th`, 54,996,327 bytes, SHA-256 `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd`. The current committed ZIP hashes to `f56eee55c71a88083bbfb2651fc65df2b9986aafc9bb217b24b6140f1c421ea6`, differing from the handoff-declared `ca4e0b1b...`; the archive remains untouched and the discrepancy is tracked explicitly. W0 is not yet declared closed until fixture/adapter validation is committed.
- U12/RC20 worker reconstruction status (2026-09-24): W1/W2/W3/W4 are digitally closed for the selected CPU8 shadow shape. U7 #158 / run `36078452293` PASS on exact producer `90428c81feca5eea4593ab384280846bd1175ccb`; backend/auth, dependency closure, SBOM/Trivy gate, runtime contract, real prepared-reference v2 smoke, corrected W3 and W4 all passed. Finalist shadow image: `sha256:6a6d5017e0e2c9bf3800bca3a3c3988ed9398add4ef2c3097241a7a260893dbe`. W4 CPU8 metrics: `totalMs=280648`, inference `100839 ms`, warm inference `89875 ms`, render `84971 ms`, max RSS `127600 KiB`, estimated cost `$0.049394048`, `other` energy share ~14.39%, and zero quality rejects. Artifact `u12-rc20-w4-shadow-90428c81...` / id `10842185288` is SHA-bound to that producer. The technical 180 s fixture remains synthetic. D-092 records the owner's explicit decision to waive the additional W5 real-source listening gate; this is a waiver, not a claimed W5 PASS. CPU4 remains optional. U12bj rebuilds the worker only to extend the fail-closed diagnostic namespace, so its new digest must pass proportional shadow requalification before W6. Production remains on pre-RC20 digest `a70bd221…` until that qualification is green.
- RC19 Prepare search finding (screen recording, 2026-09-24): query artist `memphys may fire`, song `misery` enters `Pesquisando fontes compatíveis…` for ~15 s and then returns to idle `Pesquisar fontes` with no persistent result, zero-result copy, provider failure, retry guidance or correction suggestion. This is release-blocking UX/reliability. The corrective plan now requires explicit terminal search states and a bounded `Você quis dizer…` path using existing `SourceSearchRules.normalize/textSimilarity/Levenshtein` plus provider candidate metadata; no LLM or external spelling service is required.
- Owner-requested RC20 completion scope (confirmed 2026-09-24): after the PyTorch backend and primary search flow are qualified/homologated, the project must still deliver before final freeze: **Recolocar referências no Studio** (local/idempotent binding repair), grouped Prepare technical details, **Configurações → Diagnóstico**, a bounded sanitized persistent event journal, and **Exportar pacote de diagnóstico** ZIP. These features are committed scope but do not block W5/W6 or the backend production cutover. Extended cloud observability, exhaustive matrices and DID_YOU_MEAN polish remain optional unless needed by a blocker.
- RC19 production cutover: U7 Cloud Backend #130 / run `36024185442` PASS on `f0f743c3c311a4332175b15737cf6a661be07f55`. Production worker advanced from `sha256:fb6cd3e5a97e858184058bdcbaaa9676e631935137c3c0424c409f5ab3ba5172` to `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`; Functions/rules deploy PASS; runtime contract PASS; real U4 execution `gbw-demucs-lcslf` PASS. Same-generation recovery explicitly passed with quota unchanged, no second Cloud Run execution, retry upload cleaned, then real ACK/PURGED cleanup passed. Automatic rollback was not required.
- RC18 identity: `0.5.0-rc18` / versionCode `38` / `studio.guitarlab.app`; producer `ee947c6fbbd8b24430d3a3faa04e27ef9a46856c`; signed homologation artifact `GuitarLabStudio-0.5.0-rc18-homologacao` / artifact id `10804182567`.
- RC18 scope: retain RC16 signal validation, canonicalization and Studio synchronization; remove the partitioned Demucs executable proven to emit non-finite samples; pin and enforce continuous `single8` inference end to end; keep sampled reconstruction SNR diagnostic after the RC17 shadow proved it can falsely veto valid finite Demucs output.
- RC18 exact-source digital evidence: Android CI signed run `35990311091` PASS; U7 verify run `35990311054` PASS; isolated shadow run `35991484767` PASS; controlled production deploy run `35992709966` PASS; U4 production smoke run `35994148711` PASS.
- RC14 is the latest physically tested fallback at producer `13c6f36e5e3c2bcf82cf21f885e8d7aa6c41ed34`: Android CI run `35937621047` passed unit, Lint, build, API 36 and signing. Its cloud-identical predecessor `9b4ee9f` passed U4 `35935229248` and U7 `35935229232`.
- RC13 signed artifact: `GuitarLabStudio-0.5.0-rc13-homologacao` / artifact id `10779803277`.
- RC14 release notes and physical checklist: `history/RELEASE_NOTES_0.5.0-rc14.md`, `history/RC14_FINAL_PHYSICAL_HOMOLOGATION.md`.
- First RC14 qualification at `9b4ee9f`: software gate PASS, U4 `35935229248` PASS and U7 `35935229232` PASS; its only API 36 failure was a superseded test target. The test-only correction was subsequently sealed by signed Android run `35937621047` on `13c6f36`.
- RC14 physical evidence then exposed two v2 integration defects: Storage rules omitted `output/prepared/*`, corrected by production ruleset `525c392a-2aeb-48f3-8685-e55f13c9d528`; and Android rejected valid extensible float32 WAV at `WavStructure.read`, corrected in RC15. Job `66464e42-937f-4d78-a5bd-dabc7c9687f9` remains the direct resume-import acceptance fixture.
- RC15 physical homologation exposed missing signal-level acceptance and stale binding/empty-clip recovery. RC16 added the corresponding safeguards, but U7 shadow run `35984546104` failed closed with `OUTPUT_INVALID: non-finite sample in stem reconstruction`. A same-input/model A/B reproduction measured 154,350 non-finite samples in every `mt4_omp2` stem and zero in every `single8` stem. RC17 removed the defective binary, but isolated shadow run `35988775722` then failed on `stem reconstruction quality is unsafe: -1.875 dB SNR`; Cloud Run logs proved the sequential output was finite and the failure was the SNR-by-sum acceptance rule, not Demucs corruption. RC18 keeps that score as diagnostic and leaves hard publication gates on objective format, finiteness, duration, peak and prepared-reference checks; its isolated shadow, production deploy and U4 production smoke all passed.

## Superseded rc11 authority
- **rc11 is CLOSED / DIGITAL PASS and historical.**
- Exact producer: `34cb60624b2fabf21cfe2c60003b04eac1597418`.
- Version/package: `0.5.0-rc11` / versionCode `31` / `studio.guitarlab.app`.
- Android CI #806 / run `35885021871`: PASS — exact materialization through U12w, unit tests, Android Lint, debug/release build, full API 36 grouped regression and signed homologation.
- U4 Cloud Integration Smoke #123 / run `35885021902`: PASS — controlled real six-stem Cloud Run contract on the same producer.
- U7 Cloud Backend #81 / run `35885021969`: PASS — Firebase Email/Password auth enforced, anonymous auth disabled, allowlisted password-backed Firebase users verified, backend source/container/security verification PASS; controlled deploy step skipped.
- Signed artifact: `GuitarLabStudio-0.5.0-rc11-homologacao` / artifact id `10762204176`.
- Signed APK SHA-256: `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`; size `79,965,840` bytes.
- Signed artifact ZIP digest: `sha256:9f9ad8e16222c4eac1d8dcc27d7946e75e54be6a9a9bfcd243beb0284e14e7b2`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- rc11 supersedes rc9/rc10 after target-device diagnostics proved the cloud worker was retrying before Storage/Functions/Cloud Run because the migrated client authentication model no longer matched the backend's stable UID allowlist. The original GBW contract used Firebase Email/Password with a stable personal UID; rc11 restores that contract without storing the password in GuitarLab.
- Remote separation now requires a stable authenticated Firebase account before enqueue, validates that account against the backend, types Firebase/Auth/Firestore/Storage/Functions failures by pipeline stage, retries only transient failures and surfaces retry/terminal state explicitly.
- Activity lifecycle is hardened: deleting/missing projects terminalizes stale active history, and `Limpar resolvidos` removes only `SUCCEEDED`/`CANCELLED`, preserving failures and unresolved active work.
- U12 is **READY / PHYSICAL PASS PENDING** on rc11. Existing unrelated physical evidence remains reusable under the U12 invalidation rule.
- CI supports controlled `[run ci]` / `[run ci signed]` / `[run u4 cloud]` / `[run u7 cloud]` triggers on `main`; ordinary docs commits remain `[skip ci]`.

## U12 rc11 candidate evidence
- exact producer: `34cb60624b2fabf21cfe2c60003b04eac1597418`;
- Android CI #806 / run `35885021871`: PASS;
- U4 Cloud Integration Smoke #123 / run `35885021902`: PASS;
- U7 Cloud Backend #81 / run `35885021969`: PASS;
- source materialization tail: U12w, fail-closed hash/reverse-apply guards PASS;
- API 36 regression: PASS including the explicit Email/Password cloud-account Settings surface;
- signed artifact: `GuitarLabStudio-0.5.0-rc11-homologacao` / artifact id `10762204176`;
- signed APK SHA-256: `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`;
- signed APK size: `79,965,840` bytes;
- artifact ZIP SHA-256: `9f9ad8e16222c4eac1d8dcc27d7946e75e54be6a9a9bfcd243beb0284e14e7b2`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- final target-device acceptance remains pending under `docs/RC20_PHYSICAL_HOMOLOGATION.md`.

## U12 rc9 historical corrective evidence
- exact producer: `635124acfbf133553a96c8b2013f2245f58a6877`;
- Android CI #794 / run `35856278980`: PASS;
- U4 Cloud Integration Smoke #111 / run `35856278976`: PASS;
- U7 Cloud Backend #69 / run `35856279117`: PASS, deploy skipped;
- source materialization tail: U12k, fail-closed hash/reverse-apply guards PASS;
- unit coverage includes remote-missing `CANCEL_REQUESTED`, `RUNNING`, `COMPLETED` and `IMPORTING`, bounded cancel failure, source-generation ownership and stale-terminal-vs-new-active selection;
- API 36 includes recovered-orphan retry UI with accepted source preserved and raw backend error hidden;
- signed artifact: `GuitarLabStudio-0.5.0-rc9-homologacao` / artifact id `10748475073`;
- signed APK SHA-256: `4fcf529b935a584217b3b882ca8dfa05e3c360f99cfe9d70071080175439dd6c`;
- signed APK size: `79,945,360` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- final target-device acceptance remains pending under `docs/RC20_PHYSICAL_HOMOLOGATION.md`.

## U12 rc8 historical corrective evidence
- exact producer: `68ddfd98ad61bd32f412872ab0332a7f6e97b60e`;
- Android CI #793 / U4 #110 / U7 #68: DIGITAL PASS;
- rc8 is withdrawn from final U12 approval because physical testing proved that a local active record could itself be orphaned from the backend and remain stuck in `CANCEL_REQUESTED`.

## U12 rc7 replacement-candidate evidence
- exact producer: `74274dd51ad75a7d4b9e15a82fe4b64ba448c498`;
- Android CI #792 / run `35810108338`: PASS;
- exact source snapshot, unit, performance evidence, Android Lint, debug/release assembly and API 36 regression: PASS;
- signed homologation job: PASS;
- signed artifact: `GuitarLabStudio-0.5.0-rc7-homologacao` / artifact id `10729009613`;
- signed APK SHA-256: `142b892b375d495df90030806843e66a2f884a0e1023aefad183d9fe46304e44`;
- signed APK size: `79,937,168` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- final physical acceptance remains pending under `docs/RC20_PHYSICAL_HOMOLOGATION.md`.

## U12 rc6 replacement-candidate evidence
- exact producer: `d0926e9dbd231b6d91c19448279fe8749d182ba1`;
- Android CI #789 / run `35807008698`: PASS;
- unit, performance evidence, Android Lint, debug/release assembly and exact unsigned provenance: PASS;
- API 36 grouped instrumentation: PASS across all configured groups/classes;
- signed homologation job: PASS;
- signed artifact: `GuitarLabStudio-0.5.0-rc6-homologacao` / artifact id `10728840806`;
- signed APK SHA-256: `4bc76565c74366d89df23db2e6410e77976a1cc827a79486ceec629fd16112b6`;
- signed APK size: `82,894,480` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- physical acceptance remains pending under `docs/RC20_PHYSICAL_HOMOLOGATION.md`.

## U11 closure evidence
- exact frozen producer: `4218e4343746932a4de61c5abaa29ba5769a30ed`;
- Android CI #783 / run `35793456972`: PASS;
- software gate, Android Lint, debug/release assembly and exact unsigned provenance: PASS;
- API36: 23/23 classified instrumented classes, 76 observed tests across 5/5 groups PASS;
- screenshot matrix: 24/24 PASS;
- target-tablet geometry: 1920×1200 PASS;
- signed homologation: PASS;
- U4 Cloud Integration Smoke #104 / run `35793456955`: PASS, six-stem real Cloud Run contract + cleanup;
- U7 Cloud Backend #62 / run `35793456941`: PASS, tests/schema/script/secret-hygiene/container build; deploy skipped;
- unsigned APK SHA-256: `6af047a1f40b0e08382a5cca74890bc186aad68a3207a14e20a87cde5df9bc2c`;
- signed APK SHA-256: `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- detailed evidence: `docs/history/U11_FINAL_DIGITAL_RELEASE_GATE.md`;
- physical campaign: `docs/RC20_PHYSICAL_HOMOLOGATION.md`.

## U10 / C8 closure evidence
- exact technical source: `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`;
- canonical CI: Android CI #781 / run `35791192802` — PASS;
- software gate: PASS, including Android Lint with 0 errors and debug/release build;
- API 36: 23/23 classified instrumented classes, 76 observed tests across 5/5 groups;
- screenshot matrix: 24/24 required artifacts retained;
- target-tablet geometry: 1920×1200 PASS;
- deterministic visual review: PASS, with no ANR/system overlay contamination;
- Android integration artifact digest: `sha256:1c355f88bef4e20393a5c26c39fb4db94da8383089023724988af55953f509e3`;
- exact-source artifact digest: `sha256:e17472db3fc95fe2ae2e49c56571f06f72a0465c4fbbeecb0ad14e7c0d02210f`;
- signed homologation: intentionally skipped; U11 owns the next signing event;
- detailed evidence: `docs/history/U10_FINAL_DIGITAL_COHESION_GATE.md`.

## U8m / U8 closure evidence
- exact technical source: `2375dcb72983376cb486eccf41faf1734633cc94`;
- software CI: Android CI #757 / run `35753982993` — PASS;
- real-provider result: `U8m PASS · r_1790095960 · cleanup 8/8/14`;
- sanitized report SHA-256: `84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`;
- acceptance used a dedicated Android OAuth client for the debug signing identity solely to authorize the controlled real-Drive campaign; this does not supersede the official signed candidate identity;
- provider-real U8m evidence remains supporting authority; the latest signed authority is now U11/CI #783.

## U5 digital evidence
Canonical U5 source: `4bada624e68f8a55ff22830e1dc276c8d51af223`.

- deterministic materialization through U5c PASS;
- **378/378 JVM/unit tests PASS**, 0 failures/errors/skips;
- Android Lint PASS with **0 errors**;
- debug APK assembly PASS;
- **38/38 standard API 36 instrumented tests PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- prepared references are project-managed and Studio consumes them without user-visible copy/reimport;
- new preparations do not silently replace existing Studio bindings;
- save/reopen, Undo/Redo, recording preservation and master-render-after-rebind are regression-covered;
- detailed gate evidence: `docs/history/U5_PREPARED_REFERENCE_GATE.md`.

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
The authoritative plan is `history/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Before stable 1.0.0, the #663 baseline is digitally closed; only the remaining hardware-only H29/H30/H32/H35 residual must now be physically closed:
- recording synchronization/session-health hardening;
- interrupted-recording recovery UX;
- USB audio disconnect/reconnect resilience;
- takes-management refinement;
- song/session quality and stress coverage through 10 minutes;
- diagnostics refinement;
- H28 backup/restore regression hardening;
- final UX/accessibility polish.

Within the H29-H37/1.1 closure line, external MIDI/footswitch control remains the only added feature family; the exclusions in `history/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md` continue to protect that line from scope creep.

A later product decision now approves a **separate successor program** that unifies GBW Android into GuitarLab. Its authoritative plan is `history/UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`. The unified program does not retroactively change H29-H37 evidence or current release identity. It is designed to carry the still-unproven hardware residual into one consolidated final unified-candidate physical campaign whenever technically possible, while maximizing digital validation before that campaign.


## U3 source acquisition — DIGITAL PASS
Implementation candidate prepared on top of the exact U2d materialized source. The U3 source block is SHA-256 locked and reconstructs:
- pure provider-neutral ranking/normalization in `core:source`;
- Android source acquisition in `platform:source-android` with explicit INTERNET permission;
- local SAF staging and supported-format validation while preserving the original file as authoritative;
- Bandcamp discovery plus yt-dlp YouTube/SoundCloud discovery/download with bounded retry classification;
- duration/integrity validation before publication, including partial/wrong-duration rejection;
- `SourceAssetPublisher` as the sole project mutation boundary, publishing AUTHORITATIVE/MANAGED `SOURCE_ORIGINAL` with SHA-256 and provenance;
- operation-id idempotency, stale-source conflict rejection and rename-safe reread-before-save publication;
- persistent WorkManager progress/cancel/retry state surfaced in Prepare as the U3 activity surface;
- active New Project Search/Import entry points and API36 Compose regression coverage.

Pre-gate evidence: clean U2d→U3 materialization PASS, terminal Git-blob verification PASS, repeat/idempotent materialization PASS, reverse dry-run PASS and `git diff --check` PASS. Canonical automatic CI #676 then closed U3 at exact source `a7af51bf6622b4eecc32308c091fbb5020a1d54b`: 358/358 JVM/unit tests PASS; Android Lint PASS with 0 errors (58 warnings + 4 hints in app, plus 1/1/2 warnings in source/codec/audio platform modules); debug APK assembly PASS; 37/37 standard API36 instrumented tests PASS; 1/1 isolated target-tablet geometry PASS. The signed homologation job was intentionally skipped for this intermediate milestone.
