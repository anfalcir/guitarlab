# GuitarLab Visual System

Updated: 2026-10-07

## Product intent
GuitarLab should visually read as one coherent music-production/study instrument: clear modules, restrained graphite surfaces, precise controls and small-radius rectangular geometry. Studio may be the densest workspace, but Prepare, Export, Activity, Backup and Settings must use the same visual grammar. The interface must prioritize immediate operational comprehension over decorative softness.

## Geometry contract
Ordinary UI uses a deliberately small shape vocabulary:
- 4dp — micro elements/indicators only;
- 6dp — actionable controls and compact hardware-style chassis;
- 8dp — internal rows/cards, selectors and track-level surfaces;
- 10dp — major panels, dialogs and screen-level containers.

Circular geometry is reserved for semantics that are inherently circular, such as the large recording countdown. General buttons, track-color choices, cards and selectors should not use circular/pill geometry.

## Color hierarchy
- Graphite/neutral surfaces: structure and non-semantic containment.
- Blue / `secondary`: comparison/reference family.
- Teal / `primary`: adjustments/primary studio action family.
- Amber / `tertiary`: timeline/marker/section family.
- Red: recording, destructive action and error only.
- Green: loop or explicit positive state where already established.

Color is a secondary cue. Every group must remain understandable from geometry, borders, spacing and typography even without relying on hue.

## Title vs action contract
A section/group title:
- is not clickable;
- lives in a visually distinct header area or above the action content;
- uses label/title typography rather than button container treatment;
- never shares the exact same shape/border treatment as adjacent buttons.

An action:
- has a visible chassis or explicit button treatment;
- retains clear enabled/disabled state;
- preserves ergonomic touch area (icon controls retain 48dp touch target);
- is contained by the semantic group that owns it.

## Studio action navbar contract
`Comparação`, `Timeline` and `Níveis` occupy three fixed-size icon triggers in the existing transport navbar. Comparison/Timeline open anchored overlays; Níveis is a direct action that opens level analysis. The Níveis icon must remain visually distinct from the top-bar Mixer toggle and Options/settings iconography. No trigger changes location/size when a mode, disabled state, Mixer visibility or minimum/complete state changes.

Neutral closed triggers reduce persistent color noise. Comparison's active mode uses blue plus a fixed R/M/2 indicator and accessible state description. Panels preserve semantic grouping and local overflow for narrow windows or larger fonts. Comparison remains open during repeated audition changes. Narrow viewports scroll the same navbar row; no capability disappears.

## Screen-by-screen application
### Home
- squared major hero/project surfaces;
- project rows use small-radius bordered hardware panels;
- Help/Options and row actions use visible square icon controls.

### Novo Projeto
- template choices use compact rounded rectangles rather than soft cards;
- create/back actions follow the shared control geometry.

### Studio top bar and transport
- transport container follows major-panel geometry;
- all icon controls use visible square chassis;
- recording/loop colors remain semantic, not decorative.

### Timeline / track workspace
- track, clip, drag, trim and add-track surfaces use the common 6/8/10dp vocabulary;
- track color selection uses rounded squares;
- timeline semantic colors remain stable.

### Mixer
- strip/master surfaces retain clear rectangular module boundaries;
- every channel uses a centered header treatment; the track color marker and title read as one centered identity group;
- functional regions are softly segmented into header, state actions, metering and mix controls using low-contrast graphite surfaces and restrained accent outlines;
- segmentation may clarify hierarchy but must not add fake controls, change audio semantics, reduce touch targets or inflate the 252/172 dp dock budgets;
- buttons and meters remain visually distinct from labels/readouts;
- track accent remains a restrained identity cue on unselected cards and becomes stronger on the selected card; color is never the only selected-state cue;
- MASTER uses the same visual grammar with centered title, metering section and volume section, but does not render an empty/fake action bank; transient clipping uses a compact warning face inside a 48 dp action target and the visible warning may not cover the centered title;
- MASTER remains structurally separate and fixed.

### Options
- each option category is a bordered major panel;
- category title/subtitle live in a header band with an accent rail;
- individual settings use internal 8dp rows;
- dropdown trigger buttons remain visually actionable.

### Audio diagnostics
- diagnostic cards and device selectors use major/internal panel geometry;
- action buttons retain obvious button chassis.

### Codec diagnostics
- inherits global compact Material3 shapes and stronger outlines;
- actions remain visually separated from status copy.

### Track settings / level analysis / help dialogs and Export panels
- major dialog/panel geometry capped at 10dp;
- nested sections use 8dp;
- actions use 6dp/default compact control shape;
- destructive actions remain color-semantic.

## Accessibility and robustness
- touch targets are not reduced merely to achieve compact appearance;
- selected/disabled state must not rely on color alone;
- text scaling must not cause cross-group overlap;
- narrow layouts must preserve discoverability rather than clipping;
- borders/outlines must remain visible against both dark and light surfaces;
- headings and labels must stay non-actionable unless explicitly designed as a control.

## Prohibited regressions
- pill-shaped general buttons/cards returning through ad-hoc local styling;
- group titles visually indistinguishable from buttons;
- one semantic group visually bleeding into another;
- hidden controls used as a workaround for insufficient width;
- circular controls used solely as decoration;
- per-screen shape systems that diverge from the 6/8/10dp contract without a documented semantic reason.


## Settings information hierarchy
Settings is a configuration surface, not a wall of primary CTAs.
- On wide/tablet layouts, keep the readable Settings column centered and bounded rather than stretching controls edge-to-edge.
- A section groups related information once; avoid unnecessary card-within-card chrome for read-only rows.
- Secondary actions should be compact trailing actions on wide layouts and may stack/full-width only when narrow width requires it.
- Filled primary buttons are reserved for genuinely primary/committing actions, not routine diagnostics or toggles.
- Maintain at least 48 dp action height and preserve clear labels/semantics.
- Dense control sets such as latency increments must remain fully visible without hidden horizontal carousels when a compact grid can represent the same choices.


## Unified project-shell contract
Prepare, Studio and Export are workspaces of one project and must share project-level chrome.

Shared shell responsibilities:
- project identity/name;
- return to project library;
- adaptive Prepare / Studio / Export navigation;
- project-level status/activity indicator;
- project overflow/actions;
- consistent missing/loading/error presentation.

Studio transport/mixer controls remain specialized inner chrome. They may not replace the shared project navigation language.

Wide/tablet:
- stable compact workspace navigation;
- preserve Studio timeline width and transport ergonomics;
- avoid duplicate full-width navigation rows.

Narrow/phone:
- same destinations in a compact adaptive treatment;
- no capability disappears solely because width is reduced.

## Shared product primitives
Use reusable visual components for:
- project page scaffold;
- section/card;
- status chip;
- operation/progress panel;
- empty state;
- recoverable error/blocked state;
- destructive confirmation;
- source/result card;
- project-media row;
- cloud/sync status.

Per-screen reimplementation of these primitives is a regression risk.

## Prepare
Prepare is a progressive journey, not a dashboard of unrelated technical panels.

Visual hierarchy:
- one active step is primary;
- completed steps collapse to compact summaries;
- the next safe action is visually dominant;
- technical detail is secondary/expandable;
- accepted source controls collapse behind an explicit “Trocar fonte” action;
- automatic post-stem reference generation appears as progress, not a redundant confirmation button.

## Export
Export is the only external-delivery workspace.

Its hierarchy is:
- Projeto portátil;
- Arquivos para estudo;
- Mix final do Studio.

Home/Studio may link to Export but must not reproduce a competing format-selection dialog.

## Activity
Activity uses the same status/progress components as Prepare/Export/Backup.

The user sees semantic operation states, not raw backend enums. Provider/job IDs belong in diagnostics/details.

## Backup and cloud
Backup/restore must use the same page/section/status language as the rest of GuitarLab. It must not read visually as a separate utility app.

## Visual qualification when affected
Changes to shared shell, layout, typography, shapes, colors or responsive behavior require focused semantic geometry and screenshot review for the affected surfaces and representative reference-device conditions. Do not replay the historical full screenshot matrix for unrelated audio/backend/search logic changes. Existing accepted screenshots remain evidence until a visual change invalidates them.

Current Mixer geometry remains normative from the live Mixer contract: normal-font track width 200 dp, Master 144 dp, complete/minimum heights 252/172 dp, 48 dp semantic action targets with compact visible faces, centered track/Master identity, full-width meter regions and an edge-anchored compact clipping warning that may not overlap centered identity text. Candidate-specific RC24/RC26/RC27 evolution is preserved under `docs/history/`, not in this live visual contract.
