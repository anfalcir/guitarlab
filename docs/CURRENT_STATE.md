# Current state

Updated: 2026-09-09

## Repository truth
- stable baseline: `main`, signed `0.2.0-alpha03`;
- active branch: `dev/parallel-m3-m5` in draft PR #1;
- active candidate line: `0.2.0-alpha12`, versionCode 13;
- canonical CI: diff sanity, unit tests, Android Lint, debug APK; signed homologation only on explicit gate;
- certificate SHA-256 is locked to `0762D4F3ECB8E1A9AAA4BDB1E098A666C9BA9BF8B71C4F14B0E7A7065404E181`.

## Milestones
- M2: PASS/CLOSED physically on Samsung SM-X230 / Android 16/API 36 + Pocket Amp USB.
- M3: WAV codec/import foundation implemented.
- M4: managed media, waveform, playback, timeline, Mixer/Master, metering, routing/options and product polish implemented.
- M5.A/B/C: recording transaction, capture engine, routing enforcement, countdown, managed take finalization, waveform insertion, duplex/backing and coordinator implemented.
- M5 final status: **OPEN — alpha11 rejected physically; awaiting alpha12 validation**. Do not mark PASS/CLOSED yet.
- M6: blocked until M5 closes; first scope is measured round-trip latency, synchronization, take-position compensation, jitter and loopback.

## Physical evidence carried forward
- alpha09: broadly approved, with minor UX corrections identified.
- alpha10: Trim approved; distinction `Limpar pista` vs `Excluir pista` approved; both drag flows rejected; `Configurar pista` layout rejected/incomplete.
- video evidence `125843.mp4` reproduces the alpha10 drag failure: item detaches, ghost shifts/snaps and does not follow the pointer continuously.
- alpha11: software-green and correctly signed, but **physically rejected** on 2026-09-09 from video evidence `125888.mp4`. Both track-sidebar drag and clip/waveform drag flash briefly and cancel immediately, returning to origin. Track Settings was also rejected visually because the two-column arrangement left large unused space beside the color palette, separated the role label/value awkwardly and placed `Excluir pista` inside the content rather than with the final actions.

## Alpha12 corrective scope
- preserve the shared workspace drag coordinator, overlay ghost, real-bounds targeting and autoscroll architecture introduced in alpha11;
- fix the immediate drag cancellation root cause: entering drag state must not change the `pointerInput` key or disable/recreate the detector that owns the active pointer stream;
- keep drag permission stable for the lifetime of the gesture while disabling unrelated controls independently;
- track and clip drag must remain active after long press instead of flashing/cancelling;
- Track Settings uses one clear hierarchy: Name + Role, full-width responsive color palette, full-width source metadata, then footer actions `Excluir pista`, `Cancelar`, `Salvar`;
- delete remains disabled while the track contains clips and guidance to use `Limpar pista` remains visible;
- preserve approved Trim, recording, mixer, loop/playhead and clear/delete behavior.

## Active gate
Use only `M5_ALPHA12_FINAL_HOMOLOGATION_CHECKLIST.md`. M5 closes only after green CI + signed identity verification + explicit physical approval with zero repeatable P0/P1.
