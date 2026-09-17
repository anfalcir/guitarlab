# UI Copy Style

Updated: 2026-09-16

## Scope
This standard applies to all user-visible GuitarLab copy: Home, project creation, Studio, mixer, options, backup/restore, dialogs, diagnostics, accessibility descriptions, snackbars and the in-app `Ajuda` guide.

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

## Accessibility
Content descriptions follow the same product-first and sentence-case rules and should describe the action or control directly, without decorative punctuation.

## Release discipline
Any user-visible rename must update dependent Compose instrumentation and the `Ajuda` guide in the same change. Release candidates require a product-copy audit that searches for internal implementation/release vocabulary and accidental personal/hardware-specific wording in normal screens. Copy review is part of release hardening, not a post-release cleanup task.
