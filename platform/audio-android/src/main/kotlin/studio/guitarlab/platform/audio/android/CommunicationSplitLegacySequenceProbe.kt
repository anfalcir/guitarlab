package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin

enum class CommunicationSplitLegacyScenario {
    G_COMMUNICATION_BEFORE_OPEN,
    H_MEDIA_PRECONDITION_THEN_COMMUNICATION,
}

internal object CommunicationSplitLegacySequencePolicy {
    const val PRECONDITION_MS = 900L
    const val MUTED_COMMUNICATION_WARMUP_MS = 650L
    const val AUDIBLE_WINDOW_MS = 2_400L
    const val SETTLE_BETWEEN_SCENARIOS_MS = 350L
    const val CHUNK_FRAMES = 256
    const val MAIN_TONE_HZ = 440.0
    const val CUE_TONE_HZ = 880.0
    const val AMPLITUDE = 0.06f

    fun requiresMediaPrecondition(scenario: CommunicationSplitLegacyScenario): Boolean =
        scenario == CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION
}

data class CommunicationSplitLegacyPreconditionEvidence(
    val attempted: Boolean,
    val mainPreferredAccepted: Boolean?,
    val cuePreferredAccepted: Boolean?,
    val mainPreferredReasserted: Boolean?,
    val cuePreferredReasserted: Boolean?,
    val mainPhysicalKeys: List<String>,
    val cuePhysicalKeys: List<String>,
    val mainAdvanced: Boolean,
    val cueAdvanced: Boolean,
    val mainWrittenSamples: Long,
    val cueWrittenSamples: Long,
    val mainZeroWrites: Int,
    val cueZeroWrites: Int,
    val error: String?,
)

data class CommunicationSplitLegacyScenarioEvidence(
    val scenario: CommunicationSplitLegacyScenario,
    val completed: Boolean,
    val error: String?,
    val precondition: CommunicationSplitLegacyPreconditionEvidence?,
    val communicationSelectedBeforeTracks: Boolean,
    val communicationPhysicalKey: String?,
    val mainPreferredAccepted: Boolean,
    val mainPreferredReassertedAfterPlay: Boolean,
    val communicationReassertedAfterPlay: Boolean,
    val mainPhysicalKeys: List<String>,
    val cuePhysicalKeys: List<String>,
    val mainPlaybackHeadStart: Long,
    val mainPlaybackHeadEnd: Long,
    val cuePlaybackHeadStart: Long,
    val cuePlaybackHeadEnd: Long,
    val mainWrittenSamples: Long,
    val cueWrittenSamples: Long,
    val mainZeroWrites: Int,
    val cueZeroWrites: Int,
    val mainShortWrites: Int,
    val cueShortWrites: Int,
    val mainUnderruns: Int,
    val cueUnderruns: Int,
    val musicVolume: Int?,
    val musicVolumeMax: Int?,
    val musicMuted: Boolean?,
    val voiceVolume: Int?,
    val voiceVolumeMax: Int?,
    val voiceMuted: Boolean?,
)

data class CommunicationSplitLegacySequenceResult(
    val expectedMainPhysicalKey: String,
    val expectedCuePhysicalKey: String,
    val sessionSampleRateHz: Int,
    val cueSampleRateHz: Int,
    val gEvidence: CommunicationSplitLegacyScenarioEvidence? = null,
    val hEvidence: CommunicationSplitLegacyScenarioEvidence? = null,
    val gPairOutcome: CommunicationSplitProbePairOutcome? = null,
    val hPairOutcome: CommunicationSplitProbePairOutcome? = null,
) {
    fun diagnosticSummary(): String = buildString {
        append("expectedMain=").append(expectedMainPhysicalKey)
        append("; expectedCue=").append(expectedCuePhysicalKey)
        append("; sessionRate=").append(sessionSampleRateHz)
        append("; cueRate=").append(cueSampleRateHz)
        append("; G=").append(gEvidence?.summary())
        append("; H=").append(hEvidence?.summary())
        append("; gPairOutcome=").append(gPairOutcome?.name)
        append("; hPairOutcome=").append(hPairOutcome?.name)
    }

    private fun CommunicationSplitLegacyScenarioEvidence.summary(): String = buildString {
        append(scenario.name)
        append("{completed=").append(completed)
        append(",error=").append(error)
        append(",precondition=").append(precondition?.let {
            "attempted=${it.attempted},mainRoutes=${it.mainPhysicalKeys},cueRoutes=${it.cuePhysicalKeys}," +
                "mainAdvanced=${it.mainAdvanced},cueAdvanced=${it.cueAdvanced},error=${it.error}"
        })
        append(",commBeforeTracks=").append(communicationSelectedBeforeTracks)
        append(",comm=").append(communicationPhysicalKey)
        append(",mainPreferred=").append(mainPreferredAccepted)
        append(",mainReassert=").append(mainPreferredReassertedAfterPlay)
        append(",commReassert=").append(communicationReassertedAfterPlay)
        append(",mainRoutes=").append(mainPhysicalKeys)
        append(",cueRoutes=").append(cuePhysicalKeys)
        append(",mainAdvanced=").append(mainPlaybackHeadEnd > mainPlaybackHeadStart)
        append(",cueAdvanced=").append(cuePlaybackHeadEnd > cuePlaybackHeadStart)
        append(",mainWritten=").append(mainWrittenSamples)
        append(",cueWritten=").append(cueWrittenSamples)
        append(",mainZero=").append(mainZeroWrites)
        append(",cueZero=").append(cueZeroWrites)
        append(",mainShort=").append(mainShortWrites)
        append(",cueShort=").append(cueShortWrites)
        append(",mainUnderruns=").append(mainUnderruns)
        append(",cueUnderruns=").append(cueUnderruns)
        append(",musicVol=").append(musicVolume).append('/').append(musicVolumeMax)
        append(",musicMuted=").append(musicMuted)
        append(",voiceVol=").append(voiceVolume).append('/').append(voiceVolumeMax)
        append(",voiceMuted=").append(voiceMuted)
        append('}')
    }
}

object CommunicationSplitLegacySequenceProbe {
    @Volatile private var lastResult: CommunicationSplitLegacySequenceResult? = null

    fun lastResult(): CommunicationSplitLegacySequenceResult? = lastResult

    fun recordPairOutcome(
        scenario: CommunicationSplitLegacyScenario,
        outcome: CommunicationSplitProbePairOutcome,
    ) {
        lastResult = when (scenario) {
            CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN ->
                lastResult?.copy(gPairOutcome = outcome)
            CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION ->
                lastResult?.copy(hPairOutcome = outcome)
        }
    }

    fun runScenario(
        audioManager: AudioManager,
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        profile: CueOutputProfile,
        scenario: CommunicationSplitLegacyScenario,
        keepRunning: () -> Boolean,
        onMessage: (String) -> Unit = {},
    ): CommunicationSplitLegacyScenarioEvidence {
        require(profile.strategy == CueOutputStrategy.COMMUNICATION_SPLIT)
        val expectedMain = AndroidOutputRouteIdentity.physicalKey(main)
        val expectedCue = AndroidOutputRouteIdentity.physicalKey(cue)
        require(expectedMain != expectedCue)

        if (
            scenario == CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN ||
            lastResult?.expectedMainPhysicalKey != expectedMain ||
            lastResult?.expectedCuePhysicalKey != expectedCue ||
            lastResult?.sessionSampleRateHz != profile.mainSampleRateHz ||
            lastResult?.cueSampleRateHz != profile.cueSampleRateHz
        ) {
            lastResult = CommunicationSplitLegacySequenceResult(
                expectedMainPhysicalKey = expectedMain,
                expectedCuePhysicalKey = expectedCue,
                sessionSampleRateHz = profile.mainSampleRateHz,
                cueSampleRateHz = profile.cueSampleRateHz,
            )
        }

        val precondition = if (CommunicationSplitLegacySequencePolicy.requiresMediaPrecondition(scenario)) {
            onMessage("H · Pré-condicionando AudioPolicy com a sequência dual-MEDIA do rc31…")
            runMediaPrecondition(
                main = main,
                cue = cue,
                mainSampleRateHz = profile.mainSampleRateHz,
                cueSampleRateHz = profile.cueSampleRateHz,
                keepRunning = keepRunning,
            )
        } else null

        if (!keepRunning()) throw InterruptedException("Probe cancelled")
        if (precondition != null) {
            Thread.sleep(CommunicationSplitLegacySequencePolicy.SETTLE_BETWEEN_SCENARIOS_MS)
        }

        onMessage(
            if (scenario == CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN) {
                "G · Communication/CUE primeiro; depois abrindo os dois tracks…"
            } else {
                "H · Após media-first, repetindo communication-before-open…"
            },
        )
        val evidence = runCommunicationBeforeOpen(
            audioManager = audioManager,
            main = main,
            cue = cue,
            mainSampleRateHz = profile.mainSampleRateHz,
            cueSampleRateHz = profile.cueSampleRateHz,
            precondition = precondition,
            keepRunning = keepRunning,
            scenario = scenario,
            onAudibleWindow = {
                onMessage(
                    if (scenario == CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN) {
                        "G · Ouça agora: grave=MAIN, agudo=CUE."
                    } else {
                        "H · Ouça agora após media-first: grave=MAIN, agudo=CUE."
                    },
                )
            },
        )

        lastResult = when (scenario) {
            CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN ->
                requireNotNull(lastResult).copy(gEvidence = evidence)
            CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION ->
                requireNotNull(lastResult).copy(hEvidence = evidence)
        }
        return evidence
    }

    private fun runMediaPrecondition(
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        mainSampleRateHz: Int,
        cueSampleRateHz: Int,
        keepRunning: () -> Boolean,
    ): CommunicationSplitLegacyPreconditionEvidence {
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        var mainAccepted: Boolean? = null
        var cueAccepted: Boolean? = null
        var mainReasserted: Boolean? = null
        var cueReasserted: Boolean? = null
        var mainWritten = 0L
        var cueWritten = 0L
        var mainZero = 0
        var cueZero = 0
        var mainHeadStart = 0L
        var cueHeadStart = 0L

        return try {
            mainTrack = createTrack(mainSampleRateHz, AudioAttributes.USAGE_MEDIA)
            cueTrack = createTrack(cueSampleRateHz, AudioAttributes.USAGE_MEDIA)
            mainTrack.setVolume(0f)
            cueTrack.setVolume(0f)
            mainAccepted = mainTrack.setPreferredDevice(main)
            cueAccepted = cueTrack.setPreferredDevice(cue)
            val mainSilence = FloatArray(512 * 2)
            val cueSilence = FloatArray(512 * 2)
            repeat(8) {
                val mw = mainTrack.write(mainSilence, 0, mainSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                val cw = cueTrack.write(cueSilence, 0, cueSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                check(mw >= 0 && cw >= 0) { "media precondition write failed" }
                mainWritten += mw
                cueWritten += cw
                if (mw == 0) mainZero += 1
                if (cw == 0) cueZero += 1
            }
            mainTrack.play()
            cueTrack.play()
            mainReasserted = mainTrack.setPreferredDevice(main)
            cueReasserted = cueTrack.setPreferredDevice(cue)
            mainHeadStart = playbackHead(mainTrack)
            cueHeadStart = playbackHead(cueTrack)
            val deadline = System.nanoTime() +
                CommunicationSplitLegacySequencePolicy.PRECONDITION_MS * 1_000_000L
            while (keepRunning() && System.nanoTime() < deadline) {
                val mw = mainTrack.write(mainSilence, 0, mainSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                val cw = cueTrack.write(cueSilence, 0, cueSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                check(mw >= 0 && cw >= 0) { "media precondition write failed" }
                mainWritten += mw
                cueWritten += cw
                if (mw == 0) mainZero += 1
                if (cw == 0) cueZero += 1
                Thread.sleep(4L)
            }
            if (!keepRunning()) throw InterruptedException("Probe cancelled")
            CommunicationSplitLegacyPreconditionEvidence(
                attempted = true,
                mainPreferredAccepted = mainAccepted,
                cuePreferredAccepted = cueAccepted,
                mainPreferredReasserted = mainReasserted,
                cuePreferredReasserted = cueReasserted,
                mainPhysicalKeys = AndroidOutputRouteIdentity.routedPhysicalKeys(mainTrack).sorted(),
                cuePhysicalKeys = AndroidOutputRouteIdentity.routedPhysicalKeys(cueTrack).sorted(),
                mainAdvanced = playbackHead(mainTrack) > mainHeadStart,
                cueAdvanced = playbackHead(cueTrack) > cueHeadStart,
                mainWrittenSamples = mainWritten,
                cueWrittenSamples = cueWritten,
                mainZeroWrites = mainZero,
                cueZeroWrites = cueZero,
                error = null,
            )
        } catch (cancelled: InterruptedException) {
            Thread.currentThread().interrupt()
            throw cancelled
        } catch (failure: Throwable) {
            CommunicationSplitLegacyPreconditionEvidence(
                attempted = true,
                mainPreferredAccepted = mainAccepted,
                cuePreferredAccepted = cueAccepted,
                mainPreferredReasserted = mainReasserted,
                cuePreferredReasserted = cueReasserted,
                mainPhysicalKeys = mainTrack?.let(AndroidOutputRouteIdentity::routedPhysicalKeys)?.sorted().orEmpty(),
                cuePhysicalKeys = cueTrack?.let(AndroidOutputRouteIdentity::routedPhysicalKeys)?.sorted().orEmpty(),
                mainAdvanced = mainTrack?.let { playbackHead(it) > mainHeadStart } == true,
                cueAdvanced = cueTrack?.let { playbackHead(it) > cueHeadStart } == true,
                mainWrittenSamples = mainWritten,
                cueWrittenSamples = cueWritten,
                mainZeroWrites = mainZero,
                cueZeroWrites = cueZero,
                error = failure.message ?: failure::class.java.simpleName,
            )
        } finally {
            releaseTrack(cueTrack)
            releaseTrack(mainTrack)
        }
    }

    private fun runCommunicationBeforeOpen(
        audioManager: AudioManager,
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        mainSampleRateHz: Int,
        cueSampleRateHz: Int,
        precondition: CommunicationSplitLegacyPreconditionEvidence?,
        keepRunning: () -> Boolean,
        scenario: CommunicationSplitLegacyScenario,
        onAudibleWindow: () -> Unit,
    ): CommunicationSplitLegacyScenarioEvidence {
        var session: CommunicationCueSession? = null
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        var mainAccepted = false
        var mainReasserted = false
        var communicationReasserted = false
        var mainWritten = 0L
        var cueWritten = 0L
        var mainZero = 0
        var cueZero = 0
        var mainShort = 0
        var cueShort = 0
        var mainHeadStart = 0L
        var cueHeadStart = 0L
        var mainUnderrunStart = 0
        var cueUnderrunStart = 0

        try {
            session = AndroidCommunicationCueRouting.begin(audioManager, cue, modeRequired = false)
                ?: error("Communication device selection rejected before track creation")
            mainTrack = createTrack(mainSampleRateHz, AudioAttributes.USAGE_MEDIA)
            cueTrack = createTrack(cueSampleRateHz, AudioAttributes.USAGE_VOICE_COMMUNICATION)
            mainTrack.setVolume(0f)
            cueTrack.setVolume(0f)
            mainAccepted = mainTrack.setPreferredDevice(main)
            check(mainAccepted) { "MAIN preferred route rejected" }

            val mainSilence = FloatArray(512 * 2)
            val cueSilence = FloatArray(512 * 2)
            repeat(8) {
                val mw = mainTrack.write(mainSilence, 0, mainSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                val cw = cueTrack.write(cueSilence, 0, cueSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                check(mw >= 0 && cw >= 0) { "legacy communication prime write failed" }
                mainWritten += mw
                cueWritten += cw
                if (mw == 0) mainZero += 1
                if (cw == 0) cueZero += 1
                if (mw in 1 until mainSilence.size) mainShort += 1
                if (cw in 1 until cueSilence.size) cueShort += 1
            }

            mainTrack.play()
            cueTrack.play()
            mainReasserted = mainTrack.setPreferredDevice(main)
            communicationReasserted = session.reassert()
            check(mainReasserted && communicationReasserted) {
                "legacy post-play route reassertion rejected"
            }

            val warmupDeadline = System.nanoTime() +
                CommunicationSplitLegacySequencePolicy.MUTED_COMMUNICATION_WARMUP_MS * 1_000_000L
            while (keepRunning() && System.nanoTime() < warmupDeadline) {
                val mw = mainTrack.write(mainSilence, 0, mainSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                val cw = cueTrack.write(cueSilence, 0, cueSilence.size, AudioTrack.WRITE_NON_BLOCKING)
                check(mw >= 0 && cw >= 0) { "legacy warmup write failed" }
                mainWritten += mw
                cueWritten += cw
                if (mw == 0) mainZero += 1
                if (cw == 0) cueZero += 1
                if (mw in 1 until mainSilence.size) mainShort += 1
                if (cw in 1 until cueSilence.size) cueShort += 1
                Thread.sleep(4L)
            }
            if (!keepRunning()) throw InterruptedException("Probe cancelled")

            mainTrack.setVolume(1f)
            cueTrack.setVolume(1f)
            onAudibleWindow()
            mainHeadStart = playbackHead(mainTrack)
            cueHeadStart = playbackHead(cueTrack)
            mainUnderrunStart = mainTrack.underrunCount
            cueUnderrunStart = cueTrack.underrunCount

            var mainCursor = 0L
            var cueCursor = 0L
            val audibleDeadline = System.nanoTime() +
                CommunicationSplitLegacySequencePolicy.AUDIBLE_WINDOW_MS * 1_000_000L
            while (keepRunning() && System.nanoTime() < audibleDeadline) {
                val mainTone = tone(
                    mainTrack.sampleRate,
                    CommunicationSplitLegacySequencePolicy.MAIN_TONE_HZ,
                    mainCursor,
                )
                val cueTone = tone(
                    cueTrack.sampleRate,
                    CommunicationSplitLegacySequencePolicy.CUE_TONE_HZ,
                    cueCursor,
                )
                val mw = mainTrack.write(mainTone, 0, mainTone.size, AudioTrack.WRITE_NON_BLOCKING)
                val cw = cueTrack.write(cueTone, 0, cueTone.size, AudioTrack.WRITE_NON_BLOCKING)
                check(mw >= 0 && cw >= 0) { "legacy audible write failed" }
                mainWritten += mw
                cueWritten += cw
                if (mw == 0) mainZero += 1
                if (cw == 0) cueZero += 1
                if (mw in 1 until mainTone.size) mainShort += 1
                if (cw in 1 until cueTone.size) cueShort += 1
                mainCursor += mw / 2L
                cueCursor += cw / 2L
                Thread.sleep(4L)
            }
            if (!keepRunning()) throw InterruptedException("Probe cancelled")

            val communicationKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { audioManager.communicationDevice }
                    .getOrNull()
                    ?.let(AndroidOutputRouteIdentity::physicalKey)
            } else null

            return evidence(
                scenario, true, null, precondition, session, communicationKey,
                mainTrack, cueTrack, mainAccepted, mainReasserted, communicationReasserted,
                mainHeadStart, cueHeadStart, mainWritten, cueWritten, mainZero, cueZero,
                mainShort, cueShort, mainUnderrunStart, cueUnderrunStart, audioManager,
            )
        } catch (cancelled: InterruptedException) {
            Thread.currentThread().interrupt()
            throw cancelled
        } catch (failure: Throwable) {
            return evidence(
                scenario, false, failure.message ?: failure::class.java.simpleName, precondition,
                session, session?.device?.let(AndroidOutputRouteIdentity::physicalKey),
                mainTrack, cueTrack, mainAccepted, mainReasserted, communicationReasserted,
                mainHeadStart, cueHeadStart, mainWritten, cueWritten, mainZero, cueZero,
                mainShort, cueShort, mainUnderrunStart, cueUnderrunStart, audioManager,
            )
        } finally {
            releaseTrack(cueTrack)
            releaseTrack(mainTrack)
            session?.close()
        }
    }

    private fun evidence(
        scenario: CommunicationSplitLegacyScenario,
        completed: Boolean,
        error: String?,
        precondition: CommunicationSplitLegacyPreconditionEvidence?,
        session: CommunicationCueSession?,
        communicationKey: String?,
        mainTrack: AudioTrack?,
        cueTrack: AudioTrack?,
        mainAccepted: Boolean,
        mainReasserted: Boolean,
        communicationReasserted: Boolean,
        mainHeadStart: Long,
        cueHeadStart: Long,
        mainWritten: Long,
        cueWritten: Long,
        mainZero: Int,
        cueZero: Int,
        mainShort: Int,
        cueShort: Int,
        mainUnderrunStart: Int,
        cueUnderrunStart: Int,
        audioManager: AudioManager,
    ) = CommunicationSplitLegacyScenarioEvidence(
        scenario = scenario,
        completed = completed,
        error = error,
        precondition = precondition,
        communicationSelectedBeforeTracks = session != null,
        communicationPhysicalKey = communicationKey,
        mainPreferredAccepted = mainAccepted,
        mainPreferredReassertedAfterPlay = mainReasserted,
        communicationReassertedAfterPlay = communicationReasserted,
        mainPhysicalKeys = mainTrack?.let(AndroidOutputRouteIdentity::routedPhysicalKeys)?.sorted().orEmpty(),
        cuePhysicalKeys = cueTrack?.let(AndroidOutputRouteIdentity::routedPhysicalKeys)?.sorted().orEmpty(),
        mainPlaybackHeadStart = mainHeadStart,
        mainPlaybackHeadEnd = mainTrack?.let(::playbackHead) ?: 0L,
        cuePlaybackHeadStart = cueHeadStart,
        cuePlaybackHeadEnd = cueTrack?.let(::playbackHead) ?: 0L,
        mainWrittenSamples = mainWritten,
        cueWrittenSamples = cueWritten,
        mainZeroWrites = mainZero,
        cueZeroWrites = cueZero,
        mainShortWrites = mainShort,
        cueShortWrites = cueShort,
        mainUnderruns = mainTrack?.let { (it.underrunCount - mainUnderrunStart).coerceAtLeast(0) } ?: 0,
        cueUnderruns = cueTrack?.let { (it.underrunCount - cueUnderrunStart).coerceAtLeast(0) } ?: 0,
        musicVolume = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrNull(),
        musicVolumeMax = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrNull(),
        musicMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_MUSIC) }.getOrNull(),
        voiceVolume = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) }.getOrNull(),
        voiceVolumeMax = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) }.getOrNull(),
        voiceMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_VOICE_CALL) }.getOrNull(),
    )

    private fun createTrack(sampleRateHz: Int, usage: Int): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBuffer > 0)
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
            .setBufferSizeInBytes(maxOf(32_768, minBuffer))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
            .also { track ->
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    error("AudioTrack not initialized")
                }
            }
    }

    private fun releaseTrack(track: AudioTrack?) {
        if (track == null) return
        runCatching { track.pause() }
        runCatching { track.flush() }
        runCatching { track.release() }
    }

    private fun playbackHead(track: AudioTrack): Long =
        track.playbackHeadPosition.toLong() and 0xffffffffL

    private fun tone(sampleRateHz: Int, frequencyHz: Double, startFrame: Long): FloatArray {
        val frames = CommunicationSplitLegacySequencePolicy.CHUNK_FRAMES
        val out = FloatArray(frames * 2)
        val step = 2.0 * PI * frequencyHz / sampleRateHz.toDouble()
        repeat(frames) { frame ->
            val sample = (
                sin((startFrame + frame).toDouble() * step) *
                    CommunicationSplitLegacySequencePolicy.AMPLITUDE
                ).toFloat()
            out[frame * 2] = sample
            out[frame * 2 + 1] = sample
        }
        return out
    }
}
