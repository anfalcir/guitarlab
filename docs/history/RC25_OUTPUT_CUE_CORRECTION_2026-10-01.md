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
