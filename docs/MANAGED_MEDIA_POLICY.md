# Managed Media Policy

This document is normative for GuitarLab Studio media handling.

## Core invariant
GuitarLab never edits the user's external source file. A successful import creates a complete app-controlled project copy, and normal Studio work uses that copy. The internal managed source is also immutable.

## Storage classes
- `projects/<project>/media/source/`: authoritative project-managed source assets. Immutable after commit.
- `projects/<project>/media/derived/`: disposable/regenerable derived data such as waveform envelopes, resampled working files, proxies, freezes and renders.
- `project.json`: metadata only. It references source assets and stores non-destructive edit state; it is not an audio container.

## Import transaction
1. Open the external document read-only.
2. Copy all bytes to a temporary `.part` file under the project's managed source directory.
3. Flush/sync and finalize the copy atomically where supported.
4. Validate/decode the finalized managed copy, never the external file as the durable project dependency.
5. Generate optional derived caches from the managed copy.
6. Persist the project clip reference only after source validation succeeds.
7. If the transaction fails before project commit, delete only the uncommitted copy/cache created by that failed transaction.

After successful commit the external URI is provenance only. Moving/deleting the original external file must not break the project.

## Editing semantics
The following operations are metadata-only and may not alter managed source bytes:
- move;
- trim;
- split;
- clip gain;
- mute;
- fades when implemented;
- time placement and range selection.

Trim changes `sourceStartFrame` and `lengthFrames`; when total source frames are known, validation must keep the view inside source bounds.

## Derived data
Waveforms and future proxies/resampled files are derived artifacts. They must be safe to delete and regenerate. Corrupt/missing derived data must never imply source corruption.

## Deletion and garbage collection
Removing a clip removes its project reference, not by rewriting the source. Automatic orphan-source cleanup is a separate future concern and must prove that no live project reference remains before deleting a managed source. Until then, preferring an orphan over accidental source deletion is the safer policy.

## Export
Export always creates a new destination. It never overwrites a project-managed source as an editing shortcut.

## Backward compatibility
Older development projects may contain direct external `sourceUri` references. These are legacy state. New imports use `managedSourcePath`; future migration may ingest legacy external sources into managed storage, but must never mutate the external originals during migration.

## Test expectations
Software gates must cover copy integrity, empty-input rollback, path traversal rejection, trim bounds, waveform determinism/cache round-trip, project persistence and no write path to finalized source assets. Device validation must additionally confirm a project reopens after the original external file is moved or made unavailable.
