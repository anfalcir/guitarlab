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
- select source using Android document picker;
- invalid source fails safely;
- valid source creates exactly one clip in chosen track;
- technical metadata is consistent with source;
- save and reopen project; clip persists;
- URI remains readable after lifecycle/app restart when persistable access is available;
- remove/edit operations remain non-destructive;
- waveform cache corresponds to source/clip bounds;
- playhead/seek starts and ends at expected positions;
- mismatched project/source sample rates follow explicit strategy;
- repeated import/reopen does not corrupt JSON.

## Later recording gate
- track arm/disarm semantics;
- real guitar capture through intended route;
- safe file finalization on stop;
- interruption/disconnect recovery;
- repeated take creation;
- timeline placement and source duration correct;
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
