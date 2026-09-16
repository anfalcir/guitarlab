# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Current signed digital homologation — CI #642
Run `35121955150`, exact producer source `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, is the current signed DIGITAL PASS through H25.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `f6b4f21d0f514bad06b80eabdad141dac5cd236a707842c21258ec2072868c0e`;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID `10458236596`.

CI #642 evidence:
- **269/269** JVM/unit PASS;
- Android Lint/build/unsigned provenance PASS;
- API36 **28/28 standard + 1/1 isolated geometry**;
- signed homologation PASS;
- v2 signature, one RSA-4096 signer, locked-certificate match PASS.

## H25 — DIGITAL PASS
H25 refines release UX and destructive-action safety:
- rounded-square icon buttons use matching rounded-square hover/press/focus/ripple feedback;
- calibration status/details/measurement/fine adjustment live in a dedicated `Calibração` modal;
- main Options page keeps a compact calibration summary/action;
- diagnostic tools are consolidated into one `Diagnóstico` section;
- deleting a project requires explicit confirmation naming the selected project and warning that managed files are removed;
- in-app Help is synchronized;
- Android tests cover delete Cancel/Confirm and calibration-modal visibility/behavior.

Calibration remains optional; `Não calibrada` by itself does not block REC. H25 does not alter project schema, media package format or audio DSP behavior.

## Release decision
CI #642 is the signed authority and the #642 APK is the active physical candidate. No new deterministic CI is required merely to reconfirm it. Remaining work is residual physical homologation on SM-X230 + MK-300.
