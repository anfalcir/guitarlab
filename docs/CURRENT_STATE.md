# Current State — GuitarLab Studio

Updated: 2026-09-25

## Status

**RC21 MAINTENANCE CANDIDATE — SOFTWARE/API 36 PASS / MAIN INTEGRATED / SIGNING PENDING**

RC20 remains the currently accepted, physically homologated baseline recorded in `RELEASE_BASELINE.md`. RC21 is a proportional maintenance successor opened under the RC20 maintenance policy after owner-observed persistence/Studio and backup-feedback regressions. Implementation and serial hardening are complete; the unsigned Android qualification is green. RC21 does not replace RC20 until the exact signed RC21 APK passes residual owner acceptance.

## RC21 candidate identity

- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc21`;
- versionCode: `41`;
- maintenance source branch before integration: `maintenance/rc21-integrity-feedback-backup`;
- canonical integration: PR #7 merged into `main`;
- main integration commit: `2617fe1f1f2351a17389f165ed5d5a8e834e16a2`;
- software/API36 qualified source: `ad182678cb2704dc9bbfc622124b4f2ac121fea1`;
- Android CI: **#910 / run 36195905242 — PASS**;
- Unit tests + Lint + APK build: PASS;
- API 36 emulator regression: PASS;
- signed homologation artifact: pending intentional qualification from canonical `main`.

## RC21 maintenance scope

RC21 is deliberately narrow. The production separation backend, Demucs/model identity, recording route/timing architecture and unaffected RC20 behavior remain unchanged.

### Legacy recording/stereo integrity

- narrowly recover missing take lineage only for legacy managed recordings whose role, managed URI/path, WAV format, clip-id filename prefix and historical take timestamp all match;
- never promote generic imports into recorded takes;
- stereo separation persists the splitter's actual editing sample rate and total frame count rather than inheriting a trimmed clip length;
- separation preserves modern take metadata by creating per-track take lineage;
- ambiguous pre-existing shared take lineage fails closed instead of guessing;
- derived/proxy media is discarded if project publication fails.

### Persisted-state integrity

- Drive/package integrity validation verifies the decoded persisted canonical project state before compatibility repair changes the in-memory representation;
- compatibility recovery is applied only after the stored digest has been accepted;
- recovery remains idempotent and cannot redefine the cryptographic identity of the archived project bytes.

### Transient feedback

- Home/Studio/Backup/Diagnostics use the shared transient-feedback policy/host instead of independent Snackbar implementations;
- eligible transient events are consumed when handed to the host, preventing delayed replay after navigation;
- persistent operational state remains inline rather than becoming Snackbar spam;
- raw routing/device identifiers remain excluded from normal transient UX.

### Drive catalog/cache and automatic backup

- one Drive head snapshot is used to derive retained versions, reconciliation and remote tips;
- known immutable version/manifest metadata is reused to avoid re-downloading unchanged project-state objects;
- a small metadata-only cache is durable, schema/integrity checked, account/retention scoped and bound to current local revision identities;
- cache is never remote truth: every visible Backup screen entry forces a remote head verification;
- cached versions remain visible during background refresh and empty-catalog refresh shows immediate loading state;
- user refresh always forces Drive verification;
- automatic backup remains owned by WorkManager and does not depend on Backup UI lifetime;
- completion refreshes the catalog immediately only when the Backup screen is visible; otherwise the worker finishes without a redundant Drive read and the next screen entry performs the forced refresh;
- cancellation and terminal Activity state remain explicit.

## Evidence and provider scope

The underlying Drive v3 OAuth/transport/resumable-upload/commit/restore store is unchanged, so the accepted U8m provider-real campaign remains valid supporting evidence for those semantics. RC21 changes catalog read/cache/presentation orchestration, therefore final residual acceptance includes a representative real-Drive backup → catalog refresh → restore smoke on the target device; the full destructive U8m campaign is not repeated without a transport/store change.

## Promotion sequence

1. complete live documentation — DONE;
2. merge the maintenance work into canonical `main` — DONE via PR #7;
3. remove/disable the temporary maintenance branch and keep it out of CI triggers — next pre-signing control;
4. run `[run ci signed]` from the exact canonical `main`;
5. install that exact signed artifact on Samsung SM-X230 / Android 16;
6. perform residual acceptance for the affected maintenance paths, including the legacy Studio recovery case and representative Drive backup/catalog/restore;
7. after owner PASS, update `RELEASE_BASELINE.md` to RC21 and archive the final acceptance record.

## Accepted baseline until promotion

RC20 remains immutable and accepted until step 7. Its signed APK hash, signer, producer SHA, physical acceptance and production worker digest remain exactly as recorded in `RELEASE_BASELINE.md`.
