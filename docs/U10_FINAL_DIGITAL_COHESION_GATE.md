# U10 Final Digital Cohesion Gate — GuitarLab

Updated: 2026-09-22  
Status: **CLOSED / DIGITAL PASS**  
Canonical branch: `main`  
Exact technical source: `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`  
Canonical Android CI: **#781** / run `35791192802`

## Purpose

This document records the terminal U10/C8 digital evidence consumed by U11. It does not promote a signed release candidate; signed homologation was intentionally skipped in #781.

## Canonical gate result

Android CI #781 completed successfully on the exact U10zb source.

- software gate: **PASS**;
- deterministic source materialization through U10zb: **PASS**;
- JVM/unit gate: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS / 0 errors**;
- debug/release build gate: **PASS**;
- API 36 emulator regression: **PASS**;
- signed homologation: **SKIPPED by design** until U11 freeze.

Artifacts:
- Android integration artifact id `10722562266`;
- integration artifact digest: `sha256:1c355f88bef4e20393a5c26c39fb4db94da8383089023724988af55953f509e3`;
- exact-source artifact id `10722206126`;
- exact-source artifact digest: `sha256:e17472db3fc95fe2ae2e49c56571f06f72a0465c4fbbeecb0ad14e7c0d02210f`;
- software-gate artifact id `10721833310`;
- software-gate artifact digest: `sha256:bfc181af858479237b2361dd0d091f5d8d0c54bf329af4b598339a9f73dd837e`.

## API 36 fail-closed coverage

The runner classified and proved **23/23 instrumented classes** across five deterministic suites.

1. Project and navigation — **5/5 classes, 41 tests PASS**, 16 screenshots.
2. Studio and practice — **7/7 classes, 13 tests PASS**.
3. Import, controls and settings — **5/5 classes, 5 tests PASS**, 1 screenshot.
4. Export, backup and master — **5/5 classes, 16 tests PASS**, 6 screenshots.
5. Target-tablet geometry 1920×1200 — **1/1 class, 1 test PASS**, 1 screenshot.

Observed total: **76 instrumented tests PASS**.

## C8 visual matrix

The fail-closed visual gate retained **24/24 required screenshots**:

- Home empty;
- Home with project;
- Home with active background operation;
- New Project phone;
- New Project compact/enlarged-font stress;
- Prepare source search/running;
- Prepare separating;
- Prepare ready;
- Prepare failure;
- Studio normal;
- Studio prepared-reference update notice;
- Export ready;
- Export running;
- Export missing-project/error recovery;
- Activity completed;
- Activity mixed running/completed/failed operations;
- Settings primary screen;
- Settings compact/enlarged-font light stress;
- Backup disconnected;
- Backup empty/connected;
- Backup restore-version state;
- Backup conflict state;
- destructive project-delete confirmation;
- target-tablet Home at 1920×1200.

The retained matrix covers phone portrait, compact-width stress, enlarged font scale, light and dark presentation, and target-tablet landscape geometry.

## Deterministic visual review

The #781 artifacts were manually reviewed after capture-backend hardening.

Review result: **PASS**.

Confirmed:
- no Android ANR/system-dialog contamination remains;
- no blank/corrupt screenshot artifact;
- target-tablet capture fills the intended 1920×1200 viewport;
- Home, Prepare, Studio, Export, Activity, Settings and Backup remain visually attributable to one GuitarLab product;
- the Studio prepared-reference notice is retained;
- background-operation and failure states remain understandable without color-only semantics;
- destructive confirmation is captured from the application surface;
- compact/enlarged-font cases remain usable without a second navigation language;
- the compact Studio timeline ruler exposes the intended adaptive two-label state and guards against a third crowded label.

## Product-cohesion conclusion

C7 visual/copy/responsive/accessibility consolidation and C8 global digital cohesion are closed by the #781 evidence when read together with the earlier C1-C6 closures.

The global reviewer path is digitally evidenced as one product:

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

No second project shell, duplicate export chooser, orphan GBW product surface or raw internal-state vocabulary is accepted as a release path.

## U10 exit

**U10 is CLOSED / DIGITAL PASS.**

No repeatable P0/P1 digital defect remains in the accepted gate. The exact technical source consumed by U11 is `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`.

The latest signed release authority remains historical CI #669 until U11 freezes and signs a new exact candidate.

## U11 handoff

U11 must now:

1. run the final digital release checklist against the final documentation/source freeze;
2. preserve the #781 C8 evidence;
3. confirm real-cloud separation and Drive evidence remain current and applicable;
4. run documentation/security/credential consistency checks;
5. trigger the canonical signed gate using `[run ci signed]` only after freeze;
6. verify zipalign, package/version, certificate and signed APK SHA-256;
7. generate the final physical-only residual checklist for U12.
