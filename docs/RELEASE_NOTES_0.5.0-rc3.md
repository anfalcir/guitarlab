# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Current signed digital homologation — CI #639
Run `35096711936`, exact product source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`, is the signed DIGITAL PASS through H23b.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI #639 evidence:
- H23b materializer exact final hashes: PASS;
- unit/JVM suites: **260/260 PASS**;
- Android Lint/build/unsigned provenance: PASS;
- API36 standard connected regression: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- APK Signature Scheme v2: PASS, one RSA-4096 signer;
- package/version/versionCode verification: PASS;
- signing bundle cleanup: PASS.

## H22/H22a — route UX
Physical review confirmed semantic route consolidation on the Samsung target: duplicate built-in endpoints are removed, system-only endpoints are hidden, and MK-300 is represented as a physical route rather than low-level Android endpoints. This physical approval remains part of the RC3 baseline.

## H23/H23a
H23 introduced repeated monotonic capture/playback anchors, signed capture-vs-backing startup offset, separation of session-clock alignment / accepted route latency / residual fine adjustment, project-rate-aware calibration, punch crop from final compensated placement and centralized transient-feedback filtering. H23a aligned the app test source with JUnit 4. CI #638 digitally passed that state.

## H23b — corrective hardening — DIGITAL PASS at CI #639
An audit of the exact #638 source snapshot identified edge cases and H23b closed them without introducing a global magic offset:
- explicit 44.1 / 48 / 88.2 / 96 kHz coverage;
- stale/repeated timestamp evidence ignored and backwards clocks rejected;
- overflow-safe timing placement and 100,000-case randomized property coverage;
- exact length-prefixed `input + output + sample rate` persistence scope;
- old lossy 32-bit calibration key is not auto-applied;
- analyzer exposes route/rate, attempts, median, jitter, drift, confidence and status;
- raw Studio errors and centralized notices share the route-token sanitizer;
- routine operational actions remain silent while genuine errors/degradation/async completion retain concise transient feedback.

## Provenance
The signed job consumed the exact unsigned artifact produced by the software gate, verified its digest and `UNSIGNED_SHA256SUMS.txt`, checked source SHA/package/version/versionCode, then zipaligned, signed and verified the APK. The final `BUILD_IDENTITY.txt` ties both unsigned and signed SHA-256 values to source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`.

A later docs-only commit must not be treated as the APK source.

## Evidence WAV
`WATG - Enemy-master.wav` remains qualitative physical evidence that the pre-H23 recording path could land late. Only the **left channel** is the relevant recorded-guitar evidence channel. It is not an isolated calibration reference and no global offset is derived from it.

## Release decision
H23b is now digitally homologated. The remaining critical gate is focused physical REC alignment on Samsung SM-X230 + M-VAVE MK-300 using the exact CI #639 APK, with residual fine adjustment at **0.0 ms first**.

RC3 is not final until that exact signed candidate has no repeatable P0/P1 systematic recording offset and receives explicit physical approval.
