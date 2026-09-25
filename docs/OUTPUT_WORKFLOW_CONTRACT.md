# Output workflow contract

Updated: 2026-09-25

## Separation of concerns

Editable project persistence and external audio delivery are distinct product operations.

- `.guitarlab` save/portable-project behavior preserves an editable project and its required managed media.
- Study Export and Studio Master produce external listening/delivery audio.
- Export never substitutes for project persistence, and project save never masquerades as a rendered master.

## Canonical Export workspace

The canonical Export workspace owns output type, format and destination selection. Home and Studio entry points navigate to that same workflow. Options and local dialogs must not duplicate a competing primary format chooser.

Study/reference export and Studio master delivery remain semantically distinct even when they can share lower-level encoders.

## Non-destructive publication

Export operates from current project/timeline/mix state and does not mutate authoritative source media, prepared references, recording takes or edit metadata.

Destination publication is staged and validated before success is reported. Cancellation/failure must not leave a knowingly corrupt committed output.

## Format claims

Supported formats and device-gated encoder behavior are defined by `CODEC_SUPPORT_MATRIX.md` and `MEDIA_IO_SUPPORT_CLAIM_RULE.md`. A filename extension or available code path alone does not establish support.

## Qualification

Repeat focused output regression when render semantics, mix application, encoder behavior, destination publication or Export navigation changes. Unrelated maintenance work does not reopen output qualification.
