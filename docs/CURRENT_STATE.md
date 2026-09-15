# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed **DIGITAL PASS**: CI #625 / run `35010012582` / source `476fa740408130adf6a4e9665d166e724a9184dd`.
- #625 signed APK SHA-256: `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`.
- #625 unsigned APK SHA-256: `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`.

## Evidence boundary
CI #625 remains the authoritative signed digital baseline through H16. A final physical-review correction, **H17**, is implemented after #625 and is currently **SOURCE-VALIDATED / PRE-GATE**. Because H17 changes product/UI source, the #625 APK must not be treated as evidence for H17.

## CI #625 — retained canonical baseline
Exact source `476fa740408130adf6a4e9665d166e724a9184dd` passed:
- materialization through H16;
- unit/core/audio/DSP/persistence/migration regression;
- performance evidence;
- Android Lint;
- debug/release assembly and unsigned provenance;
- API36 connected regression **22/22 PASS**;
- isolated 1920×1200 tablet geometry;
- signed homologation and package/version/source/signer/checksum verification.

## H17 — CUT ruler + Ajustes spacing — SOURCE-VALIDATED / PRE-GATE
Implementation commit: `41534dd2fb1ba7b0fc18459dbe5c622e78f039cb`.

### CUT
- visible T1/T2 time boxes are removed;
- CUT markers are reduced to short yellow ticks confined to the **time ruler**;
- ticks no longer extend upward into the sections/playhead rail;
- horizontal projection remains the same canonical timeline geometry as playhead/loop;
- waveform Trim handles remain the editing controls.

### Practice bar
- the `Ajustes` segment keeps its dedicated 16% block;
- the **Ajustes + Níveis** content cluster is centered inside that block instead of being left-anchored;
- this creates symmetric visual breathing room between Comparação and Timeline and prevents Níveis from visually colliding with comparison controls.

### Regression hardening
- `PhysicalEditingHardeningInstrumentedTest` now requires zero visible `T1 ` / `T2 ` text labels, exact X projection, and CUT ticks fully contained inside the time-ruler vertical bounds;
- `AutoSectionsSlotInstrumentedTest` now verifies Níveis remains entirely inside Ajustes and that the Ajustes+Níveis cluster has approximately equal left/right inset inside the center segment.

### Source representation and validation
Source part: `.source-parts/H17CutRulerPracticeSpacing.patch`

Patch SHA-256:
`38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`

Validated against the exact post-H16 materialized source emitted by #625:
- `patch --dry-run -p1`: PASS;
- forward patch application: PASS;
- reverse dry-run after application: PASS;
- `git apply --check`: PASS;
- `git diff --check`: PASS;
- changed Kotlin files: no parser-level syntax errors in the available local `kotlinc` parser pass;
- updated materializer: `bash -n` PASS.

Expected post-H17 blobs:
- `StudioPlaceholderScreen.kt`: `bc5f70ab81243589caea4179bc4bf8f07f8eeec7`
- `StudioUserGuideDialog.kt`: `839098bb7814b17060eaa60fe44306f28b1e1780`
- `AutoSectionsSlotInstrumentedTest.kt`: `9757f4ea5bd0be2961cdac9716a8cc213ae27c77`
- `PhysicalEditingHardeningInstrumentedTest.kt`: `8d1ee2bb3965b079e106756586b8bf0ff0d08bbb`

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H16: **DIGITAL PASS** at #625.
- H17: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

## Next authoritative gate
The user manually dispatches `GuitarLab Android CI` on the then-current `main` with signed homologation enabled. Required PASS: software/Lint/build/provenance, full API36 regression including the new H17 assertions, isolated 1920×1200 geometry, and signed homologation on the same source SHA.

The assistant must not dispatch or rerun Actions.
