# M5 alpha11 final homologation checklist

Status: **REJECTED — superseded by alpha12**
Version: `0.2.0-alpha11` / versionCode 12
Device baseline: Samsung SM-X230 / Android 16(API 36).

## Physical result — 2026-09-09
Video evidence `125888.mp4` rejected this candidate.

### P1 — drag gesture lifecycle
Both track-sidebar reorder and clip/waveform migration begin visually for an instant, flash, then cancel and return to the origin. Root cause identified after the test: `dragState` becoming active changed `canEditClip` from true to false; because that value was part of the local `pointerInput` key, Compose recreated/cancelled the gesture detector that owned the pointer stream. The same coupling affected both drag surfaces.

### UX — Configurar pista
The dialog was also rejected visually. The landscape two-column layout left excessive unused space beside the palette, Function was presented as a disconnected label/value stack, and `Excluir pista` appeared inside the identity content instead of in the final action area.

## Preserved approvals
- Trim behavior remains approved from prior physical testing.
- `Limpar pista` versus `Excluir pista` semantics remain approved.
- recording/mixer/loop/playhead behavior was not rejected by this evidence.

## Disposition
Do not use alpha11 for M5 closure. The active gate is `M5_ALPHA12_FINAL_HOMOLOGATION_CHECKLIST.md`. M5 remains OPEN and PR #1 remains draft.
