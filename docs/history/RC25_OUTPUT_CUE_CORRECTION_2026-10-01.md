# RC25 Bluetooth media output / CUE startup correction

2026-10-01. Qualification pending; no signed identity predeclared.

Evidence: `1000079444.mp4`, 54.27 s / 1728×1080. Early frames show the same Redmi Buds as mono 8/16 kHz and stereo 44.1/48 kHz destinations. Around 25 s the app reports missing stable MAIN/CUE clocks; later disabled secondary selection reports unavailable CUE. Owner additionally reports tablet speaker MAIN + wired headset CUE failure. Video is RC23 presentation, not RC24 UX evidence.

SCO is removed only from media outputs; A2DP/LE media and mono USB remain. SCO recording input remains. Legacy output migration requires one identity match; no candidate path can select the hidden SCO endpoint.

The old admission wrote ~43 ms silence, then gathered only six samples 3 ms apart sequentially on already-draining streams. New admission prefills, continuously writes zeroes non-blockingly to both streams, polls routes/timestamps interleaved and waits up to 1.5 s for three advancing stable samples. Duplicate timestamps are ignored; backwards clocks, convergence, unavailable timestamps, offset >12 ms, negative writes and cancellation remain rejected. Probe cleanup remains in finally. Runtime separation/drift/backpressure guards are unchanged. Actual two-device playback cannot be certified from a menu or emulator.

Bluetooth MAIN + wired CUE is now explicitly unavailable in synchronized-pair settings; Bluetooth alone remains valid MAIN. New CUE activation without configuration preserves MAIN and history. Saved CUE assignments retain fail-closed behavior on route loss; there is no automatic reroute to MAIN.

Tests: delayed/repeated timestamp warmup, both sinks fed, zero startup writes, missing/converged route, stale clocks, excessive offset, cancellation/write error; SCO output filtering, legacy migration, ambiguity, recording-input and mono-USB preservation; instrumented unavailable-CUE metadata/history preservation. Local checks and immutable source reconstruction are recorded after execution. Android build/Lint/unit/instrumented evidence and owner wired-pair physical acceptance remain pending. RC24 branch/payload is preserved and inherited without monitoring its workflow.

Primary reference: Android AudioTrack getTimestamp warmup guidance and AudioDeviceInfo SCO/A2DP definitions: https://developer.android.com/reference/android/media/AudioTrack#getTimestamp(android.media.AudioTimestamp), https://developer.android.com/reference/android/media/AudioDeviceInfo#TYPE_BLUETOOTH_SCO.

A seleção secundária é validada imediatamente em Opções: mostra “Verificando”, abre duas saídas com buffers zerados e volume zero e somente persiste CUE depois de comprovar rotas distintas e clocks estáveis. Falhas mantêm CUE desativado e mostram a causa. Nova seleção, desativação, alteração de MAIN, atualização da lista e saída da tela cancelam/inutilizam resultados pendentes. Diagnósticos de latência não rodam em paralelo com esse teste. O teste usa a taxa do projeto disponível (48 kHz fora do projeto); mede evidência de apresentação relativa, não latência acústica/round-trip. Play/REC ainda verifica os streams reais e mudanças posteriores; uma aprovação anterior não garante suporte permanente do hardware.

Local validation PASS: Kotlin grammar parsing on all changed Kotlin files; whitespace check; Python stage syntax; isolated reconstruction from RC24 baseline; second-run idempotence; all 14 terminal blob hashes; reverse patch check. Selection generation tests cover replacement and invalidation. This environment has no Gradle/Android SDK/Kotlin compiler: compilation, Lint, JVM and instrumented tests are pending CI, not claimed locally. Physical output acceptance remains pending.

## RC25 follow-up — bounded mixer geometry regression

Run #940 (36922266599), RC24 at 3279b8ba: unit tests, Lint and APK build passed; API36 group 02 hit its 480-second bound during `largeFontsGrowChannelsWithoutClippingPanOrShrinkingTargets`. Logcat marks test start without completion. No per-operation log/thread dump exists, so the precise blocked operation is not proven. The identified risk is unbounded `performScrollTo()` retrying unreachable/subpixel geometry.

Large-font regression now reads full layout coordinates (clipped bounds must not hide lost targets), checks width/height growth at 150%, scrolls individual controls with at most 12 direct ScrollBy attempts, detects lack of progress and logs each stage/target/viewport. Full horizontal visibility, vertical dock containment and minimum 48dp target dimensions remain mandatory. Oversized targets fail explicitly. Existing physical swipe regression remains. Group timeout remains 480 seconds; missing coverage can no longer overwrite the original timeout/test exit status.

Immutable RC24 and RC25 audio payloads preserved. New RC25b source stage contains only mixer-test and CI diagnostic corrections. Local PASS: Kotlin grammar; Bash syntax; whitespace; isolated RC25 -> RC25b reconstruction, reverse applicability and second-run idempotence; coverage-failure checks preserve status 124/137/42 and change successful status to failure. Android execution is pending the new CI; no physical/Android pass is declared. No workflows were monitored.

## CI #942 — semantic bounds measurement correction

Run 36926511798 at 2c0ab2d: unit tests, Lint, APK build and Android-test compilation passed. API36 group 02 completed MixerDockInstrumentedTest and failed with `Volume retains 48dp touch height`, instead of timing out. The large-font log shows all four buttons reached and volume inner layout 53px (~20dp at density 2.625). The added full-bounds helper incorrectly read `layoutInfo.coordinates`, the Slider's inner drawing layout, instead of the semantic node's own coordinate/size contract. This does not establish that the actual touch region is too small.

RC25c corrects the helper to public `SemanticsNode.positionInRoot` plus `size` for unclipped semantic geometry. Target-size checks separately use `touchBoundsInRoot` and retain minimum 48dp width/height and dock containment. Both geometry and effective touch bounds are logged. Bounded scroll, font growth assertions and original timeout diagnostics remain. No product drawing/layout change is introduced. Reference: https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsNode .

Local PASS: Kotlin grammar, whitespace, isolated stage reconstruction, reverse-patch applicability and idempotence. New Android execution pending CI; earlier RC25/RC25b payloads immutable. Exact old timeout call remains unproven; this run demonstrates the previously hanging test now terminates with a specific assertion.
