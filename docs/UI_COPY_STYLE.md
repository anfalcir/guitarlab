# UI Copy Style

Updated: 2026-09-14

## Scope
This standard applies to all user-visible GuitarLab copy: Home, project creation, Studio, mixer, options, dialogs, diagnostics, accessibility descriptions, snackbars and the in-app `Ajuda` guide.

## Capitalization
- Use sentence case for titles, labels and actions: capitalize the first word and proper names only.
- Preserve standard acronyms and control names when they are intentionally displayed as such, for example `REC`, `L/R`, `WAV`, `FLAC`, `MP3`, `PCM`, `RMS`, `ATIVA`, `OCULTA` and the compact `MASTER` strip label.
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
- Never expose internal milestone names such as `M6`, implementation details or developer-only terminology in user-facing copy.

## Accessibility
Content descriptions follow the same sentence-case rules and should describe the action or control directly, without decorative punctuation.

## Release discipline
Any user-visible rename must update dependent Compose instrumentation and the `Ajuda` guide in the same change. Copy review is part of release-candidate polish, not a post-release cleanup task.
