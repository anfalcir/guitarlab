# H35 — Take-Specific Synchronization and Quiet Calibration

Updated: 2026-09-20
Status: DIGITAL PASS — CI #659

## Purpose
H35 separates two different user intents that must not share retroactive behavior:

1. **Global recording fine adjustment** in Settings is a route/rate-scoped default for **future recordings only**.
2. **Take-specific synchronization** is a persistent non-destructive edit owned by one recorded take.

This avoids rewriting past material when a global calibration preference changes and keeps take repair explicit in the creative workflow.

## Global adjustment contract
- Stored per exact input + output + sample rate.
- Default 0 ms; current H34 guardrail remains ±500 ms.
- Captured when a recording session starts and baked once into that new take's initial placement.
- Changing the global value never moves an existing take.

## Take-specific synchronization
`RecordingTake.fineAdjustmentFrames` stores the current take-owned correction, default 0.

Changing the take value:
- computes only `newFine - previousFine`;
- moves every clip carrying the same `takeId` by that exact delta;
- preserves sourceStartFrame, lengthFrames, media bytes, splits and relative clip spacing;
- leaves unrelated takes untouched;
- persists through project save/reopen and portable project serialization;
- participates in the normal project Undo/Redo history;
- fails closed if an advance would cross timeline frame 0 rather than silently trimming source audio.

Because movement is always derived from the stored previous take value, repeated edits do not accumulate drift; returning the take correction to 0 returns the lineage by the exact inverse delta.

## Silent digital verification
The calibration panel now offers **Verificação digital silenciosa**.

It uses PCM zero only to:
- activate the selected input/output;
- require exact live Android device identity for both effective routes;
- obtain stable AudioRecord/AudioTrack timestamp anchors;
- report clock delta and jitter.

It deliberately does **not** create or persist a physical round-trip latency value. Silence can verify clocks/routing but cannot measure the physical output→DSP/analog→input path.

## Physical round-trip calibration
Physical calibration remains available when a real loopback exists, but H35 makes the stimulus safer:
- route activation uses PCM zero first;
- the chirp is not emitted until both routed devices exactly match the selected live AudioDeviceInfo IDs;
- the former harsh pseudo-random ±0.62 burst is replaced with a deterministic 32 ms windowed chirp;
- adaptive peak gain is 0.03 → 0.06 → maximum 0.12;
- the sweep is band-limited (700 Hz to at most 6.5 kHz / sample-rate-safe upper bound);
- correlation remains normalized and the existing multi-pass calibration acceptance policy still decides whether the result is usable.

If the selected route cannot be confirmed, calibration aborts while still silent.

## Validation completed before publication
- actual-domain Kotlin harness PASS for ±500 ms boundaries, take-lineage delta behavior, exact return-to-zero and timeline-zero fail-closed behavior;
- deterministic chirp coverage for 44.1/48/88.2/96 kHz;
- H34→H35 materialization PASS;
- second H35 materialization PASS/idempotent;
- full exact H28→H35 materialization PASS;
- second full materialization PASS/idempotent;
- corrupted H35 source archive rejected fail-closed;
- all 12 H35 terminal Git blob hashes match between the reviewed workspace and the full materialization path;
- diff whitespace sanity PASS.

## Digital evidence — CI #659
Run `35512894518`, producer `a6a53e8ba9e75b32565e451870758c7c65ad687f`, is the exact signed DIGITAL PASS for H35/H35a.

Evidence:
- 330/330 JVM/unit tests PASS;
- Android Lint 0 errors, 50 warnings, 4 hints;
- debug/release build and unsigned provenance PASS;
- 33/33 standard API36 + 1/1 isolated target geometry PASS;
- signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H35a — CI #658 Lint corrective
CI #658 proved H35 materialization, unit tests and API36, but Android Lint reported one `MissingPermission` at `AudioRecord.Builder` inside `buildRecorder()`. H35a adds an explicit `ContextCompat.checkSelfPermission(... RECORD_AUDIO)` guard in that method immediately before constructing `AudioRecord`. No suppression is used and runtime behavior remains fail-closed. H35a passed H35→H35a, full H28→H35a, idempotence and corrupt-source fail-closed materialization proofs locally; CI #659 then passed the complete canonical gate.
