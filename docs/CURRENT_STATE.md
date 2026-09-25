# Current State — GuitarLab Studio

Updated: 2026-09-25

## Status

**FROZEN / FINAL PHYSICAL PASS**

The RC20 completion line is closed. There is no active development milestone, release blocker, pending physical gate or successor roadmap in the live documentation set.

The immutable accepted release identity is `RELEASE_BASELINE.md`.

## Current product baseline

- Android release: `0.5.0-rc20` / versionCode `40`;
- package: `studio.guitarlab.app`;
- source materialization tail: U12bx through `scripts/materialize_ci_sources_u12bx.py`;
- canonical materialization entrypoint: `scripts/materialize_ci_sources.sh`;
- final Android qualification/signing: CI #904 PASS;
- final owner physical homologation: PASS on 2026-09-25;
- production separation worker: exact digest frozen and recorded in `RELEASE_BASELINE.md`;
- official separation engine/model: Demucs/PyTorch `htdemucs_6s`;
- direct Google Drive API v3 remains the backup transport;
- legacy GBW/H37/pre-unification migration remains intentionally outside supported scope.

## Development state

U0–U12 and the C cohesion line are closed for the accepted RC20 product. Completed roadmaps, corrective plans, gate reports and release campaigns are historical evidence under `docs/history/`.

No new build, deploy, signing run or physical campaign is scheduled merely because documentation is being finalized.

## Maintenance state

The product remains frozen until one of the maintenance triggers in `PROJECT_IDENTITY.md` / D-090 / D-096 occurs or the owner explicitly requests a new feature.

A future change requalifies only the affected path plus adjacent integration risk under `TEST_AND_HOMOLOGATION_POLICY.md`. Existing accepted evidence remains reusable unless the change can materially invalidate it.

## Documentation-only freeze commits

Post-homologation documentation commits may advance `main` but do not alter the accepted Android producer SHA, signed APK hash, signer or production worker digest. Those identities remain fixed in `RELEASE_BASELINE.md`.
