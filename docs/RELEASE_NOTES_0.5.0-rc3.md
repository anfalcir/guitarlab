# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Last signed digital homologation — CI #631
Run `35025012392`, exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c`, is the signed DIGITAL PASS through H18/H18a/H19.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`;
- signed APK SHA-256 `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Physical findings after #631
The adaptive comparison bar no longer clips `Ambas`, but physical review exposed:
- the output selector still showed several built-in SM-X230 logical speaker endpoints (`SM-X230`, `• 0`, `• back`, `• bottom`);
- the practice bar needed stronger title/action/group hierarchy;
- application geometry still mixed compact hardware-style controls with larger rounded/pill surfaces elsewhere.

## H20 candidate delta — physical output canonicalization v2 — PRE-GATE
- built-in speaker logical endpoints for one device collapse into one physical output choice;
- address suffixes used only to identify logical speaker endpoints are not exposed as separate user destinations;
- earpiece/Bluetooth/HDMI remain distinct;
- USB H19 behavior is retained;
- duplicate candidate resolution still uses a silent probe plus actual `routedDevice` confirmation;
- new policy tests reproduce the four-entry SM-X230 topology exactly.

## H21 candidate delta — Studio visual system overhaul — PRE-GATE
- global hardware-inspired geometry scale: 6dp controls, 8dp internal rows/cards, 10dp major panels/dialogs;
- circles limited to genuine circular semantics;
- icon actions receive a visible square chassis and preserved 48dp touch target;
- surface and outline contrast strengthened;
- Home, Novo Projeto, Studio, Mixer, Options, diagnostics, help/dialogs and track configuration inherit the normalized geometry;
- practice bar now renders Comparação, Ajustes and Timeline as separate bordered chassis with fixed title areas;
- blue identifies Comparação, teal identifies Ajustes and amber identifies Timeline;
- action buttons inside each group inherit the same functional accent family;
- regression coverage checks group containment and title-before-action geometry.

## Release decision
H20/H21 change product source, so #631 is retained as historical signed authority but is no longer the final physical candidate for these changes. One new exact-source manually dispatched full gate is required before a new APK can enter final physical homologation.
