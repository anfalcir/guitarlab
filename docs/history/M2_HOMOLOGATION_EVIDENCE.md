# M2 Homologation Evidence

Date recorded: 2026-09-08

## Target combination
- App diagnostic build: GuitarLab Studio `0.2.0-alpha04` (versionCode 5)
- Android: 16 / API 36
- Device: Samsung SM-X230
- External interface: Pocket Amp exposed by Android as `USB-Audio - USB Composite Device`

## Confirmed USB capabilities from physical run
Pocket Amp input/output were enumerated as USB audio endpoints at 44.1 kHz, 2 channels, FLOAT32. Device IDs changed across disconnect/reconnect cycles, which is expected Android re-enumeration behavior and confirms that runtime code must not persist USB numeric IDs as stable hardware identity.

## Duplex evidence
Reported successful run before hot-unplug testing:
- selected USB input routed to selected USB input;
- selected USB output routed to selected USB output;
- 44.1 kHz / 2 ch / FLOAT32 input and output;
- elapsed approximately 5.1 s;
- capturedFrames = 220500;
- outputFrames = 220500;
- guitar signal peak = 76%;
- guitar signal RMS = 32%;
- outputUnderruns = 0;
- outcome = PASS.

This specifically validates the startup prebuffer/underrun-delta correction against the earlier deterministic one-underrun symptom on the target hardware.

## Hot-unplug evidence
During active Duplex the Pocket Amp cable was removed. The diagnostic correctly returned FAIL because requested USB routing disappeared/fell back rather than reporting false success. The app did not crash. The same controlled-failure/no-crash behavior was subsequently reported PASS for hot-unplug during Play and Record.

## Remaining checklist closure reported by tester
The tester subsequently confirmed all remaining planned M2 items passed:
- isolated Play;
- isolated Record with silence/frame progression;
- isolated Record with real guitar/non-zero input;
- disconnect during Play;
- disconnect during Record;
- disconnect during Duplex;
- idle disconnect/reconnect;
- microphone permission denied;
- repeated stability sequence.

## Gate result
**M2 physical hardware gate: HOMOLOGATED / PASS for Samsung SM-X230 + Pocket Amp on Android 16/API 36.**

This evidence is specific to the tested combination. Compatibility with another Android device or USB interface must be recorded separately; it does not invalidate or silently generalize this result.
