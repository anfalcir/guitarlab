# Studio alpha13 homologation handoff

The alpha13 APK is a **candidate**, not a closed milestone. Install on the target Samsung and follow `M5_ALPHA13_FINAL_HOMOLOGATION_CHECKLIST.md` in order.

Highest-risk new checks are:
1. imports: FLAC/AIFF/MP3/M4A/OGG/Opus plus WAV regression;
2. Share modal layout and file picker flow;
3. `.guitarlab` save → Home → open/restore → playback/edit integrity;
4. WAV Float32, FLAC and MP3 master output;
5. previous alpha11 drag failure must not recur, including edge autoscroll.

Any repeatable P0/P1 keeps M5 OPEN and must be corrected before M6.
