# UI Copy Style

Updated: 2026-09-16

## Scope
This standard applies to all user-visible GuitarLab copy: Home, New Project, Prepare, Studio, mixer, Export, Activity, Settings, backup/restore, dialogs, diagnostics, accessibility descriptions, snackbars, notifications and the in-app `Ajuda` guide.

## Product-first rule
Every normal product surface is written for an end user, not for the developer, test operator or a specific project conversation.

- Describe the user's goal, current state, result and next action.
- Do not expose implementation mechanisms merely because they are technically accurate. Examples that belong in code/docs rather than ordinary UI include Android API names, internal transaction markers, source hashes, workflow/materializer terminology and milestone identifiers.
- Do not mention a specific user's personal setup, repertoire, device model or development history in generic product copy.
- Hardware/model names may appear only when the identity itself is necessary to the task (for example, an explicit device-selection/diagnostic detail), not as the default generic status wording.
- Technical audio terms are allowed when they are meaningful to the intended surface, such as sample rate, bit depth, PCM encoding, loopback, jitter or drift in audio configuration/diagnostics.
- Errors should translate internal failures into a clear user consequence and, when possible, a safe next action.

## Capitalization
- Use sentence case for titles, labels and actions: capitalize the first word and proper names only.
- Preserve standard acronyms and control names when intentionally displayed, for example `REC`, `L/R`, `WAV`, `FLAC`, `MP3`, `PCM`, `RMS`, `ATIVA`, `OCULTA` and the compact `MASTER` strip label.
- Do not use title case for ordinary Portuguese actions. Example: `Auto seções`, not `Auto Seções`.

## Punctuation
- Titles, button labels, menu actions, field labels and compact status labels do not end with a period.
- Full explanatory sentences and paragraphs end with appropriate punctuation.
- Short metadata/data rows may omit terminal punctuation.
- Use the ellipsis character `…` for in-progress states instead of three periods.

## Terminology
- Prefer the established Portuguese term when the same concept already appears translated elsewhere in the app: `Taxa de amostragem`, `Profundidade de bits`, `Codificação`, `Sem perdas`.
- Keep established audio/DAW terms when translation would reduce clarity, including `fade-in`, `fade-out`, `loopback`, `jitter`, `drift`, `take`, `Mute` and `Solo`.
- Use `clipe`/`clipes`, never the English plural `clips` in Portuguese prose.
- Treat `master` as a common noun in prose and accessibility text; all-caps `MASTER` is reserved for the compact mixer strip label.
- Never expose internal milestone names, CI/workflow state, implementation details or developer-only terminology in ordinary user-facing copy.
- Use `Preparar`, `Studio` and `Exportar` as the three project-workspace names.
- Prefer `Arquivos para estudo` over `Study Exports`.
- Prefer `Mix final do Studio` over an untranslated `Studio Master` heading.
- Prefer `Base sem guitarra` and `Guitarra de referência` in primary UX.
- Prefer `Arquivos do projeto` or `Mídia do projeto` over the generic English word `assets`.
- Prefer `Faixas separadas` in primary explanation; `stems` may appear as a secondary technical term.
- Do not show raw `AssetRole`, remote-job enum names, project/operation IDs or SHA fragments outside Details/Diagnostics.
- Do not use `GBW` as a user-facing workspace/product name in the unified line.
- Cloud implementation names such as Firebase or Cloud Run remain diagnostics/consent details; normal UX says `Processamento em nuvem` where appropriate.

## Accessibility
Content descriptions follow the same product-first and sentence-case rules and should describe the action or control directly, without decorative punctuation.

## Release discipline
Any user-visible rename must update dependent Compose instrumentation and the `Ajuda` guide in the same change. Release candidates require a product-copy audit that searches for internal implementation/release vocabulary and accidental personal/hardware-specific wording in normal screens. Copy review is part of release hardening, not a post-release cleanup task.


## State and action wording
Every non-trivial state should answer, in this order:
1. what is happening;
2. whether the user's data is safe;
3. what action is available now.

Prefer:
- “Separando a música…”;
- “Preparando base e guitarra…”;
- “Pronto para o Studio”;
- “Aguardando conexão”;
- “Tentar novamente”;
- “Trocar fonte”;
- “Ver detalhes”.

Avoid:
- raw enum labels;
- unexplained numeric status codes;
- implementation-stage words such as publish/staging/ACK in primary UX;
- messages whose only useful content is “erro”.

## One-workflow naming rule
A capability has one canonical user-facing name across Home, project shell, menus, notifications and help.

Examples:
- if the destination is the Export workspace, use `Exportar`; do not alternate among `Salvar e exportar`, `Compartilhar`, `Área de exportação` and `Export` unless the action semantics truly differ;
- if an action opens backup management, use one stable Backup/restore label;
- project navigation must use the exact workspace names `Preparar`, `Studio`, `Exportar`.

## Background-operation wording
Notifications, Activity and in-screen progress must use the same semantic status mapper. A job may have provider-specific internal states, but the user-facing language must stay stable when implementation changes.

## Release cohesion scan
U10/U11 copy audit must search for:
- `GBW` in normal UI strings;
- `Study Exports`;
- raw `STEM_` / `REFERENCE_` role text;
- raw remote-job state names;
- SHA/operation/project identifiers in primary UI;
- duplicate export labels that imply independent workflows;
- obsolete file-handoff wording between Prepare and Studio.
