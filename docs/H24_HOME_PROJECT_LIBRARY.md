# H24 — Home Project Library

Updated: 2026-09-16

Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**

Corrective test-alignment block: **H24a**.

Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.

## Goal
Turn Home from a simple recently-modified list into an efficient project library without introducing folders/tags/favorites or changing persisted project schema.

## UX contract
Home title is `Projetos` with a compact library-management surface.

### Search
- Search field label: `Pesquisar projetos`.
- Name-only matching.
- Real-time, case-insensitive and accent-insensitive.
- Dedicated clear-search action.
- Search is non-destructive.

### Filters
Template:
- Todos os templates
- Guitarra
- Vazio

Content:
- Todo conteúdo
- Com gravações
- Com áudio/clipes
- Sem clipes

Sample rate:
- Todos os sample rates
- Auto
- 44,1 kHz
- 48 kHz
- 88,2 kHz
- 96 kHz

Filters combine by logical AND across dimensions. Active-filter state is visible and can be cleared without resetting the selected sort order.

### Ordering
- Modificados recentemente — default
- Modificados há mais tempo
- Nome A–Z
- Nome Z–A
- Criados recentemente
- Criados há mais tempo

Equal primary keys resolve deterministically by normalized name, exact name and project ID.

### Result states
- Unfiltered library: `N no total`.
- Narrowed library: `X de Y projetos`.
- Real empty repository: `Nenhum projeto ainda`.
- Search/filter zero result: `Nenhum projeto encontrado` with action to clear search+filters.

## Data/architecture contract
`HomeViewModel` keeps repository snapshot and library query as separate state.

`ProjectLibraryIndex` is rebuilt only when repository content is refreshed after load/project mutation. It stores normalized project names so typing does not repeatedly normalize every project and does not cause disk I/O.

Pure pipeline:
`ProjectRepository.list → ProjectLibraryIndex → query/filter/sort → HomeUiState.projects`.

Normalization:
- Unicode NFD decomposition;
- removal of combining marks;
- lowercase using `Locale.ROOT`.

## Content semantics
- `WITH_RECORDINGS`: project has recording takes or clip take lineage.
- `WITH_AUDIO`: project has at least one clip.
- `EMPTY`: project has no clips.

These are view predicates only; they do not alter project state.

## Persistence compatibility
H24 introduces no persistent field and no schema migration. Existing projects, portable packages and round-trips remain structurally unchanged.

Search/filter/sort state is presentation state. It may survive a ViewModel refresh but is not written into `.guitarlab`.

## Accessibility/testability
The search field, clear action, filter button, sort button, active filters, visible count and no-results state expose stable semantics/test tags where needed. Filter and sort controls include meaningful content descriptions rather than relying only on icons.

## Source materialization
Canonical source parts:
- `.source-parts/H24HomeProjectLibrary.patch.gz.part00`;
- `.source-parts/H24aAndroidTestCompileFix.patch.gz.part00`.

Final materializer message after the corrective block:
`Source patch chain materialized through H24a with verified final hashes`.

H24 final materialized blob identities:
- `HomeScreen.kt` — `643673855dd5683eccf04df614e634827cdd2a2d`
- `HomeViewModel.kt` — `45d6ae5a51eb5aec60721426ea0a4f02c4cbacc0`
- `StudioUserGuideDialog.kt` — `d8deca759a39638f507d2c23e883ec1413ac55cd`
- `HomeProjectLibraryInstrumentedTest.kt` — H24 base `1bafaa45a6372b5728d4a0eb3ba3a90d0eeb7c28`; H24a corrected `9ed877ddd44c5d271b69f4519e9cf4db68562493`
- `ProjectLibraryPolicy.kt` — `96a51d1309b2c888770d49d32c58c1a732913462`
- `ProjectLibraryPolicyTest.kt` — `9b09a64a8d7a173cffc3436f1b2c5e4f041a8f3a`

## Validation completed before CI
- pure Kotlin compilation: PASS;
- deterministic functional harness: PASS;
- 10,000 randomized library iterations across all sort modes: PASS;
- sample-rate/filter/search/tie-break checks: PASS;
- forward/reverse byte-exact patch round-trip: PASS;
- gzip/base64 integrity: PASS;
- first materialization: PASS;
- second materialization idempotent: PASS;
- corruption probe: materializer fails closed;
- final blob hashes: PASS;
- materializer shell syntax: PASS;
- `git diff --check`: PASS;
- Home parser/syntax scan: PASS within the limitation of no Android/Compose classpath.

## CI #640 corrective evidence
The first H24 workflow attempt, CI #640 / run `35103316449`, proved the software gate green but failed in `:app:compileDebugAndroidTestKotlin` because `HomeProjectLibraryInstrumentedTest.kt` imported `androidx.compose.ui.test.assertDoesNotExist`, which is not an importable top-level symbol in the pinned Compose test API. Instrumented tests therefore never executed, and signing was correctly skipped.

H24a removes only that invalid import. No Home production behavior, project policy, schema, persistence or `.guitarlab` contract changes.

H24a source validation:
- exact #640 source snapshot apply: PASS;
- reverse to original H24 test blob: PASS;
- deterministic reapply: PASS;
- second run idempotency: PASS;
- encoded-patch corruption fails closed: PASS;
- corrected instrumented-test blob: `9ed877ddd44c5d271b69f4519e9cf4db68562493`.

## Pending evidence
No local Android Gradle/Lint/API36/signing label is claimed. H24 reaches DIGITAL PASS only after the user manually runs the canonical full workflow and every required gate succeeds on the same exact SHA.

## Durable decision
Project-library discovery remains a pure, in-memory view over existing project metadata for RC3. Tags, favorites and folders are intentionally deferred because they would introduce new persisted product semantics; H24 solves discovery/organization without schema expansion.
