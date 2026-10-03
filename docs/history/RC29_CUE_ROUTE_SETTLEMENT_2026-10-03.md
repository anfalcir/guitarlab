# RC29 CUE route settlement correction

Date: 2026-10-03
Status: SOURCE READY — SOFTWARE/API36 QUALIFICATION PENDING

## Owner evidence

`142418.mp4` is a 16.55 s recording from the target SM-X230 / Android 16 using signed RC28. With MAIN `USB-Audio - MK300`, selecting `Fone com fio` as CUE repeatedly enters immediate verification and returns to `Desativada` with `O Android não confirmou duas saídas físicas distintas para MAIN/CUE`. A later attempt to enable CUE on a track is blocked because the secondary selection never persisted.

`GuitarLab-Diagnostics-1791036117830.zip` identifies the tested app as `0.5.0-rc28` / `48`, device `SM-X230`, Android 16 / API36. `audio-route.json` reports selected/effective MAIN as USB-Audio - MK300, CUE candidate availability true, canonical distinction true and USB present. Because RC28 did not persist per-track route observations, the bundle cannot distinguish late route settlement from permanent OEM convergence.

## RC28 evidence retained

- software/API36 PASS: Android CI #958 / run `37124218772`;
- exact-tree signed PASS: Android CI #959 / run `37126984321`;
- producer `a837dd9aa533951448bbbd3607fe60b529894520`;
- Git tree `2971085fcd2e386bc65055d1d7339c6d4e519ec7`;
- signed APK SHA-256 `a916a5908c8b2160d390f10b9019fa3f55833e53297d091dfeaedac3a5438edb`;
- physical synchronized-CUE result: REJECTED by the owner video above.

## Root cause addressed by RC29

RC28 corrected logical-endpoint identity but the preflight still had a single `CueStartupProbe.TIMEOUT_NS = 1.5 s`. That budget included asynchronous Android/OEM route establishment plus collection of multiple stable hardware timestamps. Android route queries are meaningful only while an AudioTrack is playing; a preferred device request is not itself proof of the effective route. The owner failure occurs on the same short timescale as that budget.

RC29 therefore treats route establishment and clock qualification as separate safety phases instead of treating routing latency as immediate incompatibility.

## Correction

- route settlement timeout: 5.0 s;
- required distinct-route stability: four consecutive polls;
- clock qualification timeout after route settlement: 2.0 s;
- both streams remain fed non-blockingly during preflight;
- the explicit MAIN/CUE preferred devices are reasserted immediately after both tracks enter `PLAYSTATE_PLAYING`;
- selection-time probe uses low-latency `AudioTrack` performance mode, matching Studio playback intent;
- once the route pair is qualified, any loss/convergence still fails as `ROUTE_CHANGED`;
- missing routes still fail as `ROUTE_UNCONFIRMED`;
- three stable clock observations, jitter policy and 12 ms initial offset remain unchanged;
- runtime CUE non-blocking write, drift rejection and MAIN protection remain unchanged;
- diagnostic export adds `lastCuePreflight` with expected canonical routes, observed routed-route transitions and final result.

## Regression

Pure JVM coverage adds a delayed-route fixture that becomes valid at 2.2 s — beyond RC28's former 1.5 s window — and must now qualify while remaining below the total bounded deadline. Existing missing-route, converged-route, stale-clock, large-offset, cancellation and write-failure tests remain blocking.

## Release gate

Candidate identity: `0.5.0-rc29` / versionCode `49`.

Run `[run ci]`. Only a complete software + API36 PASS may advance to `[run ci signed]`. Physical acceptance then repeats MK-300 MAIN + wired CUE on the exact signed RC29 APK. If RC29 still fails, export diagnostics immediately in the same app process; `audio-route.json.lastCuePreflight` must expose whether MAIN, CUE or both failed to reach their expected canonical physical route before any further code change.
