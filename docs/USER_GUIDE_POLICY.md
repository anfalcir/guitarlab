# In-app User Guide Synchronization Policy

Updated: 2026-09-21

## Contract
GuitarLab exposes **one product-wide help source of truth** covering the complete application: Home, New Project, Prepare, Studio, Export, Activity, Backup/Restore and Settings.

The historical implementation may still contain a class named `StudioUserGuideDialog` during the cohesion transition. That class name does not define product scope. C7 in `UNIFIED_PRODUCT_COHESION_AUDIT.md` must converge the user-facing help experience to GuitarLab-wide help.

Contextual Studio help is allowed for dense editing/recording controls, but it must consume the same canonical guide content/contracts rather than becoming a second independent help system.

Whenever a development block changes a user-visible workflow, the same block must update the relevant help content when the behavior is not self-evident. Source implementation, accessibility semantics, tests and guide wording must describe the same behavior before the block is considered complete.

## Current mandatory unified-guide coverage
The guide must remain synchronized with:
- Home project library, search/filter/sort and status indicators;
- New Project intents: Search song / Import audio / Start in Studio;
- Prepare source acquisition, separation, automatic reference preparation, retry/cancel and source replacement;
- Prepare → Studio zero-copy handoff;
- new-reference availability and non-destructive Update / Keep-current semantics;
- Studio navigation/transport/loop behavior;
- track controls and Mixer basics;
- Trim/CUT interaction and clip deletion scope;
- recording countdown, route selection, waveform/meters and stop behavior;
- practice/sections workflow;
- Export distinctions: project package, files for study and final Studio mix;
- Activity/background operation behavior;
- Drive backup/restore and cloud-auth boundaries when U8 is active;
- project rename/duplicate/delete and their effect on background work;
- Settings sections and diagnostics at a user-goal level.

## Product-cohesion rule
Home help must not open a Studio-only explanation as though Studio were the whole application.

The help hierarchy is:
1. GuitarLab overview and project lifecycle;
2. Prepare;
3. Studio;
4. Export;
5. Cloud/backup/activity;
6. Settings/diagnostics.

A user should understand the complete workflow without learning that capabilities originated in different historical applications.

## Copy principles
- use concise Brazilian Portuguese;
- describe user goals, current state, consequence and next action;
- avoid GBW branding in normal help;
- avoid raw Android/backend/domain identifiers;
- do not expose Git/CI/materializer/milestone terminology;
- keep destructive actions and their scope explicit;
- prefer one canonical help content model over separate Home/Studio/Prepare/Export documents;
- technical terms such as Demucs, sample rate or codec may appear only when they materially help the user.

## Verification
Any user-visible feature block that changes the guide must retain source-level review plus Android semantic/UI coverage where practical.

The U10/U11 cohesion gate additionally verifies:
- Home help is product-wide;
- contextual Studio help is not contradictory;
- no obsolete “export from one app/import into another” language remains;
- no unsupported legacy migration is advertised;
- all major workflows in the current unified product have discoverable help.

Final target-device review checks readability/touch ergonomics only when those qualities cannot be established reliably in automation.
