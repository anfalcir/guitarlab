# H24 — Home Project Library

Updated: 2026-09-16

Status: **DIGITAL PASS — CI #641**

Corrective test-alignment block: **H24a**.
Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.
Exact promoted producer source: `b11769f340f7056c37dfb17d95b062909dad87bf`.

## Goal
Turn Home from a simple recently-modified list into an efficient project library without introducing folders/tags/favorites or changing persisted project schema.

## UX contract
Home title is `Projetos` with a compact library-management surface.

### Search
- field label `Pesquisar projetos`;
- name-only matching;
- real-time, case-insensitive and accent-insensitive;
- dedicated clear-search action;
- non-destructive.

### Filters
Template: Todos os templates / Guitarra / Vazio.

Content: Todo conteúdo / Com gravações / Com áudio/clipes / Sem clipes.

Sample rate: Todos os sample rates / Auto / 44,1 / 48 / 88,2 / 96 kHz.

Filters combine by logical AND across dimensions. Clearing filters does not reset the chosen sort order.

### Ordering
- Modificados recentemente — default;
- Modificados há mais tempo;
- Nome A–Z;
- Nome Z–A;
- Criados recentemente;
- Criados há mais tempo.

Equal primary keys resolve deterministically by normalized name, exact name and project ID.

### Result states
- unfiltered library: `N no total`;
- narrowed library: `X de Y projetos`;
- real empty repository: `Nenhum projeto ainda`;
- query/filter zero result: `Nenhum projeto encontrado` with clear action.

## Data/architecture contract
`HomeViewModel` keeps repository snapshot and library query as separate state.

`ProjectLibraryIndex` is rebuilt only when repository content refreshes after load/project mutation. It stores normalized project names, so typing performs no project-file disk I/O and does not repeatedly normalize every project.

Pure pipeline:
`ProjectRepository.list → ProjectLibraryIndex → query/filter/sort → HomeUiState.projects`.

Normalization uses Unicode NFD, removes combining marks and lowercases with `Locale.ROOT`.

Content predicates are view-only:
- `WITH_RECORDINGS`: recording takes or clip take lineage;
- `WITH_AUDIO`: at least one clip;
- `EMPTY`: no clips.

## Persistence compatibility
H24 introduces no persistent field, schema migration, managed-media change or `.guitarlab` package change. Search/filter/sort state is presentation state only.

## Accessibility/testability
Search, clear, filter, sort, active-filter state, result count and no-results surface expose meaningful semantics/test hooks. Controls use meaningful labels rather than icon-only meaning.

## Source materialization
Canonical source parts:
- `.source-parts/H24HomeProjectLibrary.patch.gz.part00`;
- `.source-parts/H24aAndroidTestCompileFix.patch.gz.part00`.

Final materializer message:
`Source patch chain materialized through H24a with verified final hashes`.

Relevant final Git blob identities:
- `HomeScreen.kt` — `643673855dd5683eccf04df614e634827cdd2a2d`;
- `HomeViewModel.kt` — `45d6ae5a51eb5aec60721426ea0a4f02c4cbacc0`;
- `StudioUserGuideDialog.kt` — `d8deca759a39638f507d2c23e883ec1413ac55cd`;
- `HomeProjectLibraryInstrumentedTest.kt` — H24 base `1bafaa45a6372b5728d4a0eb3ba3a90d0eeb7c28`; H24a corrected `9ed877ddd44c5d271b69f4519e9cf4db68562493`;
- `ProjectLibraryPolicy.kt` — `96a51d1309b2c888770d49d32c58c1a732913462`;
- `ProjectLibraryPolicyTest.kt` — `9b09a64a8d7a173cffc3436f1b2c5e4f041a8f3a`.

## Pre-CI validation
Before hosted CI, H24/H24a passed pure Kotlin compilation, deterministic functional checks, 10,000 randomized library iterations, search/filter/sample-rate/tie-break matrices, byte-exact patch round-trip, gzip/base64 integrity, idempotent materialization, corruption fail-closed, final hashes, `bash -n` and `git diff --check`.

## Hosted evidence
### CI #640 — historical corrective evidence
Run `35103316449` proved the software gate but failed in Android-test compilation before instrumentation because H24 used one invalid explicit `assertDoesNotExist` import. Signing was correctly skipped.

H24a removed only that invalid import; production Home behavior did not change.

### CI #641 — promoted DIGITAL PASS
Run `35105065689`, exact producer `b11769f340f7056c37dfb17d95b062909dad87bf`:
- 269/269 JVM/unit PASS, including 9 `ProjectLibraryPolicyTest` cases;
- Android Lint/build/provenance PASS;
- H24a materialization/hash verification PASS;
- API36 standard **25/25 PASS**;
- isolated geometry **1/1 PASS**;
- signed homologation PASS.

The standard suite count increased from historical 23 to 25 because the H24 Home-library instrumentation is now included. Keep canonical reporting as **25/25 standard + 1/1 geometry**.

## Residual physical acceptance
Digital behavior is promoted. Remaining H24 evidence is a short SM-X230 UX/touch smoke: search, one combined filter, sort, `X de Y`, clear behavior, return-from-project state and responsive/tappable controls.

## Durable decision
Project-library discovery remains a pure in-memory view over existing metadata for RC3. Tags, favorites and folders remain deferred because they would introduce persisted product semantics that H24 does not need.
