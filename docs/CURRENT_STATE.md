# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Canonical signed DIGITAL PASS: CI #631 / run `35025012392` / exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c`.
- Signed APK SHA-256: `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`.
- Unsigned APK SHA-256: `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`; the assistant must not dispatch or rerun it.

## Evidence boundary
CI #631 is the authoritative digital baseline through H18/H18a/H19. It passed the complete software gate, Android API36 gate and signed homologation on the same exact source SHA.

Digital evidence:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- H19 USB route-policy JVM tests: PASS;
- standard API36 connected regression: **23/23 PASS** (`0` failures, `0` errors, `0` skipped);
- isolated 1920×1200 / 240dpi geometry: **1/1 PASS**;
- H18a narrow discoverability regression: PASS;
- H18a target-tablet logical-width comparison containment regression: PASS;
- signed homologation: PASS;
- APK Signature Scheme v2: PASS, one RSA-4096 signer, expected certificate confirmed.

Do not report the Android gate as 24/24: the authoritative expression is **23/23 standard + 1/1 isolated geometry**.

## H18 / H18a — adaptive practice bar — DIGITAL PASS
Physical review after #626 exposed clipping of the final `Ambas` comparison button. The previous fixed docked proportions were replaced by content-aware rendering:
- wide/tablet: Comparação and Ajustes size to content; Timeline owns the flexible remainder and its own overflow;
- narrow docked widths: Comparação, Ajustes and Timeline stack as semantic groups so no group can be pushed fully off-screen;
- all four comparison controls (`Desativado`, `Referência`, `Minha`, `Ambas`) are regression-guarded for containment at target-tablet logical width;
- Ajustes/Níveis containment and non-overlap remain guarded.

CI #628 exposed the first narrow-fallback defect (21/22); H18a corrected it. CI #629/#630 then failed only at `git diff --check` because the H18a source-part contained four whitespace-only blank lines. Commit `33fb05a...` cleaned those bytes without changing the materialized product source. CI #631 subsequently passed all gates.

## H19 — canonical USB output routing — DIGITAL PASS / PHYSICAL ROUTE CHECK PENDING
Root cause of the duplicate output selector was architectural: Android may expose one physical USB interface as multiple logical `AudioDeviceInfo` endpoints. GuitarLab previously surfaced each raw endpoint as a separate user choice.

H19 now:
- collapses equivalent USB endpoints into one canonical physical-facing choice;
- preserves distinct non-USB profiles;
- migrates legacy raw endpoint signatures to the canonical route signature;
- resolves duplicate candidates with an inaudible stereo `AudioTrack` probe;
- confirms the actual endpoint using `AudioTrack.routedDevice`, never enumeration order alone;
- rejects stale endpoint IDs after reconnect and clears disappeared selections;
- fails closed to the existing automatic-route fallback if a duplicate candidate cannot be confirmed.

The policy, compilation and Android app integration are digitally homologated at #631. An emulator cannot prove the MK-300's real USB endpoint behavior, so final hardware evidence still requires: one MK-300 output choice, audible playback through it, and successful disconnect/reconnect/reselection.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7 digital scope through H19/H18a: **DIGITAL PASS at CI #631**; final physical closure pending.
- M8 RC3 hardening through H19/H18a: **DIGITAL PASS at CI #631**; release decision pending the residual hardware checklist.

## Next step
Install and test the exact #631 signed APK on Samsung SM-X230 + M-VAVE MK-300 using `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`. No additional CI is required if that exact APK passes and no product/source code changes are introduced afterward.
