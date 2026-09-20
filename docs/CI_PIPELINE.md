# Android CI / release pipeline

Updated: 2026-09-17

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs without explicit user instruction.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Current source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact SHA-256/Git blob checks.

Canonical tail: `… → H25 → H26 → H26a → H26b → H26e → H27 → H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34`.

The previously accepted H28 materializer is preserved byte-for-byte as `scripts/materialize_ci_sources_through_h28.sh`. The entrypoint then runs `scripts/materialize_ci_sources_h29_h33.sh`.

H28 input remains `.source-parts/H28BackupIdentityConsistency.patch.gz.b64` with gzip SHA-256 `1abd8101361b241dfb443950c2a635141b41fecca77442d86343eb3f3025d3be` and decoded patch SHA-256 `3d06aa851ad1dc88dd078d60bb24ea097bbca7ef4f3e93089f47c9bd4a0a6e71`.

New tail inputs:
- H29 `.source-parts/H29RecordingSessionHealth.patch.gz.b64` — gzip `2f5dff49aeeb271aadf4992f85f0dbf451a75f20fc4c72a750f09f993d8cc4ab`, patch `229998a0fb2198d8ecf82cba2f48eb7d66929ddf7e8b9398d6b017d8c04d0640`;
- H30 `.source-parts/H30RecoveryUsbResilience.patch.gz.b64` — gzip `4b72aead443f21b2f61055a73e22d5c1e524afb368d74773d61c1b7dfd2463b1`, patch `26d4408812d7d8382b3e52d6542dce369071a8eb278ab8c240745e09c802b2bd`;
- H31 `.source-parts/H31TakesDiagnostics.patch.gz.b64` — gzip `a90c1577bbb17c943742b5ca6b654f0de845f9bdd654d4a604b5d60241807630`, patch `0031367ba9b61057524056540a9b57a29aecb65cd4eb2d25854057ab170eda1d`;
- H32 `.source-parts/H32TenMinuteQualityGate.patch.gz.b64` — gzip `41c5b3aba13e70b688b630bdbc5035ae92905257bd01db61b2cb6f69ba2e6435`, patch `63d62e3734ec0081b309bceffef05bd18cbc7c6d77ad4fc695a5534c8c3e5fb4`;
- H33 `.source-parts/H33ExternalControl.patch.gz.b64` — gzip `d88a320ba2323ebac7a9db24d1f9314af270cc595d209d1bbfdfefc81be0f6a9`, patch `46880223ffd10711bbf660feedb705ea9c3fa6cd6a111017bd3b6ad32e7172dc`;
- H33a/H33b — external-control CI correctives included in signed #657;
- H34 `.source-parts/H34FineLatencyRange.patch.gz.b64` — gzip `d7ea906f7300d7b00c8a384ac945e140f386e7faf0023dad1f0672b8cda17bad`, patch `4b47bdc21b58c68e5ca9616314dca495ca5c5f5b72a1be76d25c1dc9116565c0`.

Expected terminal message: `Source patch chain materialized through H34 with verified final hashes`.

Pre-publication source proofs on the exact H28 baseline:
- first H28→H33 materialization PASS;
- second invocation PASS/idempotent;
- deliberate H33 source corruption rejected with nonzero exit before ready-state acceptance;
- each H29-H33 block verifies both archive/decoded-patch SHA-256 and exact final Git blob hashes.

These are source/pre-gate proofs only. The canonical Android workflow has not yet been manually run on source checkpoint `1df91e16ad0a928b0d5ab93bfd975b49b6d2da62`.

## Current signed authority — CI #657
CI #657 / run `35290128876` / producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6` is the signed DIGITAL PASS through H33b.

Audited evidence:
- **323/323 JVM/unit tests PASS**;
- Android Lint PASS with 0 errors;
- debug/release assembly and unsigned provenance PASS;
- **33/33 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing/zipalign/package/version/certificate PASS;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed APK SHA-256 `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.

H34 is source-only PRE-GATE. Local proofs: pure Kotlin ±500 ms boundary/placement harness PASS, H33b→H34 and H28→H34 materialization PASS, idempotence PASS, corrupt source-part fail-closed PASS.

## Forward gate policy
For H29 onward, every promoted source block must preserve the same exact-source discipline: deterministic materialization, current tests plus new block-specific regression, Lint/build/provenance, API36 standard + isolated geometry, and signing of the exact tested unsigned artifact.

Actual counts and hashes are always taken from the new run; prior counts are historical evidence only.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later source/docs HEAD never changes an earlier APK producer identity.
