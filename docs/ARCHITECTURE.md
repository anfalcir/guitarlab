# Architecture

## Repository structure
- `app`: Android application, Compose UI, navigation and Android lifecycle/view models.
- `core:model`: durable project/domain model and validation.
- `core:project`: project persistence, managed-media storage, waveform cache and timeline/transport policies.
- `core:audio`: platform-neutral audio probe contracts/policies.
- `core:codec`: platform-neutral codec contracts, WAV parser/decoder, file random access, waveform-envelope generation and sample-rate planning.
- `platform:audio-android`: Android audio probe implementation and device/routing integration.
- `platform:codec-android`: SAF/content URI random-access adapter used while selecting/probing external documents where needed.
- `.source-parts`: split source materialization required by CI for the large Android audio engine source.
- `scripts`: deterministic CI/build helpers.

## Dependency direction
Domain/core modules must not depend on Android. Android modules adapt platform services to core contracts. UI may depend on both core and Android adapters, but codec/audio/timeline policies should remain testable without Compose.

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

Source files under `media/source/` are immutable by design. Editing, resampling and rendering create metadata changes or derived files rather than mutating source media.

## Clip model
An `AudioClip` is a non-destructive view over an immutable source: managed source reference, optional provenance URI, destination track, timeline start, source start, length, gain/mute, source bounds and technical metadata.

Move/trim/split/gain/mute operations change project metadata only. Trim is bounded against `sourceTotalFrames` when known.

## Waveform architecture
Waveform display is derived data, never source data. `WaveformEnvelopeBuilder` produces bounded normalized peak envelopes. `WaveformCacheStore` persists envelopes under `media/derived/waveform/`. Missing/corrupt cache may be regenerated from the immutable managed source.

## Timeline and transport architecture
`TimelineControlPolicy` owns deterministic frame/fraction mapping and bounded playhead/loop positions. `TransportPolicy` defines the canonical STOPPED / PLAYING / RECORDING state model and whether timeline editing is permitted.

The critical ownership rule is: while STOPPED, the user may manipulate timeline markers and clip edits; while PLAYING or RECORDING, the transport engine owns timeline progression and user editing is locked. View-model methods enforce the same rule rather than relying only on disabled UI controls.

The Studio transport shell uses symbol-only return-to-start, play→stop, record and loop controls. There is no separate pause state/button. Play/record remain disabled until a real production engine is attached; this preserves the product rule against fake enabled controls.

`TimelineMarkerRail` is a reusable visual layer with explicit top marker heads and minimum 48 dp interaction targets. Semantic colors are restrained accents: playhead blue, loop green, trim muted mustard, recording red. The vertical line is guidance, not the primary interaction surface.

## Codec architecture
`core:codec` defines file-format/encoding metadata, seekable byte sources, readers/decoders and sample-rate strategy. `FileSeekableByteSource` provides local managed-file random access. Android SAF adapters remain useful for external-document diagnostics/import acquisition, but successful Studio imports transition to managed local storage.

Format support is capability-driven. `CODEC_SUPPORT_MATRIX.md` is authoritative for what is planned, implemented and verified.

## Audio architecture
M2 diagnostics are a validation layer, not the final DAW engine. Production transport/recording should use a low-latency Android path suitable for sustained playback/capture, with explicit buffer strategy, route ownership, lifecycle handling and later latency compensation. That engine will publish authoritative audio-clock position into the existing transport/timeline state rather than letting UI timers define playback position.

## UI architecture
Compose screens consume state from view models. Technical diagnostics are separate from the creative Studio. The design system is adaptive light/dark, neutral and low-noise, with restrained teal branding and functional semantic colors.

## Build architecture
GitHub Actions is the canonical remote executor. Toolchain versions are locked. CI materializes split sources, runs unit tests, Android Lint and debug assembly. Signed homologation is a separate controlled job using CI-only secrets and signer fingerprint verification.

## Stability strategy
- `main` is the stable signed baseline;
- parallel feature work stays isolated in a draft PR while hardware gates remain open;
- software CI and physical homologation are reported separately;
- no secret material is committed;
- documentation is versioned with code so a future handoff does not depend on chat history.
