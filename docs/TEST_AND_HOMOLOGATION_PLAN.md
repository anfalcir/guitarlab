# Test and Homologation Plan

## Software gate for every development checkpoint
Mandatory:
1. source materialization succeeds;
2. unit tests pass;
3. Android Lint passes;
4. debug APK assembles;
5. CI diagnostics/artifacts upload succeeds.
A green CI run means the software gate passed; it does not close a physical-device gate.

## Signed homologation gate
Used only for explicit homologation candidates.
- restore signing bundle from CI secret into runner-temp only;
- build release/homologation APK;
- verify APK signer fingerprint against the locked expected certificate;
- publish APK plus hashes/build identity;
- destroy temporary signing material.
Routine feature commits must not trigger signed builds.

## M2 physical audio gate — OPEN until Pocket Amp is available
On target Android tablet:
- install over previous signed version without uninstall;
- grant/deny RECORD_AUDIO tests;
- connect Pocket Amp over USB/OTG;
- verify selected input/output route;
- Play probe;
- Record silence: frames advance, expected near-zero signal;
- Record guitar: frames advance, non-zero peak/RMS;
- Duplex ~5 s: stable capture/playback, no sustained underrun pattern;
- idle disconnect/reconnect;
- disconnect during play, record and duplex: controlled failure/no crash;
- repeat critical sequence 10x;
- no P0/P1 crash, wrong route or hotplug failure.
Pocket Amp loopback should be OFF when isolation/duplex behavior is being evaluated.

## M3 codec gate
For each enabled format/variant:
- valid metadata parsing;
- complete-frame accounting;
- random seek accuracy;
- malformed/truncated rejection;
- finite/clamped decoded samples where applicable;
- Android direct-seek provider test;
- Android cache-fallback provider test when available;
- real representative file on target tablet.
Current physical evidence includes a real WAV PCM24 / 44.1 kHz / stereo file with direct seek and exact midpoint seek behavior. This does not imply every WAV variant is physically homologated.

## M4 Studio gate
For each supported import path:
- external source is opened read-only and copied completely into project-managed `media/source/` storage;
- invalid/empty/malformed source fails safely and leaves no committed clip;
- valid source creates exactly one clip in chosen track;
- managed copy and external original remain byte-unchanged through edit/waveform workflows;
- project reopens independently after the external original becomes unavailable;
- non-destructive trim stays within immutable source bounds;
- waveform envelope/cache is deterministic and regenerable;
- repeated import/reopen does not corrupt JSON or source media.

### Timeline/transport UX gate
- playhead/loop/trim controls use explicit top marker heads with at least 48 dp touch targets;
- the thin vertical guide is not required as the drag target;
- playhead is blue, loop green, trim muted mustard `#9C741F`, record red; identity is also conveyed by glyph/context;
- canonical transport bar order is return-to-start, play/stop, record, loop;
- no separate pause button exists; state model is STOPPED / PLAYING / RECORDING;
- while PLAYING or RECORDING, user manipulation of playhead, loop, trim, clip move/trim, import and clip editing is blocked;
- the UI visibly communicates marker lock while transport is active;
- loop configuration cannot change during active transport;
- Play/Record remain disabled until their real engines are implemented; an enabled no-op/fake transport is a gate failure.

### Production transport gate (next)
- Play starts real managed-media playback and button becomes Stop;
- Stop returns state to STOPPED without corrupting playhead/timeline state;
- audio-clock-driven playhead progression matches audible position;
- return-to-start/seek positions are sample/time consistent;
- enabled loop repeats exactly between loop markers without marker mutation;
- stopping unlocks timeline editing immediately and safely;
- active transport never mutates immutable source assets;
- playback errors/disconnects fail to a controlled stopped/error state.

## Later recording gate
- track arm/disarm semantics;
- real guitar capture through intended route;
- safe file finalization on stop;
- interruption/disconnect recovery;
- repeated take creation;
- timeline placement and source duration correct;
- all user timeline edits remain locked while RECORDING;
- monitoring does not unintentionally double the signal;
- latency/alignment measurements recorded before compensation is claimed.

## Regression policy
Every homologation candidate must rerun prior milestone-critical checks that can be affected by the change. Codec/UI work cannot silently regress M2 routing; transport work cannot regress project persistence; export work cannot alter source assets.

## Severity
- P0: data loss, security/signing compromise, unrecoverable project corruption.
- P1: crash, wrong audio route, destructive source modification, unusable core workflow.
- P2: incorrect metadata/timing/visual state with workaround.
- P3: cosmetic/minor usability issue.
No candidate closes a milestone with known P0/P1 defects in that milestone scope.

## Evidence
Record exact app version/commit, device/API, hardware route, source media characteristics, test result and hashes where applicable. Physical evidence and software CI evidence must never be conflated.
