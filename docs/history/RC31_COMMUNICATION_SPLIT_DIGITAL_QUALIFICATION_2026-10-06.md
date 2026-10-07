# RC31 Communication Split — qualificação digital

Updated: 2026-10-06

## Resultado

**DIGITAL PASS / PHYSICAL ROUTE + FIDELITY PENDING**

RC31 implementa o CUE experimental por estratégia de comunicação sem remover o caminho MULTI_DEVICE. O alvo físico continua MAIN na MK-300 e CUE no fone com fio, com zero ducking e níveis controlados pelo mixer.

## Identidade

- producer/signing SHA: `00fe4d621849bf5f8f1211e25ecba9d6a6c28598`;
- Android CI: **#969 / run `37549317925`**;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc31`; versionCode: `51`;
- unsigned APK SHA-256: `d7d930ca782a6ad326cfb7295e2c2e09b96575cf8b1635c0baed05f60278a46a`;
- signed APK SHA-256: `7d9a0892ac221449ce72d3297e402cd1c605504f82bc808a6c00eaea2e4b3544`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact: `GuitarLabStudio-0.5.0-rc31-homologacao`;
- artifact ZIP digest: `sha256:323f971a7987fba017bb8adff5525cafb536d156fced025e8a9cf30f95fbc4e8`.

The downloaded homologation package was independently checksum-verified against its bundled `SHA256SUMS.txt`.

## Qualification history

CI #968 on implementation SHA `54cc503fe806b2f885f5ed9141b76f0002fc07c4` exposed a Kotlin nullability compile error in restoration of the prior communication device. No signed rc31 was produced.

Producer SHA `00fe4d621849bf5f8f1211e25ecba9d6a6c28598` fixes that issue with a stable non-null local and explicit API-level guards. CI #969 then passed Diff sanity, source guards/materialization, identity validation, Unit tests, performance-evidence collection, Android Lint, debug/release assembly, release-candidate provenance, API 36 instrumented regression, signing, package/certificate verification and signed artifact upload.

## Implemented routing policy

Admission order is:

1. existing MULTI_DEVICE route;
2. if eligible failure occurs, discover the requested CUE in Android communication devices;
3. try CUE with `USAGE_VOICE_COMMUNICATION + CONTENT_TYPE_MUSIC` without changing audio mode;
4. if needed, retry under `MODE_IN_COMMUNICATION`;
5. restore every global communication mutation when the attempt/session ends.

A routing request is never acceptance evidence. The playing MAIN/CUE tracks still must prove exact distinct canonical physical routes and valid clocks. Runtime repeats the proven strategy and retains drift, route-change, non-blocking and fail-closed protections.

## Zero-duck invariant

D-110 remains mandatory. CUE activation does not request ducking, reduce MAIN gain, change project/mixer gains or manipulate Android system volume to manufacture balance. A CI source guard protects this path.

## Diagnostic contract

CUE preflight schema v3 records strategy, communication API availability, available/selected communication physical keys, request acceptance, audio mode before/during, whether communication mode was required, `duckingRequested=false`, `fidelityQualification=PENDING_PHYSICAL`, effective track configuration and route transitions.

## Physical gate

Test target:

```text
MAIN = MK-300
CUE  = fone com fio
```

Interesting success evidence requires `status=SUPPORTED`, `strategy=COMMUNICATION_SPLIT`, MAIN effectively routed only to MK-300 and CUE only to the wired jack.

Even then fidelity remains pending. Physical acceptance must confirm no MAIN duck/pause/gain change, acceptable stereo/music quality on CUE, stable playback/seek/loop, and safe cleanup on disconnect/reconnect.

USB multichannel MAIN 1/2 + CUE 3/4 remains the preferred professional architecture when suitable hardware is available.
