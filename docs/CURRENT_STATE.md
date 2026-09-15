# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Canonical digitally homologated application/source SHA: `abc0e2a9f8708dd141735915898b508ce0948f48`.
- Canonical workflow: CI #617 / run ID `34918430241` — SUCCESS.
- Signed APK: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`.
- Signed APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.
- Unsigned release SHA-256: `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact ID: `10377695262`.
- `.github/workflows/android-ci.yml` remains manual-only. Ordinary commits use `[skip ci]`.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: complete digital scope PASS for the active RC; only final Samsung SM-X230 + M-VAVE MK-300 physical gate remains open.
- M8: H0–H10 DIGITAL PASS. Final closure depends only on residual target-device validation and explicit approval.

## Authoritative digital evidence — CI #617
Manual workflow #617 completed successfully against exact source `abc0e2a9f8708dd141735915898b508ce0948f48`.

All mandatory jobs passed:
- Unit tests + Lint + APK build — PASS;
- API 36 emulator regression — PASS;
- Signed homologation APK — PASS.

The run established source materialization, JVM/unit regression including H7–H10, performance evidence, Android Lint, debug/release assembly, complete API 36 instrumentation, isolated 1920×1200 target geometry, unsigned release provenance, signed homologation, package/version identity and locked signer verification.

`BUILD_IDENTITY.txt` records:
- `commit=abc0e2a9f8708dd141735915898b508ce0948f48`;
- `package=studio.guitarlab.app`;
- `versionName=0.5.0-rc3`;
- `versionCode=23`;
- `unsignedApkSha256=2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`;
- `signedApkSha256=7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`;
- `gate=software+android-integration-passed;physical-validation-pending`;
- `certificateSha256=4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

The downloaded artifact checksum was recomputed independently and matched `SHA256SUMS.txt` exactly. Signing verification passed with APK Signature Scheme v2, one RSA-4096 signer and the locked certificate.

## Physical Review II — H7–H10 — DIGITAL PASS
### H7 — state/history, level analysis and recording stop
- project mutations resynchronize history-derived `canUndo`/`canRedo`, playback/mixer/readiness state;
- asynchronous history/analysis callbacks are project/session guarded;
- level analysis reasons about effective level after current gain so apply → re-analyze converges;
- Stop and REC-during-capture share one idempotent successful finalization path.

### H8 — workspace flow and action semantics
- `Cortar` is first-valid-tap with explicit feedback when blocked;
- clip-level `Excluir clipe` and track-wide `Limpar toda a pista` have distinct scopes;
- Comparison/Timeline is reused inside the Mixer header and returns to normal workspace flow when Mixer closes.

### H9 — live REC waveform spatial stability
- uniform temporal bucketing replaces uneven historical pairwise compaction;
- old/new waveform material shares one active temporal resolution;
- each bucket renders across its represented interval;
- long/variable-cadence regression covers monotonic coverage, stable density and transient retention.

### H10 — race closure, integrated regression and guide sync
- project-switch/history/analysis races are guarded;
- integrated regression covers level convergence, history stress, first-tap trim, Stop-during-REC and long waveform behavior;
- `StudioUserGuideDialog` is synchronized;
- H7–H10 materialization is guarded/idempotent.

## Residual physical gate
Only target-hardware/ergonomic facts remain:
- first-tap `Cortar` reliability and trim-handle feel;
- level analysis → apply → analyze convergence plus Undo/Redo responsiveness;
- transport/history responsiveness after repeated edits and project switching;
- Stop and REC both ending capture successfully;
- Mixer-header Comparison/Timeline ergonomics/no collision;
- 2–3 minute live REC waveform spatial/temporal stability;
- MK-300 routing/isolation, guitar-vs-backing alignment, listening smoke and representative export.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the active checklist. Final M7/M8 closure requires explicit approval of this exact signed APK.
