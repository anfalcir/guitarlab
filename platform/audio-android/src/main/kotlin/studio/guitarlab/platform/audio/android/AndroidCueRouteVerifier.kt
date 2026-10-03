package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTimestamp
import studio.guitarlab.core.audio.AudioClockObservation
import studio.guitarlab.core.audio.CueProbeOutput
import studio.guitarlab.core.audio.CueStartupProbe
import studio.guitarlab.core.audio.CueStartupFailure

/** Silent output-only preflight. Does not measure acoustic or input round-trip latency. */
object AndroidCueRouteVerifier {
    @Volatile private var lastPreflightDiagnostic: String? = null

    fun lastPreflightDiagnostic(): String? = lastPreflightDiagnostic

    @Synchronized
    fun verifyDevices(main: AudioDeviceInfo, cue: AudioDeviceInfo, sampleRateHz: Int, keepRunning: () -> Boolean): String? {
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        val observations = linkedSetOf<String>()
        var failure: String? = null
        try {
            if (!keepRunning()) {
                failure = "Verificação cancelada."
                return failure
            }
            if (!AndroidOutputRouteIdentity.expectedPairDistinct(main, cue)) {
                failure = "MAIN e CUE precisam usar saídas físicas distintas."
                return failure
            }
            fun create(device: AudioDeviceInfo): AudioTrack {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(32768, AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)))
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
                try {
                    check(track.state == AudioTrack.STATE_INITIALIZED && track.setPreferredDevice(device))
                    return track
                } catch (failure: Exception) {
                    track.release()
                    throw failure
                }
            }
            mainTrack = create(main)
            cueTrack = create(cue)
            failure = verifyTracks(mainTrack, cueTrack, main, cue, sampleRateHz, keepRunning) { role, keys ->
                observations += "$role=${keys.sorted().joinToString(prefix = "[", postfix = "]")}"
            }
            return failure
        } catch (_: RuntimeException) {
            failure = "O Android não conseguiu abrir as duas saídas selecionadas."
            return failure
        } finally {
            lastPreflightDiagnostic = buildString {
                append("sampleRateHz=").append(sampleRateHz)
                append("; expectedMain=").append(AndroidOutputRouteIdentity.physicalKey(main))
                append("; expectedCue=").append(AndroidOutputRouteIdentity.physicalKey(cue))
                append("; result=").append(failure ?: "PASS")
                if (observations.isNotEmpty()) append("; routed=").append(observations.joinToString(" -> "))
            }
            runCatching { mainTrack?.release() }
            runCatching { cueTrack?.release() }
        }
    }

    fun verifyTracks(mainTrack: AudioTrack, cueTrack: AudioTrack, expectedMain: AudioDeviceInfo,
        expectedCue: AudioDeviceInfo, sampleRateHz: Int, keepRunning: () -> Boolean,
        routeObserver: ((String, Set<String>) -> Unit)? = null,
    ): String? {
        val silence = FloatArray(512 * 2)
        fun adapter(role: String, track: AudioTrack, expected: AudioDeviceInfo) = object : CueProbeOutput {
            override fun feedSilence(): Int = track.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING)
            override fun routedDeviceId(): Int? {
                val keys = AndroidOutputRouteIdentity.routedPhysicalKeys(track)
                routeObserver?.invoke(role, keys)
                val expectedKey = AndroidOutputRouteIdentity.physicalKey(expected)
                return if (AndroidOutputRouteIdentity.routesOnlyToExpected(expectedKey, keys)) expected.id else null
            }
            override fun clockObservation(): AudioClockObservation? {
                val timestamp = AudioTimestamp()
                return if (track.getTimestamp(timestamp)) AudioClockObservation(timestamp.framePosition, timestamp.nanoTime) else null
            }
        }
        return try {
            mainTrack.setVolume(0f)
            cueTrack.setVolume(0f)
            // Prefill before starting; polling then feeds both streams throughout timestamp warmup.
            repeat(8) {
                if (mainTrack.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING) < 0 ||
                    cueTrack.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING) < 0) {
                    return "O Android recusou o buffer silencioso de verificação MAIN/CUE."
                }
            }
            mainTrack.play()
            cueTrack.play()
            // OEM audio policies may defer per-track routing until the track is active. Reassert
            // the already accepted explicit devices after play(), then let the bounded settle phase
            // prove the effective routes through getRoutedDevices()/getRoutedDevice().
            if (!mainTrack.setPreferredDevice(expectedMain) || !cueTrack.setPreferredDevice(expectedCue)) {
                return "O Android recusou reafirmar as rotas MAIN/CUE após iniciar o fluxo silencioso."
            }
            when (CueStartupProbe.verify(
                main = adapter("MAIN", mainTrack, expectedMain), cue = adapter("CUE", cueTrack, expectedCue), expectedMainId = expectedMain.id,
                expectedCueId = expectedCue.id, sampleRateHz = sampleRateHz,
                nowNs = System::nanoTime, sleepMs = { Thread.sleep(it) },
                keepRunning = { keepRunning() && !Thread.currentThread().isInterrupted },
            )) {
                null -> null
                CueStartupFailure.CANCELLED -> "A verificação MAIN/CUE foi cancelada."
                CueStartupFailure.WRITE_FAILED -> "O Android recusou o fluxo silencioso de verificação MAIN/CUE."
                CueStartupFailure.ROUTE_UNCONFIRMED -> "O Android não confirmou duas saídas físicas distintas para MAIN/CUE."
                CueStartupFailure.ROUTE_CHANGED -> "As rotas MAIN/CUE mudaram ou convergiram durante a verificação."
                CueStartupFailure.CLOCK_UNSTABLE -> "Os clocks MAIN/CUE não avançaram de forma estável durante a verificação."
                CueStartupFailure.OFFSET_EXCEEDED -> "O offset inicial entre MAIN e CUE excedeu 12 ms."
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            "A verificação silenciosa MAIN/CUE foi interrompida; CUE permaneceu silencioso."
        } catch (_: RuntimeException) {
            "O Android não conseguiu comprovar sincronização inicial entre MAIN e CUE."
        } finally {
            runCatching { mainTrack.pause() }
            runCatching { mainTrack.flush() }
            runCatching { cueTrack.pause() }
            runCatching { cueTrack.flush() }
            runCatching { mainTrack.setVolume(1f) }
            runCatching { cueTrack.setVolume(1f) }
        }
    }
}
