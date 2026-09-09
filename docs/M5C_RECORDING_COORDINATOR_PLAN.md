# M5.C — Recording coordinator implementation gate

This checkpoint formalizes the project-domain contract used by the Studio recording coordinator.

## Current global-input invariant

The current GuitarLab Studio has one global recording input. Therefore each take targets exactly one armed track. If no track is armed, recording is rejected. If more than one track is armed, recording is rejected with a clear instruction to leave only one track armed.

This avoids silently duplicating one physical input into multiple tracks and later doubling playback level. Multi-input/multi-track capture remains a future explicit routing capability rather than an implicit side effect of multiple Arm states.

## Sample-rate policy

- A fixed project sample rate is authoritative.
- In Auto mode, an existing single source rate is reused for recording.
- If Auto-mode project clips already contain conflicting rates, recording is blocked rather than guessing a rate.
- Empty Auto-mode projects may let the Android capture engine negotiate a compatible rate.

## Take integration policy

Only a finalized take already promoted by `ProjectRecordingMediaStore` into `media/source/` can be attached to project metadata.

Integration is non-destructive and metadata-only:
- no source audio is rewritten;
- timeline start is preserved from the REC start frame;
- recorded length is the exact captured frame count;
- current M5 writer metadata is WAV / 32-bit float / mono or stereo;
- the managed source is referenced through both `managedSourcePath` and `managed://` source URI;
- sample-rate mismatch is rejected before the clip is committed.

## Next integration gate

The Studio layer will consume this policy to implement:
1. RECORD_AUDIO permission handoff;
2. visible/cancelable 5-second countdown;
3. revalidation of armed track and selected input at zero;
4. AndroidStudioRecordingEngine startup;
5. input meter and transport state;
6. safe stop / partial-take handling;
7. atomic media promotion;
8. automatic AudioClip + waveform insertion;
9. playback + capture duplex consolidation after capture-only flow is software-green.
