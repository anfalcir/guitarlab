# Android CI / release pipeline

Updated: 2026-09-23

## Contract
`.github/workflows/android-ci.yml` supports manual `workflow_dispatch` and controlled `main` commit triggers. A commit containing `[run ci]` runs the software and API 36 gates; `[run ci signed]` additionally produces the signed homologation APK. Ordinary commits retain `[skip ci]` and do not consume hosted CI. This workstream is authorized to use and monitor the controlled triggers while executing the unified roadmap.

Current release phase: **U12 ACTIVE**. RC16 (`0.5.0-rc16` / 36) is the active unqualified physical-corrective source. RC14 is the latest signed digital authority at producer `13c6f36e5e3c2bcf82cf21f885e8d7aa6c41ed34`, Android run `35937621047`. RC16 may replace this authority only after exact-source Android/U7 qualification, controlled production worker deployment/U4 and affected physical acceptance pass.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Current source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact SHA-256/Git blob checks.

The canonical entrypoint materializes the accepted source chain and the workflow additionally binds the exact candidate checkout/source identity. Historical tail labels describe their materialization block, not the current release version; qualification still requires exact source identity, `git diff --check`, semantic guards and the canonical software/API36/signing gates.

Historical U10/C8 source authority remains Android CI #781 and historical materializers remain preserved as evidence; they do not override the current `scripts/materialize_ci_sources.sh` tail.

U8m then completed the separate provider-real acceptance gate through the same
production vNext Drive transport/store. Result: `U8m PASS · r_1790095960 · cleanup 8/8/14`; sanitized report
SHA-256 `84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`. This provider gate authorizes closure of U8 but is not a
signed release promotion. RC14/Android run `35937621047` is the latest signed authority for the U12 physical campaign.

U10/C8 closure is recorded in `docs/U10_FINAL_DIGITAL_COHESION_GATE.md`. The
#781 integration artifact digest is
`sha256:1c355f88bef4e20393a5c26c39fb4db94da8383089023724988af55953f509e3`;
its exact-source artifact digest is
`sha256:e17472db3fc95fe2ae2e49c56571f06f72a0465c4fbbeecb0ad14e7c0d02210f`.
These are unsigned technical/cohesion authorities only.

The previously accepted historical materializers remain preserved as evidence.
For current builds, never infer the active source from an older H-series tail
description; follow `scripts/materialize_ci_sources.sh` and verify the exact
terminal Git blobs declared by its current U-series stage.

H28 input remains `.source-parts/H28BackupIdentityConsistency.patch.gz.b64` with gzip SHA-256 `1abd8101361b241dfb443950c2a635141b41fecca77442d86343eb3f3025d3be` and decoded patch SHA-256 `3d06aa851ad1dc88dd078d60bb24ea097bbca7ef4f3e93089f47c9bd4a0a6e71`.

New tail inputs:
- H29 `.source-parts/H29RecordingSessionHealth.patch.gz.b64` — gzip `2f5dff49aeeb271aadf4992f85f0dbf451a75f20fc4c72a750f09f993d8cc4ab`, patch `229998a0fb2198d8ecf82cba2f48eb7d66929ddf7e8b9398d6b017d8c04d0640`;
- H30 `.source-parts/H30RecoveryUsbResilience.patch.gz.b64` — gzip `4b72aead443f21b2f61055a73e22d5c1e524afb368d74773d61c1b7dfd2463b1`, patch `26d4408812d7d8382b3e52d6542dce369071a8eb278ab8c240745e09c802b2bd`;
- H31 `.source-parts/H31TakesDiagnostics.patch.gz.b64` — gzip `a90c1577bbb17c943742b5ca6b654f0de845f9bdd654d4a604b5d60241807630`, patch `0031367ba9b61057524056540a9b57a29aecb65cd4eb2d25854057ab170eda1d`;
- H32 `.source-parts/H32TenMinuteQualityGate.patch.gz.b64` — gzip `41c5b3aba13e70b688b630bdbc5035ae92905257bd01db61b2cb6f69ba2e6435`, patch `63d62e3734ec0081b309bceffef05bd18cbc7c6d77ad4fc695a5534c8c3e5fb4`;
- H33 `.source-parts/H33ExternalControl.patch.gz.b64` — gzip `d88a320ba2323ebac7a9db24d1f9314af270cc595d209d1bbfdfefc81be0f6a9`, patch `46880223ffd10711bbf660feedb705ea9c3fa6cd6a111017bd3b6ad32e7172dc`;
- H33a/H33b — external-control CI correctives included in signed #657;
- H34 `.source-parts/H34FineLatencyRange.patch.gz.b64` — gzip `d7ea906f7300d7b00c8a384ac945e140f386e7faf0023dad1f0672b8cda17bad`, patch `4b47bdc21b58c68e5ca9616314dca495ca5c5f5b72a1be76d25c1dc9116565c0`;
- H35 `.source-parts/H35TakeSyncQuietCalibration.patch.gz.b64` — gzip `38cc7f6ab3098ad253a3584d6e96a1780d7b316f8d8506fab981597a6d050a90`, patch `3a255b8bad4ce79463293e290ad36d66d0031983d30b443cf07441353d286043`;
- H35a `.source-parts/H35aLintPermissionCorrective.patch.gz.b64` — gzip `eb766693ffdf31c611ea038ba70ce46378b81d067a27f092cc40e960c2de8bb7`, patch `8fb5be6b03b6f8897ddb78dc38ff1c1ad565767160d11e3cba5007f4320b9179`;
- H36 `.source-parts/H36SettingsUxPolish.patch.gz.b64` — gzip `22895aa6e1d39a3c6988f503467e763893b0448ca1b087ba23614a1a2b162eaf`, patch `9489fe121cbbc6c43bf9675701cd74c2556c7d8c1461c48030745711c1b54852`;
- H36a `.source-parts/H36aSettingsTestImportCorrective.patch.gz.b64` — gzip `949c740dc67545016a27652630ed1e6504f76ae9322feaecd554191ee5753c22`, patch `f5c8b3acdbd87ce4bbb3ffa8b95be3af018b3bb4df7acc27ff0fd0794a03686a`;
- H36b `.source-parts/H36bSettingsScrollTestCorrective.patch.gz.b64` — gzip `ac06c0d951355e2d0885203e010509bbb1aade05af173e6ba848a47e1184f692`, patch `a78b88bc6545a98fd109c353fe68910931897290f5feb93a0ebd648055dd3ee1`;
- H36c `.source-parts/H36cSettingsCalibrationSemanticTagCorrective.patch.gz.b64` — gzip `9742596963536b5b9c55d59653ea68e1542bad8953f73028eff159736756b505`, patch `279ef2e51579bfd706a3071300ddf245fa06a4f0d22e9b868907a5c38bf877f7`.
- H37 `.source-parts/H37DriveV3Backup.patch.gz.b64.part00` … `.part04` — reconstructed gzip `253953752a421a5b2299a040024c7f897940cfaa7de17aec914bcc2793ce9000`, decoded patch `d4d7da1d1a097c9451644d78b92cc10dae6faf35b523b0339cce906ab01007a2`.
- H37a `.source-parts/H37aDriveCompileCorrective.patch.gz.b64` — gzip `77f5d9ad6f5ebced2e9763dc13ed37a7ac7b4da3412f40c5f6498e813e5b4565`, decoded patch `5b623309e98c2b8f79434db437068ec80f197f855ec7efc8b5f8eb45b3163cae`.
- H37b `.source-parts/H37bDriveTokenCacheHardening.patch.gz.b64` — gzip `e9e79f2c46e47f0e04ccf0f9aaead83908afa04cb3f99c81bbd3438d250cc824`, decoded patch `9ebfb1e69a6ad4e888e9782c07d54f9566630814ba40b8f6163e82727bc85c5d`.
- U8j `.source-parts/U8jPathAwareProjectSnapshots.patch.b64` — payload SHA-256 `ef6abfca3cd26c381352d8c6fbca9d528757096c100bd706b5c62bad11dac210`, decoded patch SHA-256 `5823349c252dccf7379b4fe7a05b811dcde5f413ca573610dbf22553f6de95ea`; exact terminal blobs are declared by `scripts/materialize_ci_sources_u8j.sh`.
- U8k `.source-parts/U8kProductionDriveCutover.patch.b64` — payload SHA-256 `8a508068433bc01c254a0d54ff74ebfe0e9df60e76ed264474838043cf81469a`, decoded patch SHA-256 `4d8b59089c4d512986ddab5e8d740ad978374ae2ded31f801892f17c440d1418`.
- U8l `.source-parts/U8lNetworkFaultGate.patch.b64` — payload SHA-256 `5ceb06ee612f437cd8fe2b2bf870e25745fb56e6d4cd3f30f43ccce1d2d67bae`, decoded patch SHA-256 `6b21db17a22ccbc29d108b0136d26751769be7f1ae346107b44d735d79460c27`; exact terminal blobs are declared by `scripts/materialize_ci_sources_u8l.sh`.; exact terminal blobs are declared by `scripts/materialize_ci_sources_u8k.sh`.

Expected terminal message for the current source candidate: `Source patch chain already materialized through U8k with payload/reverse verification` on an already-materialized checkout, or `Source patch chain materialized through U8k with exact blob/reverse verification` when applying the chain.

Pre-publication source proofs on the exact H28 baseline:
- first H28→H33 materialization PASS;
- second invocation PASS/idempotent;
- deliberate H33 source corruption rejected with nonzero exit before ready-state acceptance;
- each H29-H33 block verifies both archive/decoded-patch SHA-256 and exact final Git blob hashes.

These pre-gate proofs were exercised by the canonical #659 workflow on the exact H35a source.

## CI #664 failure and H37a compile corrective
Manual CI #664 / run `35544867278` on H37 producer `abecc73e4eab181a7776d98cc731758b17c64b06` materialized H37 successfully, then failed during `:app:compileDebugKotlin` in both parallel jobs before Lint, APK assembly or signed homologation.

Compiler findings:
- four internal-coroutines API diagnostics in `DriveAuthorization.kt` around `tryResume/completeResume`;
- one return-type mismatch in `DriveV3Protocol.kt`: expected `DriveHttpResponse`, actual `Unit`.

H37a addresses only those compile issues. Local evidence before publication:
- targeted Kotlin/coroutines compile probe PASS;
- clean H37 → H37a apply PASS;
- terminal blobs `DriveAuthorization.kt=be34ca749b70e33eac826eb904a71679796e4c5d` and `DriveV3Protocol.kt=0a8b5dc97f8c3760cb3956f22fd80a7b413fe5eb` PASS;
- actual H37a materializer first run PASS;
- second run idempotent PASS;
- corrupted H37a source part rejected before source mutation PASS.

CI #664 remains failed evidence and must not be promoted. A fresh manual workflow run is required for H37a.

## H37b token-cache hardening
H37b is a minimal OAuth resilience block on top of H37a. On HTTP 401, the token provider now clears the rejected access token through Google Identity Services `AuthorizationClient.clearToken(ClearTokenRequest)` before reauthorizing. This prevents the next request from receiving the same rejected token from the GIS local cache.

Pre-publication evidence:
- Google reference API verified for `AuthorizationClient.clearToken` and `ClearTokenRequest.Builder.setToken`;
- targeted Kotlin authorization/coroutines compile probe PASS;
- clean H37a → H37b materialization PASS;
- terminal blobs: `DriveAuthorization.kt=4e727ae8d2a7e24a8e2c960380ade30ad1be1bf3`, `DriveV3Protocol.kt=83a46bcd0d98007c376b3476e99a97d3d6a96a9b`;
- second H37b materializer execution idempotent PASS;
- deliberately corrupted H37b source archive rejected before H37b mutation PASS.

Historical note: at this checkpoint a fresh manual canonical workflow was still required; no digital-pass status was inferred from the local probes alone. That requirement was subsequently satisfied by signed CI #669.

## H37 historical source pre-gate evidence
At this historical checkpoint H37a was not yet a DIGITAL PASS. H37's source-integrity proofs remained valid, while H37a added the compile-corrective evidence above. The candidate still required the canonical Android gate:
- clean H36c → H37 materialization PASS;
- second full materialization PASS/idempotent;
- all 20 H37 terminal source Git blobs match the materializer contract;
- a deliberately corrupted H37 source archive is rejected before H37 source mutation;
- shell syntax and `git diff --check` PASS;
- static audit finds only OAuth `drive.file` and no Firebase/backend credential/service-account/client-secret/refresh-token path.

That canonical proof was later satisfied by CI #669 / run `35591631207`, which superseded this pre-gate state as signed authority.

## H36 closure evidence
H36 is digitally closed by CI #663. The source-materialization/idempotence/fail-closed proofs remain valid, while that canonical Android gate proved compilation, Lint, **34/34 standard instrumentation**, isolated geometry and signing. H36a/H36b/H36c are test-only correctives and introduce no runtime source change after H36. CI #669 later superseded #663 as the latest signed DIGITAL PASS; U8j/#746 is newer but intentionally unsigned.

## Historical signed authority sequence
CI #663 / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d` is the signed DIGITAL PASS through H36c.

Audited evidence:
- **330/330 JVM/unit tests PASS**;
- Android Lint PASS with **0 errors, 50 warnings and 4 hints**;
- debug/release assembly and unsigned provenance PASS;
- unsigned tested APK SHA-256 `215315f8b943704b9b820f98b4b7747df2fbbc62b862ab08e6c14d4dea9e89ad`;
- **33/33 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing/zipalign/package/version/certificate PASS;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed APK SHA-256 `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

CI #658 is retained only as failed intermediate evidence: H35 materialization, unit tests and API36 passed there, but Lint rejected the missing local permission proof. H35a corrected that issue and CI #659 closed the full gate.

## Forward gate policy
For H29 onward, every promoted source block must preserve the same exact-source discipline: deterministic materialization, current tests plus new block-specific regression, Lint/build/provenance, API36 standard + isolated geometry, and signing of the exact tested unsigned artifact.

Actual counts and hashes are always taken from the new run; prior counts are historical evidence only.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later source/docs HEAD never changes an earlier APK producer identity.
