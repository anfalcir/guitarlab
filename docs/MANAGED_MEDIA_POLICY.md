# Managed Media Policy

Updated: 2026-09-09

This document is normative for GuitarLab Studio media handling.

## Core invariant
GuitarLab never edits the user's external source file. A successful import creates a complete project-managed source copy. That managed source is authoritative and immutable after commit. Ordinary editing is non-destructive metadata work.

## Storage classes
- `projects/<project>/media/source/`: authoritative byte-preserved project-managed sources.
- `projects/<project>/media/proxy/`: derived PCM WAV editing proxies used when the native source is not directly consumable by the Studio WAV engine.
- `projects/<project>/media/derived/` and waveform cache locations: regenerable derivatives such as envelopes/future renders/resampled files.
- `project.json`: project/edit metadata. It is not an audio container.

`managedSourcePath` always means authoritative managed source. `managedEditProxyPath` means optional derivative. Code resolving playback/waveform/edit media must prefer the proxy when present and otherwise use the managed source.

## Import transaction
1. Open the external document read-only.
2. Detect the supported source format from document name/MIME policy.
3. Copy all source bytes into managed source storage transactionally.
4. Preserve that source unchanged after commit.
5. When required, decode to a temporary PCM WAV and commit it separately as an edit proxy.
6. Validate metadata/audio and generate waveform from the editing representation.
7. Persist the clip only after source/proxy validation succeeds.
8. On failure before commit, remove only assets created by that failed transaction.

After success the external URI is provenance only. Moving/deleting the external document must not break the project.

## Editing semantics
Move, trim, split, duplicate, clip gain, mute, track migration and future fades are metadata-only. They may not rewrite authoritative source bytes. Trim changes `sourceStartFrame` and `lengthFrames` inside validated bounds.

## Derived data
Proxies, waveforms and renders are derived. Their corruption must never be reported as authoritative-source corruption. Proxies may be regenerated from the managed source when a deterministic decoder path is available.

## Portable project packages
`.guitarlab` is a versioned ZIP package, not a destructive save format. The current bundle contains:
- `manifest.properties` with format/version/project identity/schema;
- `project.json`;
- each referenced managed source exactly once;
- each referenced edit proxy exactly once when present.

Restore rules:
- validate manifest and project before publishing;
- reject unsupported bundle versions/inconsistent identities;
- constrain every extracted path to a temporary project root;
- apply entry/uncompressed-size bounds;
- verify referenced media exists;
- assign a new project identity so restore cannot silently overwrite an existing project;
- publish the validated directory only after all checks pass.

## Deletion and garbage collection
Removing a clip removes its project reference, not by rewriting source media. Orphan cleanup remains conservative: prefer retained orphan media over accidental deletion until reference-safe garbage collection is implemented and tested.

## Master export
Master export is separate from edit proxies. It renders the current project/timeline/mix into a new destination and never replaces a managed source or proxy. The alpha13 render pipeline produces a floating-point master representation before format encoding.

## Backward compatibility
Legacy projects may still contain external `sourceUri` references. New imports use managed source storage. Compatibility readers must tolerate missing `managedEditProxyPath` and resolve existing managed WAV sources directly.

## Test expectations
Software gates cover source/proxy persistence compatibility, rollback, package round-trip, manifest/version validation, ZIP traversal rejection, referenced-media validation and WAV Float32 writer behavior. Device validation must additionally prove projects remain usable after external originals disappear and that requested imports/exports work on the target Android device.
