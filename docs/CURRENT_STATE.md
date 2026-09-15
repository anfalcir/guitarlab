# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #631 / run `35025012392` / exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c`.
- Signed APK SHA-256: `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`.
- Unsigned APK SHA-256: `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`; the assistant must not dispatch or rerun it.

## Evidence boundary
CI #631 remains authoritative through H18/H18a/H19. A physical review of the #631 APK exposed two post-gate product issues, so #631 is no longer the final physical candidate once H20/H21 are introduced:
1. the output selector still exposed multiple logical endpoints for the tablet's built-in speaker (`SM-X230`, `SM-X230 • 0`, `SM-X230 • back`, `SM-X230 • bottom`);
2. the practice bar was functionally laid out but still lacked sufficient visual hierarchy between group title, action buttons and adjacent groups, and the application used inconsistent large/pill-like corner radii across screens.

## H20 — physical output canonicalization v2 — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
The H19 diagnosis was correct but its scope was too narrow: only USB-family endpoints were canonicalized. Android also exposes one physical built-in speaker as several logical `AudioDeviceInfo` outputs.

H20 now:
- groups built-in speaker and built-in speaker-safe endpoints by physical product identity while ignoring logical endpoint address suffixes such as `0`, `back` and `bottom`;
- presents one user-facing label for the physical speaker (`SM-X230` in the reproduced case);
- preserves earpiece, Bluetooth profiles, HDMI and other materially different output classes as distinct routes;
- preserves H19 USB canonicalization and legacy-signature migration;
- keeps duplicate candidate endpoint resolution based on a short inaudible `AudioTrack` probe and authoritative `routedDevice` confirmation;
- no longer rejects mono-looking logical speaker endpoints before probing, because Android may route a stereo track through another endpoint in the same canonical physical group.

New route-policy tests cover the exact four-endpoint SM-X230 topology, legacy-selection migration and earpiece/speaker separation.

Source part: `.source-parts/H20PhysicalOutputCanonicalization.patch.gz`

Decoded patch SHA-256: `ef1846efa67d181c538580560042966385f0ae9974c465c96d5471f505a0bd08`.
Encoded source-part SHA-256: `badf59836e2a7f51549717105423f9e0e211ab5bbcf23ec1cd44aa204f0d3796`.

Expected H20 blobs:
- `StudioAudioRoutePolicy.kt`: `4dbc797e1ce141de6c684d2a93045b0c1a9c3c22`
- `StudioAudioRoutingStore.kt`: `4ccd07aa09df82c54ce31c00ae8701e82f07e9f7`
- `StudioAudioRoutePolicyTest.kt`: `8c04db9ed4f4f8b7c0a82cc2142e391205076309`

## H21 — Studio visual system overhaul — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
H21 standardizes visual geometry across the entire app and strengthens semantic grouping without changing audio/project behavior.

Geometry contract:
- general controls: 6dp corner radius;
- internal cards/rows: 8dp;
- major panels/dialogs: 10dp;
- circular geometry is reserved for genuinely circular semantics such as the REC countdown;
- track color choices are now rounded squares instead of circles.

Global application:
- Material3 theme shapes now follow the 4/6/8/10dp hardware-inspired scale;
- oversized explicit 12–24dp rounded surfaces were normalized screen-by-screen;
- icon actions now render inside a visible square chassis with a 6dp radius, border and preserved 48dp touch target;
- outline contrast and surface layering were strengthened in dark and light themes.

Practice bar hierarchy:
- each of `Comparação`, `Ajustes` and `Timeline` has an independent bordered chassis;
- the group title sits in a fixed non-action header area separated from the action strip;
- functional accents are consistent: blue = Comparação, teal = Ajustes, amber = Timeline;
- comparison buttons use the blue family, Níveis uses teal, Timeline actions use amber;
- title/action boundaries and segment non-overlap are regression-guarded at the target-tablet logical width;
- narrow layout keeps the same semantic groups stacked with local overflow only.

The review covers Home, Novo Projeto, Studio top bar, Transport, Timeline/track cards, Mixer, Options, Audio diagnostics, Codec diagnostics, track settings/dialogs and help/export surfaces through global theme plus targeted explicit-shape normalization.

Source part: `.source-parts/H21StudioVisualSystem.patch.gz`

Decoded patch SHA-256: `b7bc0387faaac36e974cf1a0c38638a45319a5a2a0be6d356340a4e1fbe8dc7d`.
Encoded source-part SHA-256: `dd02dde024ba3fa00267b2b02e3d3a3878dcbf70b276c9e35e972b58de8a3981`.

Key expected H21 blobs:
- `AppIconButton.kt`: `e9628125c970712023f73d4d3bfd8e4df9ed5db5`
- `SettingsScreen.kt`: `320476303d0314be13d2a0bb47c8da58ac623969`
- `StudioPlaceholderScreen.kt`: `4868b26b4bbdd256450a093494c3bb73743a5892`
- `AutoSectionsSlotInstrumentedTest.kt`: `479d4ba4bce14b7aad7a303fd759bf33abb27d8c`
- `Theme.kt`: `5b09422ebd9f507cbb9ae603baa3e2d0a921c776`
- `Color.kt`: `bc2bddc6222c5a1c4cd69fcdf039d8c785eb3152`

## Source validation
Against the exact materialized source emitted by CI #631:
- H20 forward `patch --dry-run`: PASS;
- H20 application: PASS;
- H21 forward `patch --dry-run` after H20: PASS;
- H21 application: PASS;
- complete H21→H20 reverse round-trip back to #631 source: PASS;
- `git diff --check`: PASS;
- updated materializer `bash -n`: PASS;
- pure `StudioAudioRoutePolicy` compiled locally with `kotlinc`: PASS;
- SM-X230 four-endpoint canonicalization smoke test: PASS;
- Kotlin parser scan of the changed Compose files produced no syntax diagnostics; unresolved Android/Compose dependencies are expected in the local non-Gradle environment.

No Android runtime or signed-candidate PASS is claimed for H20/H21 before the next user-dispatched CI.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H19/H18a: DIGITAL PASS at CI #631.
- H20/H21: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.

## Next authoritative gate
The user manually dispatches `GuitarLab Android CI` on the exact H20/H21 source with signed homologation enabled. Required evidence: software/unit/Lint/build/provenance, route-policy tests, complete API36 connected regression, isolated 1920×1200 geometry and signed homologation on one exact source SHA.

After that gate, final physical review should verify one user-facing SM-X230 speaker route, one MK-300 route when connected, audible playback/reconnect behavior, and the new visual hierarchy on the real tablet.
