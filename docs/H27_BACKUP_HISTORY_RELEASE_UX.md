# H27 — Backup history semantics and release UX hardening

Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**
Updated: 2026-09-16

## Trigger
Physical homologation of the CI #650/H26e candidate exposed two release-level problems in `Backup e restauração`:

1. a manual backup of an unchanged project could create another remote version because manual actions used `force = true`; if an automatic run had just protected the same persisted revision, a subsequent manual action could therefore duplicate it;
2. the setting displayed as a protected minimum (`1/3/5/10`) did not match the intended user meaning: it must be the **maximum number of historical versions kept per project**.

The screenshot also showed that the status summary could report only aggregate success/failure counts, which was insufficient to identify which project failed. The screen exposed implementation vocabulary (`Storage Access Framework`, `COMMITTED`, `SHA-256`) that does not belong on a normal end-user surface.

The exact chronology that produced every visible #650 row cannot be reconstructed from the screenshot alone. The source-level defect is nevertheless deterministic: manual backup forced a new version even when the same persisted revision already existed, while automatic backup was incremental. H27 removes that inconsistent contract.

## Corrected backup contract
### One persisted revision, one useful backup version
- `Backup total agora` and `Backup deste projeto` now use the same incremental revision check as automatic backup (`force = false`).
- Pressing a manual backup repeatedly without changing/saving the project does not create additional versions.
- If automatic backup already protected the current revision, a manual action reports that project as already updated instead of creating a duplicate.
- The process-wide backup/restore lock remains in place, so automatic and manual operations are serialized.

### History means maximum, not minimum
`BackupRetentionPolicy` now uses `maximumVersionsPerProject`.

For each project:
- the newest valid version is always preserved;
- duplicate copies with the same persisted `projectUpdatedAtEpochMs` collapse to the newest copy;
- older unique revisions are removed when they exceed the configured maximum count;
- the maximum count applies even when the age limit is disabled;
- the optional age limit can remove older revisions sooner, but never removes the newest valid version;
- cleanup remains isolated by project.

Existing installations migrate the legacy `minimum_versions` preference value into the new `maximum_versions` preference so a previously selected `3` becomes **maximum 3 versions**, matching the intended UI meaning.

### Fail-safe cleanup is retained
- if every attempted write fails, history cleanup is suppressed;
- a project whose write failed is excluded from that run's cleanup;
- cancellation propagates and cannot fall through into normal retention cleanup;
- single-project backup cleanup stays scoped to that project;
- delete failures are reported without invalidating a newly committed backup.

## Status and error UX
- `Último sucesso` becomes `Última execução`, so a partial/failed run is not misrepresented as success.
- successful, skipped/current, removed-history and failed counts use user-facing language.
- partial failure includes the project name and a human-readable reason.
- internal integrity terminology is translated into user-facing messages.

## Product-copy hardening
The backup screen and related product surfaces were reviewed as release UI rather than development UI.

Changes include:
- remove the top-bar subtitle `Storage Access Framework · provedor escolhido por você`;
- remove `SAF`, `COMMITTED` and backup `SHA-256` from ordinary user-facing backup copy;
- rename `Retenção` to `Histórico` and expose `Máximo de versões por projeto`;
- explain that unchanged projects do not create duplicate copies;
- remove provider/API implementation details from ordinary destination/status copy;
- replace hard-coded `MK-300 detectada` with generic USB-device wording in settings;
- replace proxy-PCM implementation language in ordinary readiness errors with a user-actionable preparation message;
- keep legitimate audio terms such as PCM only where they are intentionally part of an audio diagnostic/format surface.

`UI_COPY_STYLE.md` is strengthened so future product copy must describe user goals/outcomes and must not expose internal APIs, milestones, test/release vocabulary, personal context or model-specific hardware unless that identity is actually required for the user's task.

## Test changes prepared
Pure/JVM coverage now explicitly includes:
- maximum count applies even with age limit disabled;
- duplicate copies of one persisted revision collapse;
- newest version survives even if all versions are older than the age threshold;
- version maximum is independent per project;
- repeated manual backup of the same revision does not create another version;
- a safe skipped/current run can clean pre-existing duplicate copies;
- existing failure/cancellation/scope/integrity tests remain.

Android instrumentation adds release-copy assertions that the backup surface does not show `Storage Access Framework`, `COMMITTED` or `SHA-256`, and does show `Máximo de versões por projeto`.

## Source materialization
H27 is stored as `.source-parts/H27BackupReleaseUx.patch.gz.b64` and applies after H26e.

Encoded archive decodes to gzip SHA-256:
`2cbfa3bf7d3291778d73d5a3ffdf04f2e16ebf0c129a893f807d9cc548f53c05`

Decoded patch SHA-256:
`c80f0b04f34fb1b92ea47c13f7eb70df6744e0c07392e81391875f3b10aa85e5`

The materializer verifies exact final Git blob hashes for all 14 H27-changed production/test files. Expected terminal message:

`Source patch chain materialized through H27 with verified final hashes`

## Local/source validation completed
- `git diff --check`: PASS;
- `bash -n scripts/materialize_ci_sources.sh`: PASS;
- exact CI #650 H26e materialized snapshot → H27: PASS;
- idempotent second materializer execution: PASS;
- deliberate H27 source archive corruption: fail-closed before source mutation;
- focused Kotlin runtime harness against the real modified backup-domain source: PASS for max-history, duplicate-revision collapse, newest-version preservation and incremental same-revision behavior;
- release-surface static copy audit: no remaining user-facing backup copy references SAF/Storage Access Framework/COMMITTED/SHA-256; no hard-coded MK-300 product status remains in normal app UI.

Full Gradle/JVM/Lint/API36/release-signing execution is intentionally **not claimed locally**. It remains mandatory in the next manually dispatched CI.

## Evidence boundary / promotion rule
CI #650 remains the exact signed authority for H26/H26e only. Its APK does **not** contain H27 and must not be used to approve the corrected backup-history behavior.

H27 may be promoted to DIGITAL PASS only after a new exact-source manual workflow passes the complete software gate, API36 standard + isolated geometry gate and signed homologation. No workflow is dispatched by publishing H27.
