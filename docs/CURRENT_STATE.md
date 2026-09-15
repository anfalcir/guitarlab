# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active candidate: `0.5.0-rc3`, versionCode `23`.
- Canonical digitally homologated source SHA: `3051619c219e346daca00d2242f60ef03f2d80db`.
- Signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`.
- Signed APK SHA-256: `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`.
- Unsigned release SHA-256: `a5fd846bc2fb54a2995ab7fd48f3fcef0055991e6b9678b05f641e739981d0e2`.
- Locked homologation signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Package: `studio.guitarlab.app`.
- Ordinary commits remain `[skip ci]`; `.github/workflows/android-ci.yml` is manual-only.

## Milestone state
- M2 through M6: PASS/CLOSED.
- M7: automated/digital scope PASS for the exact active RC; final Samsung SM-X230 + M-VAVE MK-300 physical gate remains open.
- M8 digital hardening: H0–H6 PASS on the exact active RC; final release closure still depends on residual target-device validation and explicit approval.

## Authoritative digital evidence — CI #616
Manual workflow **#616** (run ID `34912716297`) completed successfully against exact source `3051619c219e346daca00d2242f60ef03f2d80db`.

All mandatory jobs passed:
- Unit tests + Lint + APK build — PASS;
- API 36 emulator regression — PASS;
- Signed homologation APK — PASS.

The run established:
- source materialization PASS;
- unit/JVM regression PASS, including H0–H6 editing/timing/waveform coverage;
- performance evidence PASS;
- Android Lint PASS;
- debug/release assembly PASS;
- full API 36 instrumented regression PASS;
- isolated 1920×1200 target-geometry PASS;
- unsigned release provenance/identity PASS;
- signed homologation PASS;
- APK Signature Scheme v2 PASS;
- one official signer, RSA 4096;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89` PASS;
- package/version identity PASS;
- final artifact identity/checksum PASS.

`BUILD_IDENTITY.txt` records `gate=software+android-integration-passed;physical-validation-pending`. The H0–H6 source is therefore digitally homologated, but final physical homologation is intentionally still open.

## RC3 retained scope
### Transport, sections and layout
- Loop Play begins only inside `[loopStart, loopEnd)` and ends naturally at `L▶`, returning to `L◀`.
- Ordinary natural completion returns to project start; manual Stop preserves the current position.
- Playhead can be sought while ordinary Play is active; active Loop clamps seek inside its bounds.
- `Auto seções` produces a non-persistent preview, normalizes edge-adjacent boundaries and never renders beyond the real song end.
- `Auto seções` owns a stable slot; preview replaces that slot with `Aplicar` + red `X` without moving neighboring controls.
- Comparação/Timeline use the available width responsively; `Limpar seções` remains in normal Timeline flow.
- REC countdown is a large centered translucent `3 → 2 → 1` overlay and never changes workspace geometry.
- Home and Studio use the same `StudioUserGuideDialog` implementation.

### Physical editing/recording hardening — H0–H6 — DIGITAL PASS
- H0: deterministic reproduction/invariant coverage for trim, split-lineage move/delete, timing and long live waveform.
- H1: independent start/end trim handles with dedicated touch targets, semantics and deterministic frame mapping.
- H2: recording-take lineage survives split/move/delete; canonical sibling promotion is deterministic; `Excluir clipe` is explicit and confirmed.
- H3: drag/drop resolves to `Move`, `Delete` or `NoOp`; drag-to-trash uses the same domain deletion path and stale drops revalidate before mutation.
- H4: recording synchronization measures per-session capture/backing startup skew separately from route calibration/punch offsets; no hard-coded `-0.5 s` correction is used.
- H5: live REC waveform is frame-span based, compacts without losing time coverage/transients and publishes UI updates through a bounded/conflated cadence.
- H6: integrated persistence, instrumentation, timing, waveform, guide synchronization and serial materialization regression passed in CI #616.

## Residual physical gate
Only target-hardware facts remain:
- reliably grab/move both trim handles on the Samsung tablet;
- split a recorded clip, move one child to an empty compatible track, delete a child through both `Excluir clipe` and drag-to-trash, then save/reopen/Undo/Redo;
- confirm selected MK-300 input remains fail-closed and backing is not recorded into the guitar take;
- record guitar against backing and confirm there is no repeatable systematic late placement like the previously observed ~0.5 s;
- record continuously for 2–3 minutes and confirm live waveform remains temporally stable without acceleration/backward piling;
- perform a concise listening/transport/export/stress smoke on the real device.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active manual checklist. Final M7/M8 closure requires explicit approval of this exact SHA/APK.
