# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Homologation candidate: `0.5.0-rc2`, versionCode 22.
- Exact source commit and APK SHA-256: recorded after the final local gate and signing.
- Signed APK: `GuitarLabStudio-0.5.0-rc2-homologacao.apk`.
- Previous official RC1 signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Milestone state
- M2 through M6: PASS/CLOSED.
- M7/M8 digital baseline: PASS in signed `0.4.0-rc2` / CI #593.
- RC1 consolidated delta: software gate PASS; release assembly/signature/integrity PASS; residual physical homologation OPEN.

## RC1 consolidated scope
- bounded live waveform while recording;
- explicit input routing fails closed when Android does not confirm the selected device, preventing silent fallback to the tablet microphone;
- software monitoring is presentation-only and never mixes backing tracks into the recording writer;
- route capability/health visibility for the MK-300 workflow;
- return-to-start available during playback and Play/Stop visibly disabled during recording;
- playback stop actively interrupts, flushes and joins the previous session to prevent a stuck transport;
- takes with exactly one active take per track and consistent delete/move/split/stereo reconciliation;
- Reference / My Guitar / Both audition modes;
- markers, manual sections, automatic section suggestions and section looping;
- loop-derived punch recording with pre-roll/post-roll and latency-aware retained region;
- track RMS/peak analysis with bounded, explicit gain recommendation/application.

## RC2 physical-feedback refinement
- sections, markers and punch are integrated into the shared timeline header and aligned with waveform ranges;
- touching a section applies it as the loop region, while integrated remove actions preserve editability without a separate rail;
- comparison, timeline and punch controls are visually grouped by purpose;
- affected guitar tracks show `ATIVA` or `OCULTA` in both the track sidebar and Mixer, derived from the playback audition policy;
- the manual command is explicitly named `Criar seção do loop`: it stores the current loop bounds as a named section.

## Validation evidence
The exact RC1 source passed locally: `git diff --check`, JVM `test`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` and signed `assembleRelease`.

The full API 36 instrumented suite and isolated 1920×1200 geometry passed for the preceding RC2 baseline in CI #593. The RC1 delta adds JVM coverage and compiles all Android instrumentation, but was not executed on a new emulator because this environment has no KVM and automatic GitHub Actions are intentionally disabled. No claim is made that this replaces the residual target-device pass.

## CI and recovery policy
Ordinary commits do not trigger GitHub Actions. `.github/workflows/android-ci.yml` is manual-only. Local compilation uses the persistent GuitarLab Build Kit, which contains the pinned JDK/Gradle/Android SDK and dependency caches but no source code, signing key or credentials.

## Remaining physical gate
Only facts requiring the real Samsung SM-X230 + M-VAVE MK-300 remain:
- verify MK-300 input isolation with hardware loopback disabled;
- observe the live waveform during a real take;
- verify capture contains guitar only while backing plays;
- exercise recording stop/restart, return-to-start, reopen and route reconnect;
- judge monitoring latency, pops/dropouts and subjective audio quality;
- verify the new practice/take/punch controls and tablet ergonomics;
- target MP3 availability/playability and one representative stress/export smoke.

After explicit user approval, record PASS/CLOSED directly on `main`.
