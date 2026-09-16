# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Last signed digital homologation — CI #639
Run `35096711936`, exact producer source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`, is the signed DIGITAL PASS through H23b.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI #639 evidence:
- 260/260 JVM/unit PASS;
- Lint/build/provenance PASS;
- API36 standard **23/23 PASS**;
- isolated 1920×1200 geometry **1/1 PASS**;
- signed homologation PASS.

## H22/H22a — route UX
Physical review confirmed semantic route consolidation on the Samsung target: duplicate built-in endpoints are removed, system-only endpoints are hidden and MK-300 is represented as a physical route rather than raw Android endpoints.

## H23/H23a/H23b — recording timing and feedback
H23 introduced session-clock mapping, route/rate calibration, residual fine adjustment and transient-feedback discipline. H23b hardened progressing timestamp evidence, exact calibration scope, 44.1/48/88.2/96 kHz coverage, overflow-safe placement, analyzer diagnostics and technical-route sanitization. H23b received full DIGITAL PASS at CI #639; focused physical recording-timing acceptance remains pending.

## H24 — Home Project Library — PRE-GATE
H24 adds a production-grade project-library layer to Home:
- accent/case-insensitive real-time name search;
- template filters: all, Guitar and Blank;
- content filters: all, with recordings, with audio/clips and no clips;
- sample-rate filters: all, Auto, 44.1, 48, 88.2 and 96 kHz;
- sorting by modified, created or name in both directions;
- deterministic tie-breaks;
- result counter `X de Y` when narrowed;
- distinct empty-library and no-results states;
- explicit clear-search/clear-filter flows;
- accessible/testable controls and synchronized in-app Help.

Performance architecture: a repository refresh builds one immutable index containing pre-normalized names. Search/filter/sort operates entirely in memory; typing does not reread project files and does not renormalize every stored name.

Compatibility: no project schema, managed-media or `.guitarlab` package change.

Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.

Local/source validation includes pure Kotlin compile, deterministic functional checks, 10,000 randomized libraries across all sort modes, patch forward/reverse round-trip, gzip/base64 integrity, idempotent materialization, corruption fail-closed, final blob hashes, `bash -n`, `git diff --check` and syntax/parser scan of changed Home source.

H24 is not Android-build/API36/signed PASS until the next manually dispatched full workflow succeeds.

## Release decision
CI #639 remains the signed authority. The next promoted APK must be produced by one new full manual workflow on final current `main`. After digital PASS, physical work should be residual: Home-library tablet smoke plus the outstanding H23b SM-X230 + MK-300 recording-timing validation.
