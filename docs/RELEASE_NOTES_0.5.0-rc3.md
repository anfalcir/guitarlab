# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Last signed digital homologation — CI #633
Run `35033323990`, exact source `2204e0272f9e6e2f218bebd36889db424e006e03`, is the signed DIGITAL PASS through H21.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `58165dc53cacaf39357a1f28315b6312ada3ed2b6669b59e32ddbe8e4d53c25d`;
- signed APK SHA-256 `f40b24cb36b4cb1299efcb28a35b54e66b107af2ec3d578a870c70e2966ff52e`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI evidence:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 standard connected regression: 23/23 PASS;
- isolated 1920×1200 geometry: 1/1 PASS;
- signed homologation: PASS.

## H20 — physical output canonicalization v2 — DIGITAL PASS
- built-in speaker logical endpoints for one device collapse into one physical output choice;
- the reproduced SM-X230 family (`SM-X230`, `• 0`, `• back`, `• bottom`) is covered by regression;
- earpiece/Bluetooth/HDMI remain distinct;
- USB H19 behavior is retained;
- duplicate candidate resolution uses a silent probe plus actual `routedDevice` confirmation;
- all 8 `StudioAudioRoutePolicyTest` regressions passed at #633.

## H21 — Studio visual system overhaul — DIGITAL PASS
- global hardware-inspired geometry scale around 6dp controls, 8dp internal rows/cards and 10dp major panels/dialogs;
- circles limited to genuine circular semantics;
- icon actions use a visible square chassis with preserved touch targets;
- Home, Novo Projeto, Studio, Mixer, Options, diagnostics, help/dialogs and track configuration inherit the normalized geometry;
- practice bar renders Comparação, Ajustes and Timeline as separate semantic chassis with fixed title areas and blue/teal/amber accents;
- wide/narrow containment regressions and target-tablet geometry pass.

## CI #632 transport incident
#632 failed before Android compilation because the H21 source-part archive was corrupted during repository transport and failed gzip CRC/length validation. Source materialization was hardened without changing the intended H20/H21 product behavior; #633 then materialized and validated the complete source successfully.

## Release decision
H20/H21 are digitally homologated at #633. The remaining decision depends only on residual physical validation of real output enumeration/audibility/reconnect on SM-X230 + MK-300, the new visual hierarchy on the real tablet, and the retained recording/routing/listening smoke. If those pass with no further product/source changes, RC3 may proceed without another digital CI run.
