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
    @Synchronized
    fun verifyDevices(main: AudioDeviceInfo, cue: AudioDeviceInfo, sampleRateHz: Int, keepRunning: () -> Boolean): String? {
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        return try {
            if (!keepRunning()) return "Verificação cancelada."
            if (main.id == cue.id) return "MAIN e CUE precisam usar saídas distintas."
            fun create(device: AudioDeviceInfo): AudioTrack {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(32768, AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)))
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
            verifyTracks(mainTrack, cueTrack, main, cue, sampleRateHz, keepRunning)
        } catch (_: RuntimeException) {
            "O Android não conseguiu abrir as duas saídas selecionadas."
        } finally {
            runCatching { mainTrack?.release() }
            runCatching { cueTrack?.release() }
        }
    }

    fun verifyTracks(mainTrack: AudioTrack, cueTrack: AudioTrack, expectedMain: AudioDeviceInfo,
        expectedCue: AudioDeviceInfo, sampleRateHz: Int, keepRunning: () -> Boolean): String? {
        val silence = FloatArray(512 * 2)
        fun adapter(track: AudioTrack) = object : CueProbeOutput {
            override fun feedSilence(): Int = track.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING)
            override fun routedDeviceId(): Int? = track.routedDevice?.id
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
            when (CueStartupProbe.verify(
                main = adapter(mainTrack), cue = adapter(cueTrack), expectedMainId = expectedMain.id,
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
