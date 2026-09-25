# U0 — Baseline freeze and integration inventory

Updated: 2026-09-21
Status: implementation evidence in progress

## Frozen repository state

| Product | Live branch HEAD | Behavior baseline consumed by integration |
|---|---|---|
| GuitarLab | `dc3cb95093110270c494804aa03ef625dc3ceef2` | H37b producer `01b2371310fb872eb1583231728941beb93c1a8e`, plus H37c pipeline-only corrective |
| GBW Android | `4724b030eabe289a5c2645e379181c0f9602d25d` | functional RC5 `48af553518c44b46b913aad73c22e3da04368ea5` |

The HEADs above were resolved from the remote immediately before U0 writes. The machine-readable authority is `integration/u0/inventory.json`.

## Baseline gate interpretation

CI #665 ran the exact H37b producer and passed unit tests, Android Lint, APK build, API 36 regression and unsigned-artifact integrity. Its final signing job failed before keystore restoration because raw pre-materialization version metadata was compared with the already materialized/tested APK. H37c fixes that pipeline ordering at the current HEAD.

This satisfies the U0 prerequisite that H37b pass its compile/build gate before becoming the integration base. It does **not** promote RC4, claim a signed DIGITAL PASS, or replace signed authority CI #663. A new manual canonical run remains required later under the existing CI policy.

GBW's functional remote-separation baseline remains the documented RC5 commit/image digest. Later GBW HEAD changes only prune workflows and do not redefine the runtime baseline.

## Ownership and porting rules

- GuitarLab owns the final application shell, package/signing identity, project/timeline/take domain, managed-media rules, Studio audio paths, portable package and Drive evolution.
- GBW supplies source acquisition, ranking, Demucs orchestration, six-stem validation, explicit local fallback, remote lifecycle, shared-gain preparation and cloud backend behavior.
- Cloud source moves by Phase A/B/C; production is not cut over during U0.
- Existing Firebase Storage remains temporary processing transport. Drive remains durable backup.
- GBW UI and project/backup implementations are migration/reference inputs, not parallel target systems.
- Every imported source path, target owner, strategy and target milestone is enumerated in `integration/u0/inventory.json`.

## Schema freeze

| Contract | Frozen input |
|---|---|
| GuitarLab project | schema 1; `GuitarProject.id` is current project identity |
| GBW project | schema 2; UUID `projectId`; optional source/separation/export state |
| GBW export | schema 2; backing + guitar, common shared gain |
| GBW remote result | schema 1; six expected stems |
| H37 Drive | legacy GuitarLab backup adapter input, not the U8 unified schema |
| Portable `.guitarlab` | retained self-contained package contract |

U1 must add the unified asset/provenance layer backward-compatibly. It must not rewrite these frozen fixture inputs or reinterpret H37 in place.

## Golden fixtures

`integration/u0/fixtures/manifest.json` freezes:

- a minimal valid GuitarLab schema-1 project;
- a synthetic GBW schema-2 project containing a six-stem separation record;
- the existing deterministic GuitarLab WAV codec fixture.

All fixture content is synthetic and contains no user audio or project data. `scripts/verify_u0_inventory.py` checks JSON readability, capability completeness, unique IDs and every fixture SHA-256.

## Duplicated concepts resolved

| Concept | Target authority |
|---|---|
| Project identity/model | GuitarLab core domain, evolved in U1 |
| Managed media and cleanup | GuitarLab immutable-media policy plus one reachability service |
| Backup | GuitarLab H28/H37 domain evolved to U8; GBW backup becomes legacy import input |
| Codec/audio inspection | GuitarLab core/platform codec abstractions; only missing verified behavior is ported |
| Export | Study Export and Studio Master remain separate workflows |
| Jobs | one durable operation contract with Android WorkManager adapters |
| UI/navigation | GuitarLab shell; GBW screens remain behavioral references |

## Baseline evidence and limitations

- GuitarLab materialization through H37b is deterministic and passed locally at this HEAD.
- CI #665 is the current compile/build/API36 evidence for the integration base.
- GBW RC5 CI/deploy/image evidence is preserved in its `CURRENT_STATE.md`.
- This Work environment has Java 17 but no installed Gradle 9.6.1 or Android SDK 36, so it cannot honestly reproduce the full baseline builds locally. Existing canonical evidence is retained; no hosted CI was dispatched.
- Known MK-300, real OAuth/Drive, App Check and listening/timing residuals are carried to the consolidated final campaign, not marked complete in U0.

## U0 exit gate

U0 can close when all are true:

1. current remote HEADs still match the inventory;
2. fixture and inventory verifier passes;
3. materialization is idempotent and fails closed on corruption per the existing chain;
4. baseline evidence is accurately recorded without promoting unsigned H37c;
5. roadmap/current-state/documentation map point to this evidence;
6. repository diff and security/path scans are clean.

U1 may then begin with the frozen contracts above.
