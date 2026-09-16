# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: **CI #641** / run `35105065689` / exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf`.
- Unsigned APK SHA-256: `824e8c070ebee6bbf920990dce3c948d2fc3474610524f3a087c38bd27ca5248`.
- Signed APK SHA-256: `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`); the assistant must not dispatch or rerun it.

## Evidence boundary
CI #641 is authoritative through **H24a**. H25 changes product/test source after #641 and is therefore **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until a fresh user-dispatched full signed workflow passes on the exact H25 source SHA.

The #641 APK remains valid historical/signed evidence through H24a, but it must not be used to physically judge H25 behavior.

## CI #641 — retained DIGITAL PASS
- JVM/unit: **269/269 PASS**, 0 failures/errors/skips;
- Android Lint/build/unsigned provenance: PASS;
- standard API36: **25/25 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation and locked signer: PASS.

Canonical Android reporting for #641 is **25/25 standard + 1/1 isolated geometry**.

## H22–H24a retained state
- H22/H22a semantic route UX: DIGITAL PASS + focused SM-X230 PHYSICAL PASS.
- H23b recording timing/calibration/transient-feedback hardening: DIGITAL PASS; focused physical recording-timing acceptance still pending.
- H24/H24a Home Project Library: DIGITAL PASS at #641; target-tablet UX smoke remains residual.

## H25 — UI/settings/delete safety — PRE-GATE
H25 adds:
- rounded-square hover/press/focus/ripple feedback matching square icon-button geometry;
- a dedicated calibration modal, leaving only a compact calibration summary/action on the main Options page;
- consolidated diagnostic actions in one `Diagnóstico` section;
- explicit project-deletion confirmation before repository deletion;
- synchronized in-app Help;
- focused Android instrumentation for calibration-modal visibility and destructive-delete confirmation.

H25 does not change DSP, project schema, managed-media layout or `.guitarlab` format.

Source validation is complete: exact #641-source patching, final blob hashes, idempotency, reverse/reapply, corruption fail-closed, shell syntax, diff checks and parser scan all pass. Android Gradle/API36/signing still require the canonical manual workflow.

## Next gate
Do not rerun #641. When the current H25 source is ready for a candidate, manually run:
`Actions → GuitarLab Android CI → main → signed_homologation=true`.

Required for H25 promotion: software/unit/Lint/build/provenance PASS, standard API36 PASS, isolated 1920×1200 geometry PASS and signed homologation PASS on the exact same new `head_sha`.

After H25 DIGITAL PASS, resume residual physical homologation using only that new signed APK.
