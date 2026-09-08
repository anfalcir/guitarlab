# Architecture

## Repository structure
- `app`: Android application, Compose UI, navigation and Android lifecycle/view models.
- `core:model`: durable project/domain model and validation.
- `core:project`: project persistence and serialization.
- `core:audio`: platform-neutral audio probe contracts/policies.
- `core:codec`: platform-neutral codec contracts, WAV parser/decoder and sample-rate planning.
- `platform:audio-android`: Android audio probe implementation and device/routing integration.
- `platform:codec-android`: SAF/content URI random-access adapter with cache fallback.
- `.source-parts`: split source materialization required by CI for the large Android audio engine source.
- `scripts`: deterministic CI/build helpers.

## Dependency direction
Domain/core modules must not depend on Android. Android modules adapt platform services to core contracts. UI may depend on both core and Android adapters, but codec/audio behavior should remain testable without Compose.

## Project persistence
Project JSON is metadata, not a media container. It stores project identity/configuration, groups, tracks, roles and non-destructive clip placement. Raw audio remains referenced by URI or, for recorded/project-managed assets, by controlled app storage.

`FileProjectRepository` validates before save and writes through a temporary file followed by atomic replace when available. This prevents partial JSON writes from becoming normal project state.

## Clip model
An `AudioClip` is a non-destructive view over a source:
- source reference;
- destination track;
- timeline start;
- source start;
- length;
- gain/mute;
- technical source metadata as added by M4 to support transport/resampling decisions.
Editing the clip must not rewrite the source audio unless a deliberate render/export operation is requested.

## Codec architecture
`core:codec` defines file-format/encoding metadata, seekable byte sources, readers/decoders and sample-rate strategy. Android SAF documents are adapted to seekable sources. Providers that do not support reliable random access are copied to temporary cache for the operation; temporary cache is deleted on close/error.

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
