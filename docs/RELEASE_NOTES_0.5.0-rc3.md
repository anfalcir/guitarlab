# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-14

RC3 consolidates the final practice, transport, editing, recording and release-hardening work.

## Previously established RC3 behavior
- loop-aware bounded Play and live playhead seek;
- Auto seções preview/application and project-end clipping;
- centered translucent `3 → 2 → 1` REC countdown;
- shared Home/Studio user guide;
- independent trim handles;
- safe split/take lineage;
- explicit clip deletion and confirmed drag-to-trash;
- measured session startup skew separate from route latency;
- frame-based live recording waveform;
- source materialization and signed provenance gates.

CI #616 digitally homologated H0–H6 against source `3051619c219e346daca00d2242f60ef03f2d80db`. Signed APK SHA-256: `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`.

## Physical Review II — H7–H10
Physical use of the #616 candidate exposed additional workflow inconsistencies. They are now implemented and digitally validated in CI #617.

### Editing entry reliability
- `Cortar` is a first-valid-tap action;
- blocked trim entry produces explicit feedback instead of a silent no-op.

### Level analysis convergence
- recommendation uses effective signal after current track/clip gain;
- applying a recommendation resynchronizes playback/mixer/history-derived state;
- re-analysis converges instead of repeating the same correction indefinitely.

### History/state hardening
- project mutations resynchronize `canUndo`/`canRedo` and related derived state;
- asynchronous history/analysis callbacks are project/session guarded;
- repeated level/history operations are covered by stress regression.

### Clearer destructive actions
- `Excluir clipe` remains clip/segment scoped;
- track-wide content clearing is `Limpar toda a pista` in track configuration.

### Mixer layout optimization
- Comparison/Timeline controls reuse the Mixer header on wide layouts, with `Mixer` left and Pin/Close right;
- closing Mixer returns the same component to normal workspace flow.

### Stop during recording
- Stop during active capture uses the same idempotent successful finalization path as tapping REC again;
- Stop during countdown cancels safely;
- finalizing rejects duplicate stop/finalize requests.

### Live waveform spatial stability
H9 replaces uneven pairwise historical compaction with uniform temporal bucketing:
- one current temporal resolution applies consistently to historical and new envelope data;
- represented intervals retain full temporal width in the renderer;
- maximum transient is retained per bucket;
- long/variable-cadence capture regression checks monotonic coverage and stable density.

## Validation status — CI #617
Manual workflow #617 / run ID `34918430241` passed all mandatory jobs against exact source `abc0e2a9f8708dd141735915898b508ce0948f48`:
- Unit tests + Lint + APK build — PASS;
- API 36 emulator regression — PASS;
- Signed homologation APK — PASS.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / code 23;
- unsigned APK SHA-256 `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`;
- signed APK SHA-256 `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2, one RSA-4096 signer;
- artifact ID `10377695262`.

The downloaded signed artifact checksum was independently recomputed and matched `SHA256SUMS.txt` exactly.

Only the focused Samsung SM-X230 + M-VAVE MK-300 physical checklist now remains before final M7/M8 closure.
