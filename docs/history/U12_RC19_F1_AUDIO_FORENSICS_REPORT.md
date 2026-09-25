# U12 RC19 — F1 production audio forensics report

Date: 2026-09-24  
Mode: **READ-ONLY**  
Production project: `gbwapp-ef048`  
Primary incident job: `c3ba40ec-6153-483a-801f-9e838b3e3b60`

## Collection boundary

Evidence was collected using only Firestore document GET, Cloud Logging read,
Cloud Run execution describe and Storage object listing. No Firestore document,
Storage object, Cloud Run resource, quota, job state or deployment was mutated.

Collector: `scripts/u12_readonly_audio_forensics.py`.

## Primary incident result

The physical RC19 job caused a fresh Cloud Run execution. It did not adopt or
reuse a prior result.

- execution: `gbw-demucs-gbrt9`;
- created: `2026-09-24T16:46:47.477989Z`;
- completed successfully: `2026-09-24T17:01:49.194259Z`;
- image: `remote-worker@sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`;
- runtime: 8 vCPU, 16 GiB, 1800 s timeout, one task, zero retries;
- engine/revision: `demucs.cpp` / `f1206e9adeea103aef4a636b9e62297cf1f8e34e`;
- model/SHA: `htdemucs_6s` / `09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856`;
- strategy: `single8` (`demucsThreads=0`, `blasThreads=8`);
- input SHA-256: `b0e24e05bf4adfe73a72b1cef961ad0f5322ddd4b995d8af7a84296f8feb324a`;
- manifest SHA-256: `bc9f32e465928a807008bdd3949e1a6c377debb9718ed678b4dd491751f5c29a`;
- remote backing SHA-256: `cff6be27195341d6881cf61d97927e1f2f50d76ee391f125a67fa923d4015573`;
- remote guitar SHA-256: `092493ae948ae367ed34983a7b8fc749b34deb32bc081c1d8d90055d7d11f53a`;
- sampled reconstruction diagnostic: `1.467621 dB`;
- shared gain: `-4.378385 dB`;
- Firestore terminal state: `IMPORTED`, cleanup `PURGED`;
- Storage inventory: no objects remain under the immutable job prefix.

The remote hashes do not byte-match the supplied local hashes. This is expected
to be possible because `PreparedReferenceProjectPublisher` decodes each accepted
remote WAV and writes GuitarLab's canonical float-WAV container. The project
provenance stores both local asset hashes and `remoteBackingSha256` /
`remoteGuitarSha256`. Hash inequality alone therefore does not prove stale local
assets.

The size relationship further explains the hash change without invoking stale
audio. Each remote file has `63,316,082` bytes, while each supplied local file
has `63,316,012` bytes. For `7,914,496` stereo float32 frames, the PCM payload is
exactly `63,315,968` bytes. The remaining sizes are therefore a 114-byte remote
WAVE_FORMAT_EXTENSIBLE container and a 44-byte local canonical WAV container:
an exact 70-byte header delta. The production publisher decodes and rewrites
that container while preserving frame samples. This is consistent with the
observed local hashes and strongly rejects a historical local-reference mix-up.

Firestore records source asset `2c7f0a12-f52a-4904-8320-c0155b4b41c5`, while
the incident handoff identifies `f8c3e5d7-c759-41e1-945b-4968c3a54b0e`.
The input SHA and project ID match the incident. This asset-ID difference must be
resolved from local project provenance; it is not evidence of remote-result reuse.

## Independent second-source reproduction

The subsequent different project/source also caused a fresh successful execution
and was reported physically unusable:

- job/execution: `c000003e-4293-4d2f-811b-6653f261e7c9` / `gbw-demucs-z7v9m`;
- project: `04063b84-7f22-478c-9e58-f713a4c20801`;
- input SHA-256: `87c052121ac205a13735f1d985df10999f7946366a6e01c79a98e7dac9eb9cda`;
- same immutable image, engine revision, model SHA and `single8` strategy;
- manifest SHA-256: `6aa57620b95e6577f8041aa4cdeac8cee210bd63c82cdd7bd8637e62674b8d1a`;
- remote backing/guitar SHA-256: `417d150963795925662a76976b4cf3b8203aa4290aefff410fbcede3c0205958` /
  `4959c6d17dd307890aa5967c8bebaa18c28465f84805f9f9ab21a8c8202ffef8`;
- sampled reconstruction diagnostic: `1.742529 dB`;
- shared gain: `-1.242556 dB`;
- Firestore terminal state: `IMPORTED`, cleanup `PURGED`.

## Same-source deterministic reproduction

The preserved RC18 job `0860b0a7-6dda-439d-87af-b0f200c1a8b5` was a separate
successful execution (`gbw-demucs-f6hjk`) of the same input. It produced exactly
the same remote backing SHA, guitar SHA, shared gain (`-4.378385 dB`) and sampled
reconstruction diagnostic (`1.467621 dB`) as the later physical RC19 job. The
manifest hashes differ because timestamps/job identity differ; the audio hashes
do not. The bad result is therefore a deterministic result of the pinned
worker/model/strategy for that input, not remote-result reuse.

## F1 conclusion

Case A (no fresh execution/reused result) is rejected for both reproductions.
The primary incident is Case B: a fresh worker execution deterministically
produced the exact same remote audio as the earlier independent same-source
execution. Case C is strongly rejected by the exact frame/payload/container-size
relationship and the fail-closed publication path. Case D cannot yet be separated
from the wider Case B worker boundary without stage-level audio evidence.

The defect reproducing across two independent inputs on the same immutable
worker/model/runtime makes a source-specific failure substantially less likely.
It does not yet distinguish inference from stem normalization, prepared-reference
mixing/shared gain, Android canonicalization, or playback/binding. F2 must compare
source and local references. Because remote objects were purged and internal stems
were never retained, F3 controlled diagnostic replay is required if local evidence
does not isolate the boundary.

No audio-algorithm correction is authorized by F1 alone.
