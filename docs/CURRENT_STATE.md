# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current signed DIGITAL PASS: **CI #642** / run `35121955150` / exact producer source `7bfd876b6a5b0701ab0cf5203de36c31cd117632`.
- Unsigned APK SHA-256: `f6b4f21d0f514bad06b80eabdad141dac5cd236a707842c21258ec2072868c0e`.
- Signed APK SHA-256: `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.
- Signed APK size: `13,269,914` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact ID: `10458236596`, name `GuitarLabStudio-0.5.0-rc3-homologacao`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`); the assistant must not dispatch or rerun it without explicit user instruction.

## Evidence boundary
CI #642 is authoritative through **H25** and supersedes CI #641 as the current signed digital authority. Historical runs remain point-in-time evidence.

The APK producer identity is exactly `7bfd876b6a5b0701ab0cf5203de36c31cd117632`. Any later documentation-only commit does not change that producer SHA.

## CI #642 — DIGITAL PASS
All mandatory layers passed on the same exact source SHA:
- source materialization through H25 with verified final hashes: PASS;
- JVM/unit: **269/269 PASS**, 0 failures, 0 errors, 0 skipped;
- performance evidence: PASS;
- Android Lint: PASS; non-blocking warnings/deprecations remain and are not represented as zero warnings;
- debug + release assembly and unsigned provenance: PASS;
- standard API36 connected regression: **28/28 PASS**;
- isolated 1920×1200 target-tablet geometry: **1/1 PASS**;
- signed homologation from the exact tested unsigned artifact: PASS;
- package/version identity: `studio.guitarlab.app`, `0.5.0-rc3`, versionCode `23`;
- APK Signature Scheme v2: PASS; v1/v3/v3.1/v4 false;
- signer count 1, RSA 4096, locked certificate match: PASS;
- signing material cleanup: PASS.

Canonical Android reporting is **28/28 standard + 1/1 isolated geometry**. Do not flatten this to 29/29.

## H22–H24a retained state
- H22/H22a semantic route UX: DIGITAL PASS + focused SM-X230 PHYSICAL PASS.
- H23b recording timing/calibration/transient-feedback hardening: DIGITAL PASS; focused physical recording-timing acceptance still pending.
- H24/H24a Home Project Library: DIGITAL PASS; target-tablet UX smoke remains residual.

## H25 — UI/settings/delete safety — DIGITAL PASS
H25 provides:
- rounded-square hover/press/focus/ripple feedback matching the icon-button chassis;
- dedicated `Calibração` modal with route/rate/status/measurement/stability/compensation/residual controls;
- cleaner main Options page with compact calibration summary/action;
- consolidated `Diagnóstico` section without duplicate diagnostic entry points;
- explicit project-delete confirmation naming the project, warning about managed files and requiring confirmation before deletion;
- synchronized in-app Help;
- Android instrumentation for both delete paths and calibration-modal behavior.

Calibration remains optional: an uncalibrated status alone does not block REC, and unstable calibration is not applied.

H25 does not change DSP, project schema, managed-media layout or `.guitarlab` format.

## Current residual gate
Do **not** rerun deterministic CI merely to reconfirm #642. Use only the exact #642 signed APK for physical closure:
1. H25 rounded-square interaction, Settings/calibration modal, diagnostics and project-delete confirmation smoke on SM-X230;
2. retained H24 Home-library smoke;
3. retained H22 route smoke after MK-300 reconnect;
4. H23b zero-adjustment repeated-take recording-timing validation, especially 44.1 kHz;
5. backing-isolation/live-waveform/input-fail-closed/edit/export smoke;
6. explicit approval of signed APK SHA `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` as the authoritative physical checklist.
