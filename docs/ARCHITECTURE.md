# Architecture

## Repository structure
- `app`: Android application, Compose UI, navigation and Android lifecycle/view models.
- `core:model`: durable project/domain model and validation.
- `core:project`: project persistence, managed-media storage, waveform cache and timeline/transport policies.
- `core:audio`: platform-neutral audio policies including playback clock, track mix math and meter ballistics.
- `core:codec`: platform-neutral codec contracts, WAV parser/decoder, file random access, waveform-envelope generation and sample-rate planning.
- `platform:audio-android`: Android audio probe implementation plus Studio `AudioTrack` playback/routing/meter integration.
- `platform:codec-android`: SAF/content URI random-access adapter used while selecting/probing external documents where needed.
- `.source-parts`: split source materialization required by CI for the large diagnostic Android audio engine source.
- `scripts`: deterministic CI/build helpers.

## Dependency direction
Domain/core modules must not depend on Android. Android modules adapt platform services to core contracts. UI may depend on both core and Android adapters, but codec/audio/timeline policies remain testable without Compose.

## Project persistence
Project JSON is metadata, not a media container. It stores project identity/configuration, groups, tracks, roles, non-destructive clip placement and durable mix metadata such as master gain. Raw audio lives in project-controlled media storage outside the JSON.

`FileProjectRepository` validates before save and writes through a temporary file followed by atomic replace when available. `ProjectCodec` uses defaults plus `ignoreUnknownKeys`, enabling additive fields such as `masterGainDb` to remain compatible with older project JSON. Legacy projects without that field load at 0 dB.

## Managed media architecture
Every successful import follows an ingest transaction:
1. Android opens the selected external document read-only.
2. `ProjectManagedMediaStore` copies the complete byte stream into `projects/<project>/media/source/` using a temporary `.part` file.
3. The destination is finalized atomically where supported.
4. The managed copy is validated/decoded from local project storage.
5. Only after validation succeeds does the project persist a clip referencing that managed source.
6. If validation/save fails before commit, the uncommitted copy may be rolled back.

After commit, normal Studio work does not require the original external file or persistent SAF permission. `originUri` is provenance only. `managedSourcePath` is the durable project-owned source locator. Source files under `media/source/` are immutable by design.

## Clip and mix model
An `AudioClip` is a non-destructive view over an immutable source. Move/trim/clip gain/mute change metadata only.

`AudioTrack` carries persistent gain, pan, mute, solo and arm metadata. M4 playback combines clip gain + track gain, applies track pan, resolves mute/solo audibility and then sums audible track buses. Master gain is durable `GuitarProject` metadata and is applied after the track sum.

Current validation constrains track/master gain to -60 dB..+12 dB and track pan to -1..+1. Record arm remains metadata-only until M5 activates recording semantics.

## Waveform architecture
Waveform display is derived data, never source data. `WaveformEnvelopeBuilder` produces bounded normalized peak envelopes. `WaveformCacheStore` persists envelopes under `media/derived/waveform/`; missing/corrupt cache may be regenerated from immutable managed media.

## Timeline and transport architecture
`TimelineControlPolicy` owns frame/fraction mapping and bounded playhead/loop positions. `TransportPolicy` defines STOPPED / PLAYING / RECORDING and editing ownership.

While STOPPED, the user may manipulate timeline markers, clips and mix parameters. While PLAYING/RECORDING, transport owns progression and editing is locked in the current engine. View-model methods enforce this in addition to disabled controls.

The canonical transport is return-to-start, play→stop, record and loop; there is no separate Pause state. Play is backed by the real M4 engine. Record remains disabled until M5.

`TimelineMarkerRail` uses explicit top marker heads with >=48 dp targets. Playhead is blue, loop green, trim muted mustard and record red. Loop heads are rendered only when loop is enabled. Trim uses a draft/apply/cancel transaction and never mutates source bytes.

## Studio playback and routing
`AndroidStudioPlaybackEngine` streams float stereo through Android `AudioTrack` in low-latency mode. Supported managed WAV sources currently require project-rate parity and mono/stereo input. Mono is expanded to stereo; stereo retains L/R. Overlapping clips are mixed.

Playback position comes from `AudioTrack.playbackHeadPosition`, not a UI timer. A selected main output from Options is re-resolved at playback time and passed through `AudioTrack.setPreferredDevice`; if unavailable/rejected, playback falls back to Android automatic routing without treating ephemeral numeric device IDs as durable identity.

## Meter architecture
The playback engine builds per-track stereo buses for audible tracks before the master sum. Track meters therefore represent post clip+track gain/pan and pre-master signal. Master peak/RMS is measured after track summing and master gain, before final output clamping, so overload above 0 dBFS remains observable.

Raw chunk meters feed `MeterBallisticsPolicy`, a platform-neutral time-based presentation policy with immediate attack, peak hold and decay. Meter presentation state is transient; only mix parameters are persisted.

## Options / creative-workspace separation
The Studio canvas stays timeline-first and single-screen. Low-frequency commands and configuration are centralized in the Options Center: global recording input, main output, export workflow, project/Studio preferences, codec status and engineering diagnostics. Per-track input routing is intentionally not exposed in current scope.

The Mixer is a bottom dock with independent visible/hidden and pinned/temporary states. Timeline and Mixer share one selected-track concept. The Master strip summarizes routing but route selection remains in Options.

## Codec architecture
`core:codec` defines format/encoding metadata, seekable byte sources, readers/decoders and sample-rate strategy. `FileSeekableByteSource` provides local managed-file random access. Format support is capability-driven; `CODEC_SUPPORT_MATRIX.md` is authoritative.

## UI architecture
Compose screens consume view-model state. Technical diagnostics are separate from the creative Studio. The current visual direction is Graphite Studio: near-black layered surfaces, restrained teal identity and functional semantic colors. Android uses immersive fullscreen; system bars are normally hidden and may be revealed transiently by standard edge gestures.

## Build architecture
GitHub Actions is the canonical remote executor. Toolchain versions are locked. CI materializes split sources, runs unit tests, Android Lint and debug assembly. Signed homologation is a separate controlled job using CI-only secrets and signer fingerprint verification.

## Stability strategy
- `main` is the stable signed baseline;
- feature work stays isolated in draft PR #1 while the M4 physical consolidation gate remains open;
- software CI and physical homologation are separate evidence;
- no secret material is committed;
- documentation is versioned with code so handoff does not depend on chat history.
