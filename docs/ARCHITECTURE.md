# Architecture

## Repository structure
- `app`: Android application, Compose UI, navigation and Android lifecycle/view models.
- `core:model`: durable project/domain model and validation.
- `core:project`: project persistence, managed-media storage and derived waveform cache.
- `core:audio`: platform-neutral audio probe contracts/policies.
- `core:codec`: platform-neutral codec contracts, WAV parser/decoder, file random access, waveform-envelope generation and sample-rate planning.
- `platform:audio-android`: Android audio probe implementation and device/routing integration.
- `platform:codec-android`: SAF/content URI random-access adapter used while selecting/probing external documents where needed.
- `.source-parts`: split source materialization required by CI for the large Android audio engine source.
- `scripts`: deterministic CI/build helpers.

## Dependency direction
Domain/core modules must not depend on Android. Android modules adapt platform services to core contracts. UI may depend on both core and Android adapters, but codec/audio behavior should remain testable without Compose.

## Project persistence
Project JSON is metadata, not a media container. It stores project identity/configuration, groups, tracks, roles and non-destructive clip placement. Raw audio lives in project-controlled media storage, outside the JSON.

`FileProjectRepository` validates before save and writes through a temporary file followed by atomic replace when available. This prevents partial JSON writes from becoming normal project state.

## Managed media architecture
Every successful import follows an ingest transaction:
1. Android opens the selected external document read-only.
2. `ProjectManagedMediaStore` copies the complete byte stream into `projects/<project>/media/source/` using a temporary `.part` file.
3. The destination is finalized atomically where supported.
4. The managed copy is validated/decoded from local project storage.
5. Only after validation succeeds does the project persist a clip referencing that managed source.
6. If validation/save fails before commit, the uncommitted copy may be rolled back.

After commit, normal Studio work does not require the original external file or persistent SAF permission. `originUri` is provenance only. `managedSourcePath` is the durable project-owned source locator.

Source files under `media/source/` are immutable by design. The store deliberately exposes ingest/resolve but no overwrite API. Editing, resampling and rendering create metadata changes or derived files rather than mutating source media.

## Clip model
An `AudioClip` is a non-destructive view over an immutable source:
- managed source reference plus optional original provenance URI;
- destination track;
- timeline start;
- source start;
- length;
- gain/mute;
- source total frames and technical metadata.

Move/trim/split/gain/mute operations change project metadata only. Trim is bounded against `sourceTotalFrames` when known, guaranteeing that the edit remains a valid view over the immutable source.

## Waveform architecture
Waveform display is derived data, never source data. `WaveformEnvelopeBuilder` scans decoded PCM and produces bounded normalized peak envelopes. `WaveformCacheStore` persists envelopes under `media/derived/waveform/` using a small versioned binary cache.

Cache files may be deleted, replaced or regenerated without affecting audio integrity. On project reopen, cached envelopes are loaded when valid; a missing/corrupt cache may be rebuilt from the managed source. UI rendering consumes envelope points only and never decodes or modifies the audio source directly.

## Codec architecture
`core:codec` defines file-format/encoding metadata, seekable byte sources, readers/decoders and sample-rate strategy. `FileSeekableByteSource` provides local managed-file random access. Android SAF adapters remain useful for external-document diagnostics/import acquisition, but successful Studio imports transition to managed local storage.

Format support is capability-driven. `CODEC_SUPPORT_MATRIX.md` is authoritative for what is planned, implemented and verified.

## Audio architecture
M2 diagnostics are a validation layer, not the final DAW engine. Production transport/recording should use a low-latency Android path suitable for sustained playback/capture, with explicit buffer strategy, route ownership, lifecycle handling and later latency compensation.

## UI architecture
Compose screens consume state from view models. Technical diagnostics are separate from the creative Studio. The design system is adaptive light/dark, neutral and low-noise, with restrained teal branding and functional warning/record colors.

## Build architecture
GitHub Actions is the canonical remote executor. Toolchain versions are locked. CI materializes split sources, runs unit tests, Android Lint and debug assembly. Signed homologation is a separate controlled job using CI-only secrets and signer fingerprint verification.

## Stability strategy
- `main` is the stable signed baseline.
- parallel feature work stays isolated in a draft PR while hardware gates remain open;
- software CI and physical homologation are reported separately;
- no secret material is committed;
- documentation is versioned with code so a future handoff does not depend on chat history.
