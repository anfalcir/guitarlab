# Android CI / release pipeline

Updated: 2026-09-20

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs without explicit user instruction.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Current source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact SHA-256/Git blob checks.

Canonical tail: `… → H25 → H26 → H26a → H26b → H26e → H27 → H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35 → H35a → H36 → H36a → H36b → H36c`.

The previously accepted H28 materializer is preserved byte-for-byte as `scripts/materialize_ci_sources_through_h28.sh`. The entrypoint then runs `scripts/materialize_ci_sources_h29_h33.sh`.

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

Expected terminal message: `Source patch chain materialized through H36c with verified final hashes`.

Pre-publication source proofs on the exact H28 baseline:
- first H28→H33 materialization PASS;
- second invocation PASS/idempotent;
- deliberate H33 source corruption rejected with nonzero exit before ready-state acceptance;
- each H29-H33 block verifies both archive/decoded-patch SHA-256 and exact final Git blob hashes.

These pre-gate proofs were exercised by the canonical #659 workflow on the exact H35a source.

## H36 closure evidence
H36 is digitally closed by CI #663. The source-materialization/idempotence/fail-closed proofs remain valid, while the canonical Android gate now additionally proves compilation, Lint, **34/34 standard instrumentation**, isolated geometry and signing. H36a/H36b/H36c are test-only correctives and introduce no runtime source change after H36.

## Current signed authority — CI #663
CI #663 / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d` is the signed DIGITAL PASS through H36c.

Audited evidence:
- **330/330 JVM/unit tests PASS**;
- Android Lint PASS with **0 errors, 50 warnings and 4 hints**;
- debug/release assembly and unsigned provenance PASS;
- unsigned tested APK SHA-256 `4351458825b7a07c53ff2827e69477414896b497b0a77e3aa7e681c29d97c5da`;
- **33/33 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing/zipalign/package/version/certificate PASS;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.

CI #658 is retained only as failed intermediate evidence: H35 materialization, unit tests and API36 passed there, but Lint rejected the missing local permission proof. H35a corrected that issue and CI #659 closed the full gate.

## Forward gate policy
For H29 onward, every promoted source block must preserve the same exact-source discipline: deterministic materialization, current tests plus new block-specific regression, Lint/build/provenance, API36 standard + isolated geometry, and signing of the exact tested unsigned artifact.

Actual counts and hashes are always taken from the new run; prior counts are historical evidence only.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later source/docs HEAD never changes an earlier APK producer identity.
