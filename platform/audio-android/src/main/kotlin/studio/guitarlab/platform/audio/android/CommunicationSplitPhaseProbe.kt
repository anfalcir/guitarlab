package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin

enum class CommunicationSplitProbePhase {
    A_MAIN_ONLY,
    B_COMMUNICATION_DEVICE_SELECTED,
    C_CUE_TRACK_SILENT,
    D_CUE_TONE_ACTIVE,
    E_MAIN_REASSERT_ONLY,
    F_COMMUNICATION_REASSERT,
}

enum class CommunicationSplitProbeAcousticOutcome {
    MAIN_NOT_AUDIBLE_IN_A,
    MAIN_LOST_IN_B,
    MAIN_LOST_IN_C,
    MAIN_LOST_IN_D,
    MAIN_LOST_IN_E,
    MAIN_LOST_IN_F,
    MAIN_AUDIBLE_ALL_PHASES,
    UNABLE_TO_TELL,
}

enum class CommunicationSplitProbePairOutcome {
    MAIN_ONLY,
    CUE_ONLY,
    BOTH,
    NONE,
    UNABLE_TO_TELL,
}

data class CommunicationSplitProbePhaseEvidence(
    val phase: CommunicationSplitProbePhase,
    val elapsedMs: Long,
    val mainPhysicalKeys: List<String>,
    val cuePhysicalKeys: List<String>,
    val communicationPhysicalKey: String?,
    val audioMode: Int,
    val mainPlaybackHeadStart: Long,
    val mainPlaybackHeadEnd: Long,
    val cuePlaybackHeadStart: Long?,
    val cuePlaybackHeadEnd: Long?,
    val mainWrittenSamples: Long,
    val cueWrittenSamples: Long,
    val mainZeroWrites: Int,
    val cueZeroWrites: Int,
    val mainShortWrites: Int,
    val cueShortWrites: Int,
    val mainSampleRateHz: Int,
    val cueSampleRateHz: Int?,
    val musicVolume: Int?,
    val musicVolumeMax: Int?,
    val musicMuted: Boolean?,
    val voiceVolume: Int?,
    val voiceVolumeMax: Int?,
    val voiceMuted: Boolean?,
    val mainUnderruns: Int,
    val cueUnderruns: Int?,
    val mainPreferredAccepted: Boolean,
    val communicationSelectionActive: Boolean,
    val mainPreferredReassertAccepted: Boolean?,
    val communicationReassertAccepted: Boolean?,
) {
    val mainAdvanced: Boolean get() = mainPlaybackHeadEnd > mainPlaybackHeadStart
    val cueAdvanced: Boolean get() =
        cuePlaybackHeadStart != null && cuePlaybackHeadEnd != null && cuePlaybackHeadEnd > cuePlaybackHeadStart
}

data class CommunicationSplitProbeResult(
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val expectedMainPhysicalKey: String,
    val expectedCuePhysicalKey: String,
    val sessionSampleRateHz: Int,
    val cueSampleRateHz: Int,
    val phases: List<CommunicationSplitProbePhaseEvidence>,
    val completed: Boolean,
    val error: String? = null,
    val acousticOutcome: CommunicationSplitProbeAcousticOutcome? = null,
    val dPairOutcome: CommunicationSplitProbePairOutcome? = null,
    val ePairOutcome: CommunicationSplitProbePairOutcome? = null,
    val fPairOutcome: CommunicationSplitProbePairOutcome? = null,
) {
    fun diagnosticSummary(): String = buildString {
        append("completed=").append(completed)
        append("; expectedMain=").append(expectedMainPhysicalKey)
        append("; expectedCue=").append(expectedCuePhysicalKey)
        append("; sessionRate=").append(sessionSampleRateHz)
        append("; cueRate=").append(cueSampleRateHz)
        append("; phases=")
        append(phases.joinToString("|") { phase ->
            buildString {
                append(phase.phase.name)
                append("{mainRoutes=").append(phase.mainPhysicalKeys)
                append(",cueRoutes=").append(phase.cuePhysicalKeys)
                append(",comm=").append(phase.communicationPhysicalKey)
                append(",mainAdvanced=").append(phase.mainAdvanced)
                append(",cueAdvanced=").append(phase.cueAdvanced)
                append(",mainWritten=").append(phase.mainWrittenSamples)
                append(",cueWritten=").append(phase.cueWrittenSamples)
                append(",mainZeroWrites=").append(phase.mainZeroWrites)
                append(",cueZeroWrites=").append(phase.cueZeroWrites)
                append(",mainShortWrites=").append(phase.mainShortWrites)
                append(",cueShortWrites=").append(phase.cueShortWrites)
                append(",mainUnderruns=").append(phase.mainUnderruns)
                append(",cueUnderruns=").append(phase.cueUnderruns)
                append(",musicVol=").append(phase.musicVolume).append("/").append(phase.musicVolumeMax)
                append(",musicMuted=").append(phase.musicMuted)
                append(",voiceVol=").append(phase.voiceVolume).append("/").append(phase.voiceVolumeMax)
                append(",voiceMuted=").append(phase.voiceMuted)
                append(",mainReassert=").append(phase.mainPreferredReassertAccepted)
                append(",communicationReassert=").append(phase.communicationReassertAccepted)
                append("}")
            }
        })
        append("; acousticOutcome=").append(acousticOutcome?.name)
        append("; dPairOutcome=").append(dPairOutcome?.name)
        append("; ePairOutcome=").append(ePairOutcome?.name)
        append("; fPairOutcome=").append(fPairOutcome?.name)
        error?.let { append("; error=").append(it) }
    }
}

internal object CommunicationSplitProbeSignal {
    fun stereoTone(
        sampleRateHz: Int,
        frequencyHz: Double,
        startFrame: Long,
        frames: Int,
        amplitude: Float = 0.06f,
    ): FloatArray {
        require(sampleRateHz > 0)
        require(frequencyHz > 0.0)
        require(frames >= 0)
        require(amplitude in 0f..1f)
        val out = FloatArray(frames * 2)
        val angular = 2.0 * PI * frequencyHz / sampleRateHz.toDouble()
        for (frame in 0 until frames) {
            val value = (sin((startFrame + frame).toDouble() * angular) * amplitude).toFloat()
            out[frame * 2] = value
            out[frame * 2 + 1] = value
        }
        return out
    }
}

object CommunicationSplitPhaseProbe {
    @Volatile private var lastResult: CommunicationSplitProbeResult? = null

    fun lastResult(): CommunicationSplitProbeResult? = lastResult

    fun recordAcousticOutcome(outcome: CommunicationSplitProbeAcousticOutcome) {
        lastResult = lastResult?.copy(acousticOutcome = outcome)
    }

    fun recordPairOutcome(
        phase: CommunicationSplitProbePhase,
        outcome: CommunicationSplitProbePairOutcome,
    ) {
        lastResult = when (phase) {
            CommunicationSplitProbePhase.D_CUE_TONE_ACTIVE -> lastResult?.copy(dPairOutcome = outcome)
            CommunicationSplitProbePhase.E_MAIN_REASSERT_ONLY -> lastResult?.copy(ePairOutcome = outcome)
            CommunicationSplitProbePhase.F_COMMUNICATION_REASSERT -> lastResult?.copy(fPairOutcome = outcome)
            else -> lastResult
        }
    }

    fun run(
        audioManager: AudioManager,
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        profile: CueOutputProfile,
        keepRunning: () -> Boolean,
        onPhase: (CommunicationSplitProbePhase) -> Unit = {},
    ): CommunicationSplitProbeResult {
        lastResult = null
        require(profile.strategy == CueOutputStrategy.COMMUNICATION_SPLIT)
        val expectedMainKey = AndroidOutputRouteIdentity.physicalKey(main)
        val expectedCueKey = AndroidOutputRouteIdentity.physicalKey(cue)
        require(expectedMainKey != expectedCueKey)

        val startedAt = System.currentTimeMillis()
        val evidence = mutableListOf<CommunicationSplitProbePhaseEvidence>()
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        var communicationSession: CommunicationCueSession? = null
        var mainPreferredAccepted = false

        fun result(completed: Boolean, error: String? = null) =
            CommunicationSplitProbeResult(
                startedAtEpochMs = startedAt,
                completedAtEpochMs = System.currentTimeMillis(),
                expectedMainPhysicalKey = expectedMainKey,
                expectedCuePhysicalKey = expectedCueKey,
                sessionSampleRateHz = profile.mainSampleRateHz,
                cueSampleRateHz = profile.cueSampleRateHz,
                phases = evidence.toList(),
                completed = completed,
                error = error,
            ).also { lastResult = it }

        try {
            mainTrack = createTrack(profile.mainSampleRateHz, AudioAttributes.USAGE_MEDIA)
            mainPreferredAccepted = mainTrack.setPreferredDevice(main)
            check(mainPreferredAccepted) { "MAIN preferred route rejected" }
            mainTrack.play()

            onPhase(CommunicationSplitProbePhase.A_MAIN_ONLY)
            evidence += runPhase(
                phase = CommunicationSplitProbePhase.A_MAIN_ONLY,
                audioManager = audioManager,
                mainTrack = mainTrack,
                cueTrack = null,
                mainPreferredAccepted = mainPreferredAccepted,
                communicationSelectionActive = false,
                cueToneActive = false,
                keepRunning = keepRunning,
            )

            check(keepRunning()) { "Probe cancelled" }
            communicationSession = AndroidCommunicationCueRouting.begin(audioManager, cue, modeRequired = false)
                ?: error("Communication device selection rejected")

            onPhase(CommunicationSplitProbePhase.B_COMMUNICATION_DEVICE_SELECTED)
            evidence += runPhase(
                phase = CommunicationSplitProbePhase.B_COMMUNICATION_DEVICE_SELECTED,
                audioManager = audioManager,
                mainTrack = mainTrack,
                cueTrack = null,
                mainPreferredAccepted = mainPreferredAccepted,
                communicationSelectionActive = true,
                cueToneActive = false,
                keepRunning = keepRunning,
            )

            check(keepRunning()) { "Probe cancelled" }
            cueTrack = createTrack(profile.cueSampleRateHz, AudioAttributes.USAGE_VOICE_COMMUNICATION)
            // Do not call communicationSession.reassert() here. B already selected CUE; C must
            // isolate opening/playing the communication AudioTrack itself.
            cueTrack.play()

            onPhase(CommunicationSplitProbePhase.C_CUE_TRACK_SILENT)
            evidence += runPhase(
                phase = CommunicationSplitProbePhase.C_CUE_TRACK_SILENT,
                audioManager = audioManager,
                mainTrack = mainTrack,
                cueTrack = cueTrack,
                mainPreferredAccepted = mainPreferredAccepted,
                communicationSelectionActive = true,
                cueToneActive = false,
                keepRunning = keepRunning,
            )

            check(keepRunning()) { "Probe cancelled" }
            onPhase(CommunicationSplitProbePhase.D_CUE_TONE_ACTIVE)
            evidence += runPhase(
                phase = CommunicationSplitProbePhase.D_CUE_TONE_ACTIVE,
                audioManager = audioManager,
                mainTrack = mainTrack,
                cueTrack = cueTrack,
                mainPreferredAccepted = mainPreferredAccepted,
                communicationSelectionActive = true,
                cueToneActive = true,
                keepRunning = keepRunning,
            )

            check(keepRunning()) { "Probe cancelled" }
            val mainReassertAccepted =
                runCatching { mainTrack.setPreferredDevice(main) }.getOrDefault(false)
            onPhase(CommunicationSplitProbePhase.E_MAIN_REASSERT_ONLY)
            evidence += runPhase(
                phase = CommunicationSplitProbePhase.E_MAIN_REASSERT_ONLY,
                audioManager = audioManager,
                mainTrack = mainTrack,
                cueTrack = cueTrack,
                mainPreferredAccepted = mainPreferredAccepted,
                communicationSelectionActive = true,
                cueToneActive = true,
                keepRunning = keepRunning,
                mainPreferredReassertAccepted = mainReassertAccepted,
            )

            check(keepRunning()) { "Probe cancelled" }
            val communicationReassertAccepted = communicationSession.reassert()
            onPhase(CommunicationSplitProbePhase.F_COMMUNICATION_REASSERT)
            evidence += runPhase(
                phase = CommunicationSplitProbePhase.F_COMMUNICATION_REASSERT,
                audioManager = audioManager,
                mainTrack = mainTrack,
                cueTrack = cueTrack,
                mainPreferredAccepted = mainPreferredAccepted,
                communicationSelectionActive = true,
                cueToneActive = true,
                keepRunning = keepRunning,
                communicationReassertAccepted = communicationReassertAccepted,
            )

            return result(completed = true)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            return result(completed = false, error = "CANCELLED")
        } catch (error: Throwable) {
            return result(completed = false, error = (error.message ?: error::class.java.simpleName).take(512))
        } finally {
            runCatching { cueTrack?.pause() }
            runCatching { cueTrack?.flush() }
            runCatching { cueTrack?.release() }
            communicationSession?.close()
            runCatching { mainTrack?.pause() }
            runCatching { mainTrack?.flush() }
            runCatching { mainTrack?.release() }
        }
    }

    private fun runPhase(
        phase: CommunicationSplitProbePhase,
        audioManager: AudioManager,
        mainTrack: AudioTrack,
        cueTrack: AudioTrack?,
        mainPreferredAccepted: Boolean,
        communicationSelectionActive: Boolean,
        cueToneActive: Boolean,
        keepRunning: () -> Boolean,
        mainPreferredReassertAccepted: Boolean? = null,
        communicationReassertAccepted: Boolean? = null,
    ): CommunicationSplitProbePhaseEvidence {
        val startedNs = System.nanoTime()
        val durationNs = if (
            phase == CommunicationSplitProbePhase.E_MAIN_REASSERT_ONLY ||
            phase == CommunicationSplitProbePhase.F_COMMUNICATION_REASSERT
        ) {
            RECOVERY_PHASE_DURATION_NS
        } else {
            PHASE_DURATION_NS
        }
        val deadlineNs = startedNs + durationNs
        val mainHeadStart = playbackHead(mainTrack)
        val cueHeadStart = cueTrack?.let(::playbackHead)
        val mainUnderrunStart = mainTrack.underrunCount
        val cueUnderrunStart = cueTrack?.underrunCount
        var mainWrittenSamples = 0L
        var cueWrittenSamples = 0L
        var mainZeroWrites = 0
        var cueZeroWrites = 0
        var mainShortWrites = 0
        var cueShortWrites = 0
        var mainFrameCursor = 0L
        var cueFrameCursor = 0L
        val silence = cueTrack?.let { FloatArray(CHUNK_FRAMES * 2) }

        while (keepRunning() && System.nanoTime() < deadlineNs) {
            val mainTone = CommunicationSplitProbeSignal.stereoTone(
                mainTrack.sampleRate,
                MAIN_TONE_HZ,
                mainFrameCursor,
                CHUNK_FRAMES,
            )
            val mainWrite = mainTrack.write(mainTone, 0, mainTone.size, AudioTrack.WRITE_NON_BLOCKING)
            check(mainWrite >= 0) { "MAIN write failed: $mainWrite" }
            if (mainWrite == 0) mainZeroWrites += 1
            if (mainWrite in 1 until mainTone.size) mainShortWrites += 1
            mainWrittenSamples += mainWrite
            mainFrameCursor += mainWrite / 2L

            cueTrack?.let { track ->
                val cueBuffer = if (cueToneActive) {
                    CommunicationSplitProbeSignal.stereoTone(
                        track.sampleRate,
                        CUE_TONE_HZ,
                        cueFrameCursor,
                        CHUNK_FRAMES,
                    )
                } else {
                    silence!!
                }
                val cueWrite = track.write(cueBuffer, 0, cueBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
                check(cueWrite >= 0) { "CUE write failed: $cueWrite" }
                if (cueWrite == 0) cueZeroWrites += 1
                if (cueWrite in 1 until cueBuffer.size) cueShortWrites += 1
                cueWrittenSamples += cueWrite
                cueFrameCursor += cueWrite / 2L
            }
            Thread.sleep(4L)
        }
        check(keepRunning()) { "Probe cancelled" }

        val currentCommunication = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.communicationDevice }.getOrNull()
        } else {
            null
        }
        return CommunicationSplitProbePhaseEvidence(
            phase = phase,
            elapsedMs = (System.nanoTime() - startedNs) / 1_000_000L,
            mainPhysicalKeys = AndroidOutputRouteIdentity.routedPhysicalKeys(mainTrack).sorted(),
            cuePhysicalKeys = cueTrack?.let(AndroidOutputRouteIdentity::routedPhysicalKeys)?.sorted().orEmpty(),
            communicationPhysicalKey = currentCommunication?.let(AndroidOutputRouteIdentity::physicalKey),
            audioMode = audioManager.mode,
            mainPlaybackHeadStart = mainHeadStart,
            mainPlaybackHeadEnd = playbackHead(mainTrack),
            cuePlaybackHeadStart = cueHeadStart,
            cuePlaybackHeadEnd = cueTrack?.let(::playbackHead),
            mainWrittenSamples = mainWrittenSamples,
            cueWrittenSamples = cueWrittenSamples,
            mainZeroWrites = mainZeroWrites,
            cueZeroWrites = cueZeroWrites,
            mainShortWrites = mainShortWrites,
            cueShortWrites = cueShortWrites,
            mainSampleRateHz = mainTrack.sampleRate,
            cueSampleRateHz = cueTrack?.sampleRate,
            musicVolume = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrNull(),
            musicVolumeMax = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrNull(),
            musicMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_MUSIC) }.getOrNull(),
            voiceVolume = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) }.getOrNull(),
            voiceVolumeMax = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) }.getOrNull(),
            voiceMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_VOICE_CALL) }.getOrNull(),
            mainUnderruns = (mainTrack.underrunCount - mainUnderrunStart).coerceAtLeast(0),
            cueUnderruns = cueTrack?.let { track ->
                val start = cueUnderrunStart ?: track.underrunCount
                (track.underrunCount - start).coerceAtLeast(0)
            },
            mainPreferredAccepted = mainPreferredAccepted,
            communicationSelectionActive = communicationSelectionActive,
            mainPreferredReassertAccepted = mainPreferredReassertAccepted,
            communicationReassertAccepted = communicationReassertAccepted,
        )
    }

    private fun createTrack(sampleRateHz: Int, usage: Int): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferSize > 0)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(32_768, minBufferSize))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
            .also { track ->
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    error("AudioTrack not initialized")
                }
            }
    }

    private fun playbackHead(track: AudioTrack): Long =
        track.playbackHeadPosition.toLong() and 0xffffffffL

    private const val CHUNK_FRAMES = 256
    private const val PHASE_DURATION_NS = 1_500_000_000L
    private const val RECOVERY_PHASE_DURATION_NS = 2_200_000_000L
    private const val MAIN_TONE_HZ = 440.0
    private const val CUE_TONE_HZ = 880.0
}
