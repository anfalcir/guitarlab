# M5 — alpha13 final homologation checklist

Candidate: `0.2.0-alpha13` / versionCode 14
Target: Samsung SM-X230, Android 16/API36
Audio baseline: Pocket Amp USB where required
Status before device test: software/signing gates must be green; M5 remains OPEN.

## 1. Installation and identity
- [ ] APK installs/updates normally.
- [ ] Version shown/verified as `0.2.0-alpha13` / 14.
- [ ] Signed artifact identity/hash matches delivered BUILD_IDENTITY/SHA256SUMS.
- [ ] Existing projects remain listed/openable after update.

## 2. Studio regression — no P0/P1
- [ ] Track long-press/drag follows finger continuously; no flash/cancel.
- [ ] Track drop reorders exactly once and Undo restores prior order.
- [ ] Edge autoscroll works while dragging beyond visible lanes.
- [ ] Clip/waveform long-press/drag follows finger continuously.
- [ ] Clip can migrate between tracks without losing trim/gain/mute/source metadata; Undo works.
- [ ] Populated track shows pencil/edit icon rather than generic overflow for content edit.
- [ ] `Configurar pista` looks balanced in landscape and portrait; metadata/palette/name/function are readable.
- [ ] `Limpar pista` removes content but preserves track identity/settings.
- [ ] `Excluir pista` remains a structural action and is safe for non-empty tracks.
- [ ] Trim start/end/apply/cancel regressions pass.
- [ ] Split, duplicate and remove clip pass.
- [ ] Loop/playhead/return-to-start pass.
- [ ] Mixer gain, bipolar Pan, Mute, Solo, REC Arm and Master pass.
- [ ] Recording/countdown/managed take/backing path passes with Pocket Amp where applicable.

## 3. Import matrix
Use representative real files. For every accepted file: import, waveform, playback, source metadata, Trim and project reopen must work.
- [ ] WAV PCM.
- [ ] FLAC.
- [ ] AIFF PCM or supported AIFC PCM (`NONE`/`twos`/`sowt`).
- [ ] MP3.
- [ ] AAC/M4A.
- [ ] OGG Vorbis.
- [ ] Opus.
- [ ] Corrupt/unsupported input fails cleanly without leaving a broken clip.
- [ ] A source/project sample-rate mismatch is not silently played at wrong speed/pitch; unsupported mismatch is rejected/explained.

## 4. Managed source/proxy behavior
- [ ] Imported non-WAV audio remains usable after the original external document is moved/unavailable.
- [ ] Trim/move/split do not alter the source identity.
- [ ] Waveform/playback use the managed editing representation transparently.

## 5. Portable project `.guitarlab`
- [ ] Top bar order includes Mixer → Opções → Compartilhar → Home.
- [ ] Share opens a polished `Salvar e exportar` modal.
- [ ] `Projeto GuitarLab` creates a `.guitarlab` package.
- [ ] Home > `Abrir projeto` restores that package.
- [ ] Restore creates an independent project entry rather than overwriting the original.
- [ ] Track names/order/colors/roles/mix state survive.
- [ ] Clips, timeline placement, trim/source offsets, mute/gain and media survive.
- [ ] Restored project waveforms/playback work.
- [ ] Invalid/incompatible package fails without corrupting existing projects.

## 6. Master export
Prepare a short project with at least two audible tracks and identifiable pan/gain/mute state.
- [ ] WAV 32-bit float exports successfully.
- [ ] WAV file is playable, non-empty, correct duration and reflects the current mix.
- [ ] FLAC exports successfully and is playable/non-empty/correct duration.
- [ ] MP3 320 kbps exports successfully on the target device and is playable/non-empty/correct duration.
- [ ] Export respects Mute/Solo, track gain/pan, clip gain/trim/placement and Master gain.
- [ ] Export failure never leaves the project damaged.

## 7. UX and stability
- [ ] Share modal is readable and visually consistent; project save is visually separate from master formats.
- [ ] Output actions are not duplicated as primary actions inside Opções.
- [ ] No repeatable crash/ANR during import, restore or export.
- [ ] Rotation/navigation does not corrupt the project.

## Final disposition
- [ ] Zero repeatable P0/P1.
- [ ] User explicitly approves M5 as PASS/CLOSED.

Only after both final boxes are checked may M6 begin.
