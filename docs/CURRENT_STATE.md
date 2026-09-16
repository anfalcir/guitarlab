# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current signed DIGITAL PASS: **CI #641** / run `35105065689` / exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf`.
- Unsigned APK SHA-256: `824e8c070ebee6bbf920990dce3c948d2fc3474610524f3a087c38bd27ca5248`.
- Signed APK SHA-256: `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact ID: `10450495802`, name `GuitarLabStudio-0.5.0-rc3-homologacao`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun it.

## Evidence boundary
CI #641 is authoritative through **H24a**. It supersedes CI #639 as the current signed digital authority while preserving all earlier historical evidence.

The exact product/source identity of the APK is `b11769f340f7056c37dfb17d95b062909dad87bf`. Any later documentation-only commit does not change that producer SHA.

## CI #641 — DIGITAL PASS
All mandatory layers passed on the same exact source SHA:
- JVM/unit suites: **269/269 PASS**, 0 failures, 0 errors, 0 skipped;
- Android Lint gate: PASS; non-blocking warnings/deprecations may remain and are not reported as zero warnings;
- debug + release assembly and unsigned provenance: PASS;
- H24a source materialization with verified final hashes: PASS;
- standard API36 connected regression: **25/25 PASS**;
- isolated 1920×1200 target-tablet geometry: **1/1 PASS**;
- signed homologation: PASS;
- package/version identity: `studio.guitarlab.app`, `0.5.0-rc3`, versionCode `23`;
- unsigned artifact checksum verified before signing;
- APK Signature Scheme v2: PASS;
- signer count 1, RSA 4096, locked certificate match: PASS;
- signing bundle cleanup: PASS.

Canonical Android regression reporting is **25/25 standard + 1/1 isolated geometry**. Do not flatten this to 26/26.

## H22/H22a — route UX — PHYSICAL PASS retained
Samsung SM-X230 physical review approved semantic physical routing: duplicate internal endpoints are collapsed, built-in routes use friendly labels and MK-300 endpoints are consolidated. Later blocks must not regress this behavior.

## H23/H23a/H23b — recording timing and transient feedback
H23/H23a introduced and aligned session-clock mapping, route/rate calibration, residual adjustment and transient-feedback policy. H23b hardened stale timestamp rejection, exact route+rate calibration identity, 44.1/48/88.2/96 kHz coverage, overflow-safe placement, analyzer diagnostics and route-token sanitization.

H23b remains DIGITAL PASS and focused physical recording-timing acceptance on SM-X230 + MK-300 is still pending.

## H24/H24a — Home Project Library — DIGITAL PASS
Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.
Corrective compile-alignment producer: `b11769f340f7056c37dfb17d95b062909dad87bf`.

H24 provides:
- real-time project-name search, case- and accent-insensitive;
- combinable template, content and sample-rate filters;
- deterministic modified/created/name ordering in both directions;
- result count `X de Y` when narrowed;
- distinct true-empty and no-results states;
- explicit clear-search and clear-filter actions;
- immutable in-memory normalized-name index rebuilt only when repository snapshot changes;
- query/filter/sort preservation across refresh and project operations;
- synchronized Help text and Android semantics/test coverage;
- no project-schema, managed-media or `.guitarlab` format change.

H24a only corrected the Android-test import contract discovered by CI #640; it did not change production Home behavior.

## Historical CI #640
CI #640 / run `35103316449` targeted `665bfbec78d032df21f467f226e353146595f67c`.
- software gate: PASS;
- Android gate: failed at `:app:compileDebugAndroidTestKotlin` due invalid explicit `assertDoesNotExist` import;
- instrumentation did not execute;
- signed homologation was correctly skipped.

This remains historical evidence of a test-source compile defect and does not weaken the later full #641 PASS.

## Current residual gate
Do **not** rerun deterministic CI merely to reconfirm #641. The remaining work is target-device evidence:
1. short H24 Home-library smoke on SM-X230;
2. retained H22 route smoke after MK-300 reconnect;
3. H23b zero-adjustment repeated-take recording-timing validation, especially 44.1 kHz;
4. backing-isolation/live-waveform/input-fail-closed/edit/export smoke;
5. explicit approval of the exact CI #641 signed APK SHA `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` as the authoritative physical checklist.
