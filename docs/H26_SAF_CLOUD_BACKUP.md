# H26 — SAF Cloud Backup / Restore

Status: **DIGITAL PASS at CI #650 / H26e; product semantics superseded by H27/H28**
Updated: 2026-09-16

## H26 evidence boundary
CI #650 / producer `07c99155789774cb39f9b4382829f9e1d16649e3` proved the H26/H26e implementation digitally. That evidence remains historical for the exact source tested by #650.

## Stable architecture retained
The later correctives retain the core H26 architecture:
- user-selected provider-neutral document-tree destination with persisted scoped read/write access;
- destination probe before adoption;
- safe destination change/disconnect without deleting remote content;
- full and single-project backup;
- automatic incremental/periodic backup;
- restore as independent local copies rather than silent overwrite;
- integrity-verified remote publication with final commit marker;
- invalid/incomplete versions excluded from restore;
- process-wide backup/restore serialization;
- fail-safe cancellation and cleanup boundaries.

## Supersession history
H27 corrected history semantics and end-user copy, and subsequently passed CI #651.

Physical testing of #651 then exposed eventual-consistency behavior in provider listing/commit confirmation. H28 corrects that layer with immutable project identity, strong revision identity, deterministic revision paths, direct-URI commit verification and targeted settling lookup.

Canonical current tail:
`… → H25 → H26 → H26a → H26b → H26e → H27 → H28`.

H26c/H26d remain historical recovery experiments and are not part of the current canonical path.

## Promotion boundary
CI #650 must not be described as containing H27/H28. CI #651 contains H27 but not H28. A fresh manual signed workflow is required before H28 can become the final backup candidate.
