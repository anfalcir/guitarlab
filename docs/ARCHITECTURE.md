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
Project JSON is metadata, not a media container. It stores project identity/configuration, groups, tracks, roles, non-destructive clip placement and durable mix/visual metadata such as Master gain and track color selection. Raw audio lives in project-controlled media storage outside the JSON.

`FileProjectRepository` validates before save and writes through a temporary file followed by atomic replace when available. `ProjectCodec` uses defaults plus `ignoreUnknownKeys`, enabling additive fields such as `masterGainDb` and `AudioTrack.colorIndex` to remain compatible with older schema-v1 JSON. Legacy projects without Master gain load at 0 dB; tracks without an explicit color use `colorIndex=-1` and a deterministic visual fallback.

Live mixer persistence is serialized through a view-model save mutex and re-loads the latest stored project before applying the requested transform. This prevents a completed slider gesture from writing an older project snapshot over an unrelated structural edit.

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

`AudioTrack` carries persistent gain, pan, mute, solo, arm, order and color metadata. Arm remains metadata-only until M5 activates capture semantics.

M4 playback processing is bus-oriented:
1. each clip decodes from immutable managed media;
2. clip gain is applied without rewriting the source;
3. clips are accumulated into their track stereo bus;
4. current live track gain/pan is applied to the bus;
5. track Peak/RMS is measured;
6. track buses sum into Master;
7. current live Master gain is applied;
8. Master Peak/RMS is measured before final clamp/output.

The playback engine stores runtime track mix parameters in a thread-safe map and the current Master gain in volatile runtime state. Compose/ViewModel slider preview updates these values while audio is running; persistence occurs when the gesture completes. If a legacy caller supplies no runtime track mix, clip-level gain/pan behavior remains backward-compatible.

Current validation constrains track/Master gain to -60 dB..+12 dB, pan to -1..+1 and explicit color indexes to the supported palette range.

## Waveform architecture
Waveform display is derived data, never source data. `WaveformEnvelopeBuilder` produces bounded normalized peak envelopes. `WaveformCacheStore` persists envelopes under `media/derived/waveform/`; missing/corrupt cache may be regenerated from immutable managed media.

Track color is presentation metadata only. The same resolved track color is used by sidebar identity, clip/waveform and Mixer strip; it never affects media bytes.

## Timeline and transport architecture
`TimelineControlPolicy` owns frame/fraction mapping and bounded playhead/loop positions. `TransportPolicy` defines STOPPED / PLAYING / RECORDING and structural editing ownership.

Structural timeline/clip/track edits remain STOPPED-only in M4. Live gain/pan/Master are explicit transport-time exceptions because the Android playback engine now exposes thread-safe runtime mix setters. This exception is narrow and does not enable trim, clip deletion, track structure changes or M/S/R mutation during active transport.

The canonical transport is return-to-start, play→stop, record affordance and loop; there is no separate Pause state. Play is backed by the real M4 engine. Record remains disabled until M5.

`TimelineMarkerRail` uses explicit top marker heads with >=48 dp drag targets. Playhead is blue, loop green, trim muted mustard and record red. Loop heads render only when loop is enabled. Trim uses a draft/apply/cancel transaction and never mutates source bytes.

The marker rail, ruler and every waveform lane now share one timeline origin: all start after the exact same fixed sidebar width and inter-lane gap. Marker frame→fraction mapping therefore maps to the same horizontal waveform geometry instead of spanning the sidebar.

## Studio playback and routing
`AndroidStudioPlaybackEngine` streams float stereo through Android `AudioTrack` in low-latency mode. Supported managed WAV sources currently require project-rate parity and mono/stereo input. Mono is expanded to stereo; stereo retains L/R. Overlapping clips are mixed.

Playback position comes from `AudioTrack.playbackHeadPosition`, not a UI timer. A selected main output from Options is re-resolved at playback time and passed through `AudioTrack.setPreferredDevice`; if unavailable/rejected, playback falls back to Android automatic routing without treating ephemeral numeric device IDs as durable identity.

## Meter and clip-latch architecture
The engine emits raw per-track and Master Peak/RMS. `MeterBallisticsPolicy` remains a platform-neutral transient presentation policy with immediate attack, 750 ms peak hold and time-based decay.

Clipping is stored separately as view-model latch state. A track or Master latch is set when its raw pre-clamp peak exceeds 1.0 (0 dBFS). Meter values reset on Stop, but latches persist until tapped. Starting a new playback/record attempt resets all latches before the next run. Clip-latch state is session UI state and is not serialized into project JSON.

## Track management architecture
M4 track management is metadata-only and project-safe:
- add a generic mono track;
- rename;
- select one of 20 colors;
- reorder by normalized `order` values;
- delete only if no clip references the track.

The sidebar gear owns these low-frequency per-track operations. Project validation prevents invalid persisted color ranges and repository validation protects referential integrity.

## Options / creative-workspace separation
The Studio canvas stays timeline-first and single-screen. Low-frequency configuration is centralized in the Options Center: global recording input, main output, export workflow, project/Studio preferences, import capability and advanced diagnostics. Per-track input routing is intentionally not exposed in current scope.

Mixer pin state is application/UI preference data in `SharedPreferences`, not project mix metadata. The Mixer has a scrolling track region plus a fixed Master strip at the right. Pinned state survives navigation away from the Studio; closing a pinned Mixer also clears the pin preference.

## Codec architecture
`core:codec` defines format/encoding metadata, seekable byte sources, readers/decoders and sample-rate strategy. `FileSeekableByteSource` provides local managed-file random access. Format support is capability-driven; `CODEC_SUPPORT_MATRIX.md` is authoritative.

## UI architecture
Compose screens consume view-model state. Normal product UI is pt-BR. Conventional frequent actions favor icon controls with accessibility descriptions; technical milestone/homologation wording is kept out of the creative workflow and remains available only in documentation/advanced diagnostics where useful.

Graphite Studio remains the visual foundation but uses a broader semantic palette: teal product identity, blue secondary accents, amber attention/Solo, red/coral record/mute/error, and a 20-color track identity palette. Android uses immersive fullscreen; system bars are normally hidden and may be revealed transiently by standard edge gestures.

The Studio top bar owns project identity at left, transport at center and Mixer/Options/Home icons at right. This removes the former bottom transport bar and returns vertical space to the timeline.

## Build architecture
GitHub Actions is the canonical remote executor. Toolchain versions are locked. CI materializes split sources, runs unit tests, Android Lint and debug assembly. Signed homologation is a separate controlled job using CI-only secrets and signer fingerprint verification.

## Stability strategy
- `main` is the stable signed baseline;
- feature work stays isolated in draft PR #1 while the M4 physical consolidation gate remains open;
- software CI and physical homologation are separate evidence;
- no secret material is committed;
- documentation is versioned with code so handoff does not depend on chat history.
