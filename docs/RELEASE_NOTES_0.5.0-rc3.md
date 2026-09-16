# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Current signed digital homologation — CI #641
Run `35105065689`, exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf`, is the current signed DIGITAL PASS through H24a.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `824e8c070ebee6bbf920990dce3c948d2fc3474610524f3a087c38bd27ca5248`;
- signed APK SHA-256 `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID `10450495802`.

CI #641 evidence:
- **269/269** JVM/unit PASS;
- Android Lint gate PASS;
- debug/release build and unsigned provenance PASS;
- API36 standard **25/25 PASS**;
- isolated 1920×1200 geometry **1/1 PASS**;
- signed homologation PASS;
- v2 signature, one RSA-4096 signer, locked-certificate match PASS.

Report Android regression as **25/25 standard + 1/1 isolated geometry**.

## H22/H22a — route UX
Physical review confirmed semantic route consolidation on the Samsung target: duplicate built-in endpoints are removed, system-only endpoints are hidden and MK-300 is represented as a physical route rather than raw Android endpoints.

## H23/H23a/H23b — recording timing and feedback
H23 introduced session-clock mapping, route/rate calibration, residual fine adjustment and transient-feedback discipline. H23b hardened progressing timestamp evidence, exact calibration scope, 44.1/48/88.2/96 kHz coverage, overflow-safe placement, analyzer diagnostics and technical-route sanitization.

Digital hardening is PASS. Focused physical recording-timing acceptance remains pending.

## H24/H24a — Home Project Library — DIGITAL PASS
H24 adds:
- accent/case-insensitive real-time name search;
- template filters: all, Guitar and Blank;
- content filters: all, with recordings, with audio/clips and no clips;
- sample-rate filters: all, Auto, 44.1, 48, 88.2 and 96 kHz;
- sorting by modified, created or name in both directions;
- deterministic tie-breaks;
- result counter `X de Y` when narrowed;
- distinct empty-library and no-results states;
- clear-search/clear-filter flows;
- accessible/testable controls and synchronized Help.

Performance architecture: repository refresh builds one immutable index containing pre-normalized names. Search/filter/sort then operates entirely in memory; typing does not reread project files.

Compatibility: no project schema, managed-media or `.guitarlab` package change.

Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.

H24a corrected only the invalid Compose-test import discovered by CI #640. It does not change production Home behavior.

## Historical CI #640
CI #640 (`35103316449`) completed the software gate successfully but failed before instrumentation at Android-test compilation due the invalid H24 import. Signing was correctly skipped. CI #641 supersedes that failed attempt with full exact-source digital PASS.

## Release decision
CI #641 is the signed authority. No new deterministic CI is required merely to reconfirm it. The exact #641 APK now advances to residual physical homologation on SM-X230 + MK-300.

Final physical approval still requires the Home-library tablet smoke, retained route behavior, H23b recording-timing validation, no repeatable P0/P1, and explicit approval of signed APK SHA `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.
