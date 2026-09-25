# Documentation Consistency Audit — 2026-09-20

## Scope
Audited the complete Markdown documentation inventory on `main`: **88 Markdown files** including the repository `README.md` and all `docs/*.md` files present before this sanitation commit.

## Current authority checked
- current signed DIGITAL PASS: CI #659 / run `35512894518`;
- exact producer: `a6a53e8ba9e75b32565e451870758c7c65ad687f`;
- package/version: `studio.guitarlab.app` / `0.5.0-rc3` / versionCode `23`;
- signed APK SHA-256: `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- canonical branch/default branch: `main`;
- official workflow: `.github/workflows/android-ci.yml`, manual-only `workflow_dispatch`.

## Inconsistencies corrected
1. `ARCHITECTURE.md` still identified CI #639/H23b/H24 PRE-GATE as current; it now reflects #659/H35a and the global-vs-take synchronization architecture.
2. `BACKUP_IDENTITY_CONTRACT.md` still marked H28 PRE-GATE; it now records H28 DIGITAL PASS, accepted target defect and protected-regression status.
3. `H26_SAF_CLOUD_BACKUP.md` still said a fresh H28 workflow was required; its supersession boundary now records the completed H28/#659 state.
4. `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md` still identified #657 and H34/H35 PRE-GATE; it now records #659/H35a DIGITAL PASS and physical-only residual.
5. `PRODUCT_REQUIREMENTS.md` still described the older residual fine-adjustment model and H23b/H24 regression boundary; it now defines global future-recording adjustment, take-specific synchronization, silent verification and H35a regression scope.
6. `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` was bound to CI #653; it is now bound to the exact #659 signed candidate and includes the H35 physical checks.
7. `MEDIA_IO_SUPPORT_CLAIM_RULE.md` still treated alpha13 as current authority; current authority is now the codec matrix + exact-source gate + device-gated physical evidence where required.
8. `M8_GLOBAL_DIGITAL_REGRESSION.md` and `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` contained old “active/current” wording; they are now explicitly marked historical/superseded snapshots without rewriting their historical evidence.
9. `GITLAB_CI_MIGRATION.md` is now explicitly marked historical/inactive; GitHub manual Actions is the current pipeline.
10. `DECISIONS.md` now makes the old Pocket Amp M2 evidence explicitly historical and aligns CI decision D-050 with the current manual GitHub promotion gate.
11. `HISTORICAL_CANDIDATES.md` now extends the evidence progression through CI #659 and no longer lists historical H0–H6/M8 documents as current authority.
12. `DOCUMENTATION_MAP.md` now separates live authority, normative subsystem contracts and historical evidence, preventing old dated statements from overriding current truth.

## Historical-document policy
Alpha/M4/M5/M6/M7 candidate/checkpoint files, dated documentation audits, older release notes and explicitly historical plans remain preserved. Their old words such as `current`, `latest`, `pending`, old branch names, old PR state or old hardware targets are interpreted only in the dated context of those documents.

This preserves traceability without creating contradictory current-state authority.

## Current physical boundary
Digital work through H35a is closed by CI #659. Stable-1.0 residual is hardware-only:
- real recording alignment on the intended MK-300 route;
- USB loss/reconnect and capture preservation with no microphone fallback/auto-resume;
- continuous 10-minute quality/save/reopen/export smoke;
- H35 target-device global-vs-take synchronization and quiet/silent calibration behavior;
- no repeatable P0/P1 and explicit approval of the exact signed APK.

H33 real-controller hardware acceptance remains a separate 1.1 boundary.

## Repository cleanup link
`REPOSITORY_SANITIZATION_2026-09-20.md` records the separate branch ancestry/divergence audit supporting deletion of all non-main branch references.
