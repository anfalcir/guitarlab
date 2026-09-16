# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Last signed digital homologation — CI #638
Run `35084703365`, exact product source `c310be6779e6591c57399257f380588c27bdf20a`, is the signed DIGITAL PASS through H23/H23a.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `621e02355d265bdb6c24cb5e324b445b63e739f8e7d6adc233eebea3b31fe0d5`;
- signed APK SHA-256 `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI #638 evidence:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 standard connected regression: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS.

## H22/H22a — route UX
Physical review confirmed semantic route consolidation on the Samsung target: duplicate built-in endpoints are removed, system-only endpoints are hidden, and MK-300 is represented as a physical route rather than low-level Android endpoints.

## H23/H23a — digital PASS at CI #638
H23 introduced:
- repeated monotonic capture/playback anchors instead of trusting a single startup timestamp;
- signed capture-vs-backing startup offset;
- separation of session-clock alignment, accepted route latency and residual fine adjustment;
- calibration using the project editing/recording sample rate;
- punch crop from final compensated placement;
- route/rate-specific bounded fine adjustment;
- centralized transient-feedback policy and technical-route filtering.

H23a only aligned the new app test with JUnit 4; production H23 code was unchanged.

## H23b — corrective hardening — PRE-GATE
An audit of the exact source snapshot emitted by CI #638 found edge cases worth closing before physical latency acceptance:
- explicit 88.2 kHz coverage was missing from the H23 timing test matrix;
- repeated/non-progressing `AudioTimestamp` observations could still count too generously as evidence;
- analyzer UI did not persistently expose the complete requested attempt/median/jitter/drift/confidence/status set;
- normal Studio error Snackbar display had a path that did not pass the raw message through the route-token sanitizer;
- H23 calibration persistence used a 32-bit hashed tuple key, which is not strong enough to prove exact route identity under collision.

H23b therefore:
- requires progressing frame/time observations and rejects backwards clocks;
- covers 44.1 / 48 / 88.2 / 96 kHz explicitly;
- adds overflow-safe placement arithmetic and 100,000-case property/fuzz coverage;
- uses an exact length-prefixed `input + output + sample rate` calibration/fine key;
- intentionally does not auto-apply the old hashed calibration key; rerun the analyzer after upgrade before relying on measured route compensation;
- surfaces selected routes, session rate, status, median latency, attempts, jitter, drift and confidence;
- sanitizes both notices and raw transient error text before normal Studio display;
- expands low-level token filtering (`deviceId`, `productName`, address, endpoint index, route internals).

## Evidence WAV
`WATG - Enemy-master.wav` remains physical evidence that the pre-H23 recording path could land late. Only the **left channel** is the relevant recorded-guitar evidence channel. The file is not an isolated calibration reference and no global offset is derived from it.

## Release decision
CI #638 remains the signed authority until H23b itself receives a full user-dispatched signed CI pass. The H23b source must not be called Android-build/API36/signed PASS based only on local source validation.

After that CI, the critical physical gate is repeated REC alignment on SM-X230 + MK-300 at the actual project rate, with residual fine adjustment at **0 ms first**. RC3 is not final while a repeatable P0/P1 systematic recording offset remains.
