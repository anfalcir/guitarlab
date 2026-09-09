# M5 alpha12 final homologation checklist

Status: **ACTIVE — physical approval pending**
Version target: `0.2.0-alpha12` / versionCode 13
Device baseline: Samsung SM-X230 / Android 16(API 36); Pocket Amp where audio hardware is relevant.

## Preconditions
- install alpha12 over the prior signed build without uninstalling;
- confirm the project opens and existing managed media remains intact;
- M5 remains OPEN during this checklist;
- alpha11 is REJECTED and must not be used for closure.

## 1. Reorder de pistas — regression target P1
- long press on the track sidebar must enter drag once and stay active; **no flash/cancel immediately after selection**;
- ghost follows the finger continuously without jump/lock;
- selected track remains visually coherent during the gesture;
- insertion indicator updates continuously;
- move first↔last and intermediate positions;
- cancel causes zero mutation;
- same-origin drop causes zero history entry;
- IDs, colors, role, mix and clip ownership remain intact.

## 2. Migração de waveform/clipe — regression target P1
- long press on the clip/waveform must enter drag once and stay active; **no flash/cancel immediately after selection**;
- ghost follows the finger above all lanes;
- target lane is highlighted/named;
- moving to another track preserves source, timing, trim, gain, mute and metadata;
- same-track drop/cancel is no-op;
- Undo restores prior track; Redo reapplies.

## 3. Autoscroll
- hold near top: continuous upward scroll if content exists;
- hold near bottom: continuous downward scroll if content exists;
- speed increases toward the edge but stays controlled;
- leaving the edge stops immediately;
- ghost/indicator remain visible and target stays correct after scrolling;
- explicitly test a drag long enough for the source lane to approach or leave the visible viewport.

## 4. Configurar pista — visual acceptance
Verify both landscape and portrait/tablet widths.
- header is compact and visually balanced;
- Name and Function share one coherent identity row on wide layouts; on narrow layouts they stack cleanly;
- Function is presented as one compact card/field, not a disconnected heading with value floating below;
- `Cor da pista` uses the full available width with a responsive palette (10 per row when wide, 5 per row when narrow), with no large unused blank area beside it;
- `Áudio fonte` appears below the identity/color area at full width and displays source metadata cleanly;
- when the track is non-empty, delete guidance remains visible;
- final actions are grouped at the bottom: `Excluir pista`, `Cancelar`, `Salvar`;
- `Excluir pista` stays disabled until the track is empty;
- name validation remains 1–24 characters.

## 5. Regressão de Trim
- readable bubbles near handles;
- initial 35%/65%;
- safe clamps/non-destructive source;
- controls only in active waveform.

## 6. Limpar / Excluir
- `Limpar pista` removes content only and preserves track/function/color/order/mix;
- `Excluir pista` removes only an empty track.

## 7. Undo/Redo + transporte
Validate Undo/Redo after reorder and clip migration, then recheck Loop/playhead behavior and basic playback/recording entry for regression.

## Pass rule
PASS/CLOSED only after explicit physical approval with zero repeatable P0/P1. Until then M5 stays OPEN, PR #1 stays draft and M6 remains blocked.
