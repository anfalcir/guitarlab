# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Signed authority through H24a
CI #641 / run `35105065689` / producer `b11769f340f7056c37dfb17d95b062909dad87bf` remains the latest signed DIGITAL PASS: 269/269 JVM/unit, Lint/build/provenance, API36 **25/25 standard + 1/1 isolated geometry**, signed homologation. Signed APK SHA-256: `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.

## H22–H24a retained
Semantic route UX, recording-timing/calibration hardening, transient-feedback discipline and Home Project Library remain as previously documented. H24/H24a are DIGITAL PASS at #641; H23b physical recording-timing acceptance remains pending.

## H25 — PRE-GATE
H25 refines release UX and destructive-action safety:
- rounded-square icon buttons now use matching rounded-square hover/press/focus/ripple feedback rather than circular interaction feedback;
- calibration status/details/measurement/fine adjustment move into a dedicated `Calibração` modal;
- main Options page keeps a compact calibration summary and route refresh only;
- diagnostic tools are consolidated into one `Diagnóstico` section;
- deleting a project now requires explicit confirmation naming the selected project and warning that managed files are removed;
- in-app Help is synchronized;
- focused Android tests cover delete Cancel/Confirm and calibration-modal visibility.

H25 does not alter project schema, media package format or audio DSP behavior. Calibration remains optional; REC is not blocked merely by `Não calibrada` status.

H25 is source-validated but requires one fresh full signed workflow before DIGITAL PASS. The #641 APK does not contain H25 and should not be used to validate these new behaviors.
