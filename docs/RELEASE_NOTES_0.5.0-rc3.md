# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Last digitally homologated baseline
CI #624 / run `35005147318` / source `7858dca021a51e0e08835e3fa3f86e6d3b657215` is the current signed DIGITAL PASS through H15/H14a.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / code `23`;
- signed APK SHA-256 `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

#624 passed the complete software gate, API36 **22/22**, isolated 1920×1200 geometry and signed homologation.

## H16 candidate delta — PRE-GATE
These refinements are implemented after #624 and therefore require one new exact-source gate before they belong to the signed baseline.

### Practice bar clarity
- Comparison neutral state is labeled **Desativado** instead of Mixer.
- The independent top-bar Mixer open/close control keeps the name **Mixer**.
- `Níveis` is moved into a dedicated **Ajustes** block between Comparação and Timeline.
- The docked bar is proportioned for content rather than split into equal segments: Comparação 34%, Ajustes 16%, Timeline 50%.

### Trim presentation correction
- T1/T2 now share the exact horizontal timeline geometry used by playhead/loop markers; the ruler no longer subtracts 8 dp at the right edge.
- The dedicated-looking Trim strip is removed: ruler height returns to 20 dp and directly touches the marker rail.
- The H13 header allocation is reduced by 30 dp.
- During Cut, yellow T1/T2 lines and precise labels overlay the existing marker rail/ruler with higher visual priority than playhead/loop.
- Trim handles continue to live in the waveform and remain independently draggable.

### Regression hardening
- practice-bar instrumentation verifies the new three-segment organization and Níveis placement;
- Trim instrumentation verifies no reserved Trim lane and validates T1 horizontal alignment against the canonical timeline rail after a real handle drag.

H16 patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

Source-level patch application checks pass against the exact #624 materialized source. No Android runtime/signing claim is made for H16 until the next manual CI succeeds.

## Next candidate rule
The next signed RC3 artifact is valid only if a manually dispatched `GuitarLab Android CI` on the exact final `main` SHA passes software/Lint/build, API36, 1920×1200 geometry and signing with matching package/version/source/signer/checksums.
