# Unified Product Cohesion Audit — GuitarLab

Updated: 2026-09-21  
Status: **MANDATORY PRODUCT-CONSOLIDATION CONTRACT**  
Applies to: unified GuitarLab line after U5 and during U6-U12  
Primary roadmap: `docs/UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`

---

## 1. Purpose

This audit exists to prevent the final Android product from feeling like two applications placed side by side.

The target is one product with one identity, one navigation language, one project model, one visual system, one operation model and one coherent user journey:

**Create/Open → Prepare when needed → Studio → Export → Backup/Restore**

GBW-derived capabilities are implementation provenance only. They must not survive as a second product shell, a parallel set of concepts, duplicate actions, raw backend terminology or a separate UX personality.

The audit was performed against the exact materialized U6 candidate source from Android CI #694, source SHA:

`364a8ddb1904003af150c9162fed6aa6a4106d8e`

CI #694 passed the technical Android gate. That evidence is preserved, but U6 must not be declared finally closed until the U6 cohesion residuals in this document are resolved.

---

## 2. Product-level conclusion

The architecture already has the correct core direction:

- one immutable project identity;
- managed source/stems/references;
- zero-copy Prepare → Studio handoff;
- unified project library;
- one Android package/signing identity;
- shared media graph;
- explicit Prepare / Studio / Export concepts;
- transactional/fail-closed media publication;
- separation and Studio state tied to the same project.

However, the current materialized UI still contains several seams that would make the final APK feel assembled from historical subsystems unless they are deliberately removed.

The highest-risk seams are:

1. Prepare/Export use a shared scaffold while Studio still presents a separate shell/navigation language.
2. Home and Studio still expose an older “Salvar e exportar” dialog in parallel with the new Export workspace.
3. Opening a project from Home always enters Studio even when the meaningful current activity is Prepare.
4. Prepare is a long flat technical page instead of one progressive preparation journey.
5. deterministic reference creation still requires a manual “Criar base e referência” action even though there is no user choice to make.
6. no real global Activity/Jobs Center exists yet, despite the roadmap describing one.
7. source operation and separation refresh are polled from the Compose screen every 600 ms.
8. `HomeViewModel` currently owns library, source acquisition, separation, prepared-reference and export orchestration responsibilities.
9. user-facing surfaces still expose implementation vocabulary such as raw enum names, `AssetRole`, “assets”, SHA fragments and mixed English/Portuguese labels.
10. source replacement/re-preparation lifecycle is not yet represented as an explicit user-safe flow.
11. “Manter atual” for a newer prepared reference needs a durable acknowledgement contract, not only temporary UI suppression.
12. a restored route pointing to a missing/deleted project needs a first-class recovery state instead of an indefinite loading presentation.
13. backup/authentication remain visually and operationally separate from the unified project shell.
14. Home help currently opens the Studio-oriented guide, which reinforces the perception that Studio is the whole app.
15. visual tokens exist, but shared components, spacing/status conventions and responsive project-shell chrome are not yet strong enough to guarantee a single design language.
16. delete/duplicate/rename while background operations are active needs an explicit cross-feature lifecycle contract.
17. loading/empty/error/blocked/disabled states do not yet use one shared semantic model.
18. there is no final visual-regression/screenshot matrix that proves Home, Prepare, Studio, Export, Settings, Backup and Activity look like one application.

These are not requests for unrelated feature expansion. They are consolidation work required to make the already-approved capabilities behave and present as one product.

---

## 3. Locked final product model

### 3.1 One product shell

The displayed product is **GuitarLab**.

“Studio” is a workspace inside GuitarLab, not a second application identity.  
“Prepare” is a workspace inside GuitarLab, not “GBW inside GuitarLab”.  
“Export” is a workspace inside GuitarLab, not a collection of old and new export dialogs.

All project workspaces share a common project-level shell.

### 3.2 One project-level navigation system

Every project uses the same adaptive navigation model:

- Prepare
- Studio
- Export

Tablet/wide:
- persistent compact project navigation, rail or equivalent stable chrome.

Phone/narrow:
- compact adaptive project navigation using the same destinations.

The shared project shell owns:
- project name/identity presentation;
- navigation among Prepare / Studio / Export;
- return to project library;
- project-level overflow actions;
- compact background-activity indication;
- backup/sync status when U8 is implemented;
- common loading/missing-project/error shell.

Studio may retain its specialized transport/mixer chrome **inside** this project shell.

### 3.3 One “open project” rule

Home must not hard-code Studio as the universal entry point.

Opening a project resolves to:
1. last meaningful workspace when valid; otherwise
2. Prepare when a preparation workflow is active/incomplete and no Studio work requires priority; otherwise
3. Studio for a normal playable/editable project.

The project must never jump workspaces automatically while the user is actively working.

### 3.4 One operation language

Long-running work must use one operation abstraction and one user-facing state vocabulary.

Operations include:
- source acquisition;
- remote separation;
- reference generation;
- backup;
- restore;
- study export;
- master export.

Common semantic states:
- waiting;
- running;
- paused/retrying;
- completed;
- cancelled;
- attention required.

Raw backend/enum state names are diagnostics only.

### 3.5 One visual language

The Graphite Studio theme remains the base identity.

The product must standardize:
- page margins;
- content max widths;
- card elevation/borders;
- status chips;
- primary/secondary/destructive action hierarchy;
- progress presentation;
- empty/error panels;
- typography hierarchy;
- icon semantics;
- responsive breakpoints;
- touch targets;
- focus/TalkBack descriptions.

Color may reinforce state but never be the only state signal.

### 3.6 One terminology dictionary

Primary user-facing vocabulary is Portuguese and must be consistent.

Preferred terms:
- Preparar
- Studio
- Exportar
- Base sem guitarra
- Guitarra de referência
- Arquivos do projeto / Mídia do projeto
- Faixas separadas (with “stems” only as secondary technical detail when useful)
- Arquivos para estudo
- Mix final do Studio
- Processamento em nuvem
- Backup no Google Drive
- Atividade

Avoid in primary UX:
- raw `AssetRole` names;
- raw remote job enum names;
- “Study Exports”;
- “Studio Master” as an untranslated section heading;
- “assets gerenciados” in explanatory copy;
- SHA-256 and internal IDs outside Details/Diagnostics;
- Cloud Run/Firebase implementation terminology unless diagnosing.

---

## 4. Serial cohesion program

The following passes are mandatory and serial. A later pass may prepare tests early, but it may not be declared closed before the prior pass.

### C1 — U6 export convergence

**Status: CLOSED / DIGITAL PASS — Android CI #711 / run `35658467705`, exact source `6ff1988a8309b7174eeb328e0044d6b1d3167ff6` (2026-09-21).** The exact source snapshot uploaded by the gate satisfies the final C1 semantic contract: no independent `SaveAndExportDialog` or parallel format-selection copy remains in `app/src/main/java`; no obsolete Studio-local export state/entry point remains; Home and Studio route export actions through `ExportEntryPointPolicy` to the canonical Export workspace; primary WAV controls no longer expose “sem conversão” while the zero-transcode explanation remains secondary copy; software gate and the complete API 36 instrumented regression passed. The run was unsigned because signed homologation was not requested. C2 has not started.

**Objective:** finish U6 as one export experience.

Required work:
- make the Export workspace the canonical destination for all project export actions;
- remove the parallel Home/Studio “Salvar e exportar” master-format dialog as an independent UX;
- Home/Studio export/share actions may deep-link to Export, but must not maintain separate format-selection behavior;
- use one export capability model, one operation state and one error/success model;
- rename user-facing sections to Portuguese;
- hide implementation detail such as “sem conversão” from the primary button label; keep quality/provenance explanation in secondary text;
- when an output is unavailable, explain why and provide the next relevant action instead of presenting unexplained disabled controls;
- preserve the U6 zero-transcode internal media contract.

Exit:
- exactly one external-delivery workflow exists;
- no duplicate master-format chooser exists elsewhere;
- all U6 tests remain green.

### C2 — Unified project shell and navigation

**Status: CLOSED / DIGITAL PASS — Android CI #713 / run `35660979142`, exact source `66cedad38875d6f7284180ef46d25d4e9049d287` (2026-09-21).** Prepare, Studio and Export use one adaptive `ProjectShellScaffold`; Studio no longer owns a second project-navigation top bar; its transport remains immediately below the shared shell. Workspace-local saveable state survives project-workspace switches, Home resolves each project to its remembered/relevant workspace, project-scoped Settings returns to the exact originating workspace across Activity recreation, and missing-project routes fail safely back to Home. Unit tests, Android Lint, APK build and the complete API 36 instrumented regression passed. C3 is next; U7 production cutover remains blocked on C2-C4.

**Objective:** Prepare, Studio and Export look and behave like workspaces of the same project.

Required work:
- create one reusable adaptive `ProjectShellScaffold` or equivalent;
- move common project identity/back/library/navigation/status chrome into it;
- embed Studio’s transport top bar beneath/within the shared shell without reducing timeline ergonomics;
- preserve state when switching workspaces;
- define process-recreation and missing-project fallback;
- Home opens last/relevant workspace, not always Studio;
- project-level Settings returns to its exact originating project/workspace.

Exit:
- Prepare/Studio/Export navigation is visually and behaviorally identical at project level;
- no screen has a second bespoke project-navigation grammar.

### C3 — New project and project lifecycle coherence

**Status: CLOSED / DIGITAL PASS — Android CI #720 / run `35669035762`, exact source `f730e08cc242ed590c333727cde4c96883f7df07` (2026-09-21).** New Project now has three explicit intents: Search song, Import audio and Start in Studio. Search/Import create the guitar-study model directly without exposing template choice; Studio-only creation retains Guitar/Blank. New-project name/intent/template state is saveable across Activity recreation. Source replacement is generation-safe: the validated replacement becomes active, prior stems/references stop being active while old media remains protected, and Studio tracks/clips/takes remain intact. Duplicate keeps durable project content but normalizes transient Prepare state and never inherits remote/background ownership; rename keeps the immutable project id; delete closes source acquisition, separation and WorkManager ownership before repository removal and communicates active-operation cancellation in user-facing language. Unit/JVM, performance evidence, Lint, APK build and all 5/5 API 36 regression groups passed. C4 is next; no C4 implementation is included in this closure.

**Objective:** remove creation/lifecycle ambiguity.

Required work:
- simplify New Project into clear intents:
  - Search song;
  - Import audio;
  - Start in Studio;
- Search/Import create the guitar-study project model directly; do not expose a hidden template choice;
- Studio-only creation may offer Guitar template / Blank where still useful;
- accepted source metadata may propose artist/title metadata, but must not silently overwrite a user-customized project name;
- define source replacement explicitly:
  - user chooses “Trocar fonte”;
  - incompatible in-flight Prepare work is cancelled/reconciled;
  - old authoritative/source-derived media remains protected until safe cleanup;
  - existing Studio recordings/edits are never silently reset;
- define rename/duplicate/delete while operations are active;
- deleting a project with an active cloud/background operation must explicitly cancel/close ownership and clean temporary state;
- duplicate copies durable creative state only, never transient operation identity.

Exit:
- every create/replace/delete/duplicate path has deterministic ownership and a clear user-facing consequence.

### C4 — Progressive Prepare journey

**Objective:** Prepare feels like one guided process instead of a collection of GBW-derived panels.

Target progression:
1. Source
2. Separation
3. Preparing study references
4. Ready for Studio

Required work:
- render one active step prominently and completed steps as compact summaries;
- source search/import controls collapse behind “Trocar fonte” after a source is accepted;
- map ranking score into an accessible semantic badge while preserving the numeric value if useful;
- map all source/separation states into user-safe messages;
- move technical SHA/asset IDs/model/job details into expandable Details/Diagnostics;
- after six validated stems are published, automatically generate the deterministic default backing/reference because no additional user decision is required;
- manual action remains only for retry/rebuild after an error or explicit reprocessing;
- replace Compose-level 600 ms polling with lifecycle-aware observable state and background reconciliation; remote polling, when required, belongs in repository/job infrastructure with bounded cadence/backoff;
- completion notification/reopen returns to the same project Prepare state.

Exit:
- a normal Search/Import → separation → references journey needs no redundant confirmation clicks;
- the primary screen never shows raw internal state names.

**Status: CLOSED / DIGITAL PASS — Android CI #723 / run `35673758282`, exact source `0d7b05d0192cc415d328e415c517b3cad40dc209` (2026-09-21/22 UTC).** Prepare now presents one progressive Source → Separation → References → Studio journey. Accepted sources collapse acquisition behind “Trocar fonte”; ranking is exposed with textual quality plus numeric score; primary messages are user-safe while durable job/asset diagnostics stay in expandable details. Six validated stems start deterministic reference preparation automatically, and manual action is reserved for retry after failure. Compose no longer owns a 600 ms backend polling loop: lifecycle-aware observation reconstructs durable source/separation/reference state while work continues away from Prepare. The gate passed deterministic C4c materialization, JVM/unit, performance evidence, Android Lint, debug/release assembly and all five API 36 regression groups, including the corrected viewport-independent unified-guide assertion. C5 was implemented and closed in the subsequent cohesion gate.

### C5 — Studio handoff and project media coherence

**Status: CLOSED / DIGITAL PASS — Android CI run `35717888405`, exact materialized source `03735dabd3f4cbfc5a7180633884c85ce5540adb` (2026-09-22).** The gate passed C5 source materialization with exact payload/blob verification, JVM/unit tests, reproducible performance evidence, Android Lint, debug/release assembly and all five API 36 regression groups, including the tablet geometry check. C5 makes prepared-reference acknowledgement durable by revision, preserves creative Studio state during non-destructive rebinding, exposes project-owned managed media through the Studio shell, keeps stems out of the timeline by default, and offers a direct Prepare → Studio handoff.

**Objective:** Studio consumes preparation naturally without appearing bolted onto it.

Required work:
- make “new prepared reference available” acknowledgement durable against a specific prepared revision;
- “Manter atual” means “keep this currently bound version” and survives reopen;
- user can later inspect and deliberately switch from the project-media surface;
- expose one compact project-media/Files surface reachable from the project shell, rather than making Prepare the only place where stems/references can be inspected;
- do not auto-add all stems as timeline tracks;
- preserve zero-copy reference binding;
- preserve recordings, takes, clips, sections, loop and mixer when references change;
- make the successful end of Prepare offer a clear primary “Abrir Studio” action without forcing navigation.

Exit:
- reference versions are understandable and non-destructive;
- project media belongs to the project, not visually to “the GBW side”.

### C6 — Cloud, Backup and Activity integration

**Status: CLOSED / DIGITAL PASS — Android CI #744 / run `35729709715`,
exact source `89a7cca5acd2d5a1c9aabbb5f72e028347dd00db`.**

**Objective:** background/cloud capabilities feel like services of one app.

Required work during U7/U8:
- introduce a shared operation contract/store instead of separate ad-hoc UI refresh mechanisms;
- provide one Activity surface aggregating source acquisition, separation, reference generation, backup, restore and exports;
- Home may show compact active-operation progress but not duplicate the full operation UI;
- notifications deep-link to the owning project/workspace/activity item;
- Settings presents one “Conta e nuvem” hierarchy with two clearly separate capabilities:
  - Processamento em nuvem;
  - Backup no Google Drive;
- authentication is just-in-time; local Studio remains usable without cloud login;
- Firebase/Cloud Run/Drive names belong to diagnostics unless the provider name is necessary for consent;
- U8 replaces historical backup presentation with the unified backup model rather than layering another backup UI beside it;
- project cards gain one compact sync state derived from the unified backup model.

Exit:
- there is one activity/history surface and one cloud/settings language;
- no feature owns a visually isolated “mini app” for its jobs.

U8h established the shared store, Activity surface, compact Home progress,
unified cloud/settings hierarchy and project-card sync presentation. U8i closes
the two remaining C6 contracts: project sync labels now derive from the current
local revision versus the durable server-confirmed revision boundary, and backup
notifications use a typed Activity deep-link carrying the owning operation id.
The API 36 regression includes the deep-link lifecycle path and the confirmed
revision store rejects unconfirmed failures. C6 is therefore closed; subsequent
U8 transport/cutover/fault work must preserve these product-cohesion contracts.

### C7 — Visual, copy, responsive and accessibility consolidation

**Status: CLOSED / DIGITAL PASS — consumed by Android CI #781 / run `35791192802` at exact technical source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`.** The final responsive/accessibility and visual artifact campaign is recorded in `docs/U10_FINAL_DIGITAL_COHESION_GATE.md`.

**Objective:** make the entire APK visually coherent before global hardening.

Required work:
- centralize common layout tokens/components:
  - page scaffold;
  - section/card;
  - status chip;
  - operation/progress panel;
  - empty/error/blocked panel;
  - destructive confirmation;
  - project header/navigation;
- remove mixed Portuguese/English user-facing headings;
- Home help becomes GuitarLab-wide help; Studio-specific help remains contextual inside Studio;
- harmonize copy tone: short, direct, non-technical first; technical details secondary;
- define primary CTA per screen/state;
- audit all disabled controls for an understandable reason;
- preserve immersive/system-bar behavior without clipping focus/touch content;
- validate dark/light, tablet/phone, portrait/landscape where supported, and font scale;
- TalkBack order and state announcements must be meaningful;
- no status may depend on color alone.

Exit:
- every major screen passes the cohesion checklist and responsive/accessibility matrix.

### C8 — Global digital cohesion gate

**Status: CLOSED / DIGITAL PASS — Android CI #781 / run `35791192802`, exact technical source `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`.** The fail-closed gate proved 23/23 instrumented classes, 76 observed API 36 tests and 24/24 retained screenshots; deterministic visual review found no system-overlay contamination and confirmed the required Home → Prepare → Studio → Export → Backup/Activity/Settings cohesion path. Full evidence is in `docs/U10_FINAL_DIGITAL_COHESION_GATE.md`.

**Objective:** prove the final application behaves as one product before U11 candidate freeze.

This gate is executed as part of U10 and consumed by U11.

Required evidence:
- complete critical user-journey tests;
- navigation/recreation matrix;
- background-operation ownership/cancellation matrix;
- source replacement/re-preparation matrix;
- destructive-action matrix;
- unified terminology scan;
- no raw enum/internal-role text in primary UX;
- screenshot artifact matrix for major screens/states;
- phone/tablet + dark/light + representative font scale;
- accessibility semantics/touch target/contrast checks;
- visual inspection checklist;
- no duplicate export workflow;
- no duplicate project-shell navigation;
- no orphan legacy GBW branding or copy;
- no user-facing dependency on historical app boundaries.

Exit:
- a reviewer can traverse Home → Prepare → Studio → Export → Backup/Activity/Settings without encountering a second interaction language.

---

## 5. Critical happy-path contracts

### 5.1 Search-based study project

Home  
→ New project  
→ Search song  
→ source candidates  
→ select source  
→ source validated  
→ separation starts  
→ user may leave screen/app  
→ six stems validated  
→ default references generated automatically  
→ project becomes Ready for Studio  
→ notification/status updates  
→ Studio opens managed backing/reference directly  
→ user records/edits  
→ Export publishes requested external files  
→ backup operates on the same project identity.

There is no manual file handoff at any step.

### 5.2 Local-audio study project

Home  
→ New project  
→ Import audio  
→ picker  
→ validation/publication  
→ same preparation path as above.

After source publication, online/local source origin must not create different downstream UX.

### 5.3 Studio-only project

Home  
→ New project  
→ Start in Studio  
→ Guitar template or Blank  
→ Studio.

Prepare remains optional. Cloud authentication must not be required.

### 5.4 Re-preparation with existing recordings

Existing project with Studio edits  
→ Prepare  
→ explicit reprocess/replace-source intent  
→ new source/stems/references produced  
→ old Studio binding remains valid  
→ Studio shows new-reference availability  
→ user chooses Update or Keep current  
→ choice persists  
→ recordings/takes/edits remain intact.

### 5.5 Export

Any export entry point  
→ canonical Export workspace  
→ choose semantic output (project package / base / guitar / final mix)  
→ choose supported format  
→ operation progress  
→ success/failure/cancel feedback.

No other screen owns a separate encoder-selection flow.

---

## 6. Structural engineering changes required

The final architecture does not need module churn for its own sake, but ownership must be corrected where current source shows consolidation risk.

### 6.1 HomeViewModel boundary

`HomeViewModel` should converge toward project-library responsibilities:
- list/filter/sort projects;
- create/rename/duplicate/delete;
- compact project summaries.

Project execution responsibilities should move behind project-scoped coordinators/use cases:
- source acquisition;
- separation;
- preparation;
- export;
- backup/activity observation.

A temporary facade is acceptable during migration, but Home must not remain the permanent owner of every subsystem.

### 6.2 Operation domain

Implement the roadmap’s missing shared jobs/operation abstraction, either as `core:jobs` or an equivalent repository-owned core contract.

It must expose:
- operation ID;
- project ID;
- operation type;
- semantic state;
- progress;
- timestamps;
- retry/cancel capability;
- terminal result;
- user-safe message;
- sanitized diagnostic detail.

Platform WorkManager/network/cloud adapters update this contract. Compose observes it; Compose does not drive fast polling loops.

### 6.3 Presentation models

Primary UI must consume presentation models rather than render domain enums directly.

Required mappers include:
- preparation stage;
- operation state;
- asset/media role;
- source ranking state;
- backup/sync state;
- codec availability reason.

### 6.4 Navigation ownership

The current string route codec may remain if it stays deterministic and testable. Replacing it with another navigation library is **not** a goal by itself.

What is mandatory is:
- one project-shell navigation contract;
- deterministic recreation;
- missing-project fallback;
- originating-workspace return;
- last/relevant workspace resolution.

---

## 7. Visual verification matrix

At minimum generate and retain CI screenshots/artifacts for:

Screens/states:
- Home empty;
- Home with prepared/recorded/syncing projects;
- New Project;
- Prepare source search/results;
- Prepare separating;
- Prepare ready;
- Studio normal;
- Studio new-reference notice;
- Export ready;
- Export running/error;
- Activity with mixed operations;
- Settings;
- Backup/restore;
- destructive confirmation/error state.

Viewport matrix:
- target tablet 1920×1200 class;
- representative phone portrait;
- representative phone landscape or compact-width stress;
- dark theme;
- light theme;
- normal font scale;
- enlarged representative font scale.

Pixel-perfect screenshot comparison is optional if it proves too brittle. Screenshot generation plus semantic geometry assertions and a deterministic review checklist is mandatory.

---

## 8. Copy/branding audit rules

Release source must not contain user-facing orphan labels implying two products.

Allowed historical/internal references:
- GBW in provenance/docs/tests;
- Demucs as optional secondary technical detail;
- Firebase/Cloud Run in diagnostics;
- legacy class/file names that are not user-facing and are harmless.

Not allowed in normal UI:
- “GBW” as a user workspace/product;
- “enviar para GuitarLab”;
- “importar no Studio” for prepared project assets;
- duplicate “Salvar e exportar” path competing with Export;
- raw `REMOTE_JOB_...`, `STEM_GUITAR`, etc.;
- English section names when the surrounding UI is Portuguese.

---

## 9. Milestone ownership

- **U6:** must absorb C1 before final DIGITAL PASS.
- **U7:** must consume C2-C4 contracts and must not reintroduce backend-specific UI.
- **U8:** owns the backup portion of C6 and must replace, not stack onto, historical backup UX.
- **U9:** remains retired.
- **U10:** closes C2-C8 globally with stress/regression/security/performance evidence.
- **U11:** may freeze a signed candidate only after the global cohesion gate is PASS.
- **U12:** manual campaign validates physical/audio/subjective residuals, not basic information architecture that should already be digitally closed.

---

## 10. Definition of “one consolidated app”

The final APK is considered product-cohesive only when all are true:

1. one product name/identity is visible;
2. one project library exists;
3. one project-level navigation language exists;
4. one source/separation/reference journey exists;
5. one operation/activity model exists;
6. one export workflow exists;
7. one backup/sync model exists;
8. one settings hierarchy exists;
9. one visual component language exists;
10. one terminology dictionary is respected;
11. background work survives navigation/process recreation without screen-owned hacks;
12. Prepare output becomes Studio input automatically without file handoff or redundant conversion;
13. historical GBW/GuitarLab boundaries are invisible to the user;
14. all major empty/error/loading/destructive flows have deliberate UX;
15. digital screenshot/geometry/accessibility evidence supports the final UI;
16. the remaining physical campaign is about hardware and subjective behavior, not discovering basic UX integration problems.

This contract is release-blocking.
