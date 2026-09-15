# GuitarLab Studio Visual System

Updated: 2026-09-15
Status: H21 source contract — PRE-GATE until the next exact-source full CI passes.

## Product intent
GuitarLab should visually read like compact studio hardware: clear modules, restrained graphite surfaces, precise controls and small-radius rectangular geometry. The interface must prioritize immediate operational comprehension over decorative softness.

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

## Practice bar contract
`Comparação`, `Ajustes` and `Timeline` are three independent modules, not one undifferentiated row.

Wide/tablet:
- Comparação and Ajustes measure to content;
- Timeline owns remaining width and local horizontal overflow;
- each group has its own border/chassis;
- each title has a fixed non-action header surface;
- group spacing replaces ambiguous shared boundaries.

Narrow:
- groups stack vertically;
- long action rows scroll internally;
- group title remains fixed/visible.

Functional accents:
- Comparação = blue;
- Ajustes = teal;
- Timeline = amber.

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
- buttons and meters remain visually distinct from labels/readouts;
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

### Track settings / level analysis / help / export dialogs
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
