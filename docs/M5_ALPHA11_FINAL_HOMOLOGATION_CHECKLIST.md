# M5 alpha11 final homologation checklist

Status: **ACTIVE — physical approval pending**
Version target: `0.2.0-alpha11` / versionCode 12
Device baseline: Samsung SM-X230 / Android 16(API 36); Pocket Amp where audio hardware is relevant.

## Preconditions
- install alpha11 over the prior signed build without uninstalling;
- confirm project opens and existing managed media remains intact;
- M5 remains OPEN during this checklist.

## 1. Reorder de pistas
- long press detaches naturally;
- ghost follows finger continuously without jump/lock;
- insertion indicator updates continuously;
- move first↔last and intermediate positions;
- cancel causes zero mutation;
- same-origin drop causes zero history entry;
- IDs, colors, role, mix and clip ownership remain intact.

## 2. Autoscroll
- hold near top: continuous upward scroll if content exists;
- hold near bottom: continuous downward scroll if content exists;
- speed increases toward the edge but stays controlled;
- leaving edge stops immediately;
- ghost/indicator remain visible and target stays correct after scrolling.

## 3. Migração de waveform/clipe
- ghost follows finger above all lanes;
- target lane is highlighted/named;
- moving to another track preserves source, timing, trim, gain, mute and metadata;
- same-track drop/cancel is no-op;
- Undo restores prior track; Redo reapplies.

## 4. Configurar pista
Landscape: name/function/colors at left and `Áudio fonte` metadata at right with balanced space. Portrait: sections stack without clipping; internal scroll only when needed. Verify empty state. `Excluir pista` disabled for non-empty track with guidance to use `Limpar pista`; no ordering arrows; Save/Cancel pt-BR; name 1–24 chars.

## 5. Regressão de Trim
- readable bubbles near handles;
- initial 35%/65%;
- safe clamps/non-destructive source;
- controls only in active waveform.

## 6. Limpar / Excluir
- `Limpar pista` removes content only and preserves track/function/color/order/mix;
- `Excluir pista` removes only an empty track.

## 7. Undo/Redo
Validate after reorder and clip migration, and recheck Loop/playhead behavior.

## Pass rule
PASS/CLOSED only after explicit physical approval with zero repeatable P0/P1. Until then M5 stays OPEN and PR #1 stays draft.
