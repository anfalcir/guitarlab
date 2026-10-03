# RC28 CUE physical-route identity correction

Date: 2026-10-03
Status: SOURCE READY — SOFTWARE/API36 QUALIFICATION PENDING

## Owner evidence

`142269.mp4` (8.2 s, 1728×1080) on the target Android tablet shows:

1. MAIN selected as `USB-Audio - MK300`;
2. CUE selected as `Fone com fio`;
3. immediate silent verification enters `Verificando…`;
4. verification fails with `CUE desativado. O Android não confirmou duas saídas físicas distintas para MAIN/CUE.`.

This is exactly the physical pair the RC25 history left pending for owner acceptance.

## Root cause

GuitarLab's selector already canonicalizes one physical USB output across several Android logical endpoints and keeps their candidate ids together. The RC25 CUE verifier and runtime guard did not use that abstraction: they required `AudioTrack.routedDevice.id == requested AudioDeviceInfo.id`. On Android 16/API36 that is too strict because a HAL may route through a sibling logical endpoint and `AudioRouting.getRoutedDevices()` can expose more than one logical routed device.

The defect is therefore an identity-layer mismatch, not evidence that MK-300 + wired CUE is physically unsupported.

## Correction

RC28 adds `AndroidOutputRouteIdentity` and uses it in both selection-time admission and runtime safety.

- USB endpoints canonicalize by normalized product + physical card while logical device/endpoint address components are stripped.
- Built-in speaker aliases canonicalize as one physical destination.
- Wired headset/headphones output modes canonicalize as one physical jack by address, independent of the mode/product label; line/AUX outputs remain distinct.
- Other routes keep type/product/address identity.
- API36 reads the complete routed-device set.
- A stream passes only when all routed logical endpoints map to the selected physical route.
- Empty route evidence, a second physical destination, or MAIN/CUE physical convergence fail closed.
- The candidate-output probe likewise accepts multiple API36 logical routed endpoints only when every id belongs to the selected canonical candidate group.
- Clock stability, three observations, 12 ms initial offset, continuous drift and non-blocking secondary-write guards are unchanged.

## Regression

New JVM coverage proves same-card USB aliases share one physical identity, different USB cards remain distinct, built-in speaker aliases do not create false routes, raw wired types do not collapse, and an unexpected extra physical route is rejected. Existing CUE startup/clock/backpressure tests remain authoritative.

## Release gate

Candidate identity: `0.5.0-rc28` / versionCode `48`.

Run `[run ci]` first. Signing is allowed only after deterministic materialization, unit tests, Android Lint, release build and relevant API36 regression pass. Physical acceptance then uses the exact signed RC28 APK on SM-X230 / Android 16 with MK-300 MAIN + wired CUE. Verify CUE selection persists, CUE-only track is heard only on the wired output, MAIN remains isolated, Play/REC stay stable, and disconnect/reconnect still fails closed.
