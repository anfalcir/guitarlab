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
Physical use of the #616 candidate exposed additional workflow inconsistencies. The following changes are now implemented but await a new exact-source digital gate.

### Editing entry reliability
- `Cortar` is now a first-valid-tap action: one tap opens Trim when allowed.
- blocked trim entry produces explicit feedback instead of a silent no-op.

### Level analysis convergence
- level recommendation is based on the effective signal after current track/clip gain rather than repeatedly measuring raw PCM as if no gain had been applied;
- applying a recommendation updates derived playback/mixer/history state;
- re-analysis converges instead of proposing the identical correction indefinitely.

### History/state hardening
- project mutations resynchronize `canUndo`/`canRedo` and other derived UI state;
- asynchronous history/analysis callbacks are project/session guarded so results from an old project cannot overwrite the newly opened project;
- repeated level/history operations are covered by stress regression.

### Clearer destructive actions
- `Excluir clipe` remains clip/segment scoped;
- track-wide content clearing is labeled `Limpar toda a pista` in track configuration rather than appearing as a second neighboring trash action in the clip pencil flow.

### Mixer layout optimization
- Comparison/Timeline controls are reused inside the Mixer header on wide layouts, with `Mixer` anchored left and Pin/Close anchored right;
- closing Mixer returns the same control component to normal workspace flow, avoiding duplicated state/implementations.

### Stop during recording
- Stop while capture is active calls the same idempotent successful finalization routine as tapping REC again;
- Stop during countdown cancels safely;
- finalizing state rejects duplicate stop/finalize requests.

### Live waveform spatial stability
The physical video showed that H5's pairwise bounded compaction still produced sparse old material and dense recent material. H9 replaces that behavior with uniform temporal bucketing:
- one global temporal resolution applies consistently to historical and new envelope data;
- represented intervals retain full temporal width in the renderer;
- maximum transient is retained per bucket;
- long/variable-cadence capture regression checks monotonic coverage and stable density.

## Validation status
- CI #616: H0–H6 DIGITAL PASS.
- H7–H10: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.
- A new manually dispatched exact-source workflow is required before the next APK may be promoted for physical homologation.
