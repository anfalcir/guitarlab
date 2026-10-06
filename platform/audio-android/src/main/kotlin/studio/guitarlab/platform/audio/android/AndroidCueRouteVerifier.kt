package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTimestamp
import android.os.Build
import java.util.concurrent.ConcurrentHashMap
import studio.guitarlab.core.audio.AudioClockObservation
import studio.guitarlab.core.audio.CueProbeOutput
import studio.guitarlab.core.audio.CueRouteObservation
import studio.guitarlab.core.audio.CueStartupFailure
import studio.guitarlab.core.audio.CueStartupProbe

enum class CuePreflightStatus {
    SUPPORTED,
    EXPECTED_PAIR_NOT_DISTINCT,
    CANCELLED,
    OPEN_FAILED,
    WRITE_FAILED,
    PREFERRED_ROUTE_REJECTED,
    MISSING_EFFECTIVE_ROUTE,
    CONVERGED_TO_MAIN,
    WRONG_OR_MIRRORED_ROUTE,
    ROUTE_CHANGED,
    CLOCK_UNSTABLE,
    OFFSET_EXCEEDED,
    VERIFICATION_FAILED,
}

data class CueTrackConfigurationEvidence(
    val state: Int,
    val sampleRateHz: Int,
    val channelCount: Int,
    val encoding: Int,
    val bufferCapacityFrames: Int,
    val bufferSizeFrames: Int,
    val startThresholdFrames: Int?,
    val performanceMode: Int,
)

data class CueRouteTraceSample(
    val elapsedMs: Long,
    val mainWriteResult: Int,
    val cueWriteResult: Int,
    val mainPhysicalKeys: Set<String>,
    val cuePhysicalKeys: Set<String>,
)

data class CuePreflightEvidence(
    val sampleRateHz: Int,
    val expectedMainPhysicalKey: String,
    val expectedCuePhysicalKey: String,
    val mainAdvertisedSampleRates: List<Int>,
    val cueAdvertisedSampleRates: List<Int>,
    val mainAdvertisedChannelCounts: List<Int>,
    val cueAdvertisedChannelCounts: List<Int>,
    val mainPreferredAccepted: Boolean,
    val cuePreferredAccepted: Boolean,
    val mainPreferredReassertedAfterPlay: Boolean?,
    val cuePreferredReassertedAfterPlay: Boolean?,
    val mainTrack: CueTrackConfigurationEvidence?,
    val cueTrack: CueTrackConfigurationEvidence?,
    val routedTransitions: List<CueRouteTraceSample>,
    val attemptedCueSampleRates: List<Int> = emptyList(),
    val negotiatedCueSampleRateHz: Int? = null,
    val cueResamplingRequired: Boolean = false,
)

data class CuePreflightResult(
    val status: CuePreflightStatus,
    val userMessage: String,
    val evidence: CuePreflightEvidence,
) {
    val supported: Boolean get() = status == CuePreflightStatus.SUPPORTED

    fun diagnosticSummary(): String = buildString {
        append("status=").append(status.name)
        append("; sampleRateHz=").append(evidence.sampleRateHz)
        append("; expectedMain=").append(evidence.expectedMainPhysicalKey)
        append("; expectedCue=").append(evidence.expectedCuePhysicalKey)
        append("; preferredAccepted=MAIN:").append(evidence.mainPreferredAccepted)
        append(",CUE:").append(evidence.cuePreferredAccepted)
        append("; preferredReassertedAfterPlay=MAIN:").append(evidence.mainPreferredReassertedAfterPlay)
        append(",CUE:").append(evidence.cuePreferredReassertedAfterPlay)
        append("; attemptedCueSampleRates=").append(evidence.attemptedCueSampleRates)
        append("; negotiatedCueSampleRateHz=").append(evidence.negotiatedCueSampleRateHz)
        append("; cueResamplingRequired=").append(evidence.cueResamplingRequired)
        if (evidence.routedTransitions.isNotEmpty()) {
            append("; routed=")
            append(evidence.routedTransitions.joinToString(" -> ") {
                "${it.elapsedMs}ms:MAIN=${it.mainPhysicalKeys.sorted()},CUE=${it.cuePhysicalKeys.sorted()}"
            })
        }
    }
}

internal class CueRouteTraceBuffer(private val capacity: Int = 64) {
    private val transitions = ArrayDeque<CueRouteTraceSample>()

    init { require(capacity > 0) }

    fun record(sample: CueRouteTraceSample) {
        if (transitions.lastOrNull()?.let {
                it.mainPhysicalKeys == sample.mainPhysicalKeys &&
                    it.cuePhysicalKeys == sample.cuePhysicalKeys
            } == true) return
        if (transitions.size == capacity) transitions.removeFirst()
        transitions.addLast(sample)
    }

    fun snapshot(): List<CueRouteTraceSample> = transitions.toList()
}

/** Silent output-only preflight. Does not measure acoustic or input round-trip latency. */
object AndroidCueRouteVerifier {
    @Volatile private var lastPreflightResult: CuePreflightResult? = null
    private val negotiatedProfiles = ConcurrentHashMap<String, CueOutputProfile>()

    fun lastPreflightResult(): CuePreflightResult? = lastPreflightResult
    fun lastPreflightDiagnostic(): String? = lastPreflightResult?.diagnosticSummary()

    fun negotiatedProfile(
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        sessionSampleRateHz: Int,
    ): CueOutputProfile? = negotiatedProfiles[profileKey(main, cue, sessionSampleRateHz)]

    fun invalidateNegotiatedProfiles() {
        negotiatedProfiles.clear()
    }

    @Synchronized
    fun verifyDevices(
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        sampleRateHz: Int,
        keepRunning: () -> Boolean,
    ): CuePreflightResult {
        require(sampleRateHz > 0)
        val expectedMainKey = AndroidOutputRouteIdentity.physicalKey(main)
        val expectedCueKey = AndroidOutputRouteIdentity.physicalKey(cue)
        val profileKey = profileKey(main, cue, sampleRateHz)
        negotiatedProfiles.remove(profileKey)

        if (expectedMainKey == expectedCueKey) {
            val result = CuePreflightResult(
                CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT,
                messageFor(CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT),
                emptyEvidence(main, cue, sampleRateHz),
            )
            lastPreflightResult = result
            return result
        }

        val candidates = CueOutputNegotiationPolicy.candidateCueSampleRates(
            sessionSampleRateHz = sampleRateHz,
            cueAdvertisedSampleRates = cue.sampleRates.toList(),
        )
        val attempted = ArrayList<Int>(candidates.size)
        var last: CuePreflightResult? = null

        for (cueSampleRateHz in candidates) {
            if (!keepRunning()) {
                last = CuePreflightResult(
                    CuePreflightStatus.CANCELLED,
                    messageFor(CuePreflightStatus.CANCELLED),
                    emptyEvidence(main, cue, sampleRateHz).copy(
                        attemptedCueSampleRates = attempted.toList(),
                    ),
                )
                break
            }
            val attempt = verifyDeviceConfiguration(
                main = main,
                cue = cue,
                mainSampleRateHz = sampleRateHz,
                cueSampleRateHz = cueSampleRateHz,
                keepRunning = keepRunning,
            )
            attempted += cueSampleRateHz
            val annotated = attempt.copy(
                evidence = attempt.evidence.copy(
                    attemptedCueSampleRates = attempted.toList(),
                    negotiatedCueSampleRateHz = cueSampleRateHz.takeIf { attempt.supported },
                    cueResamplingRequired = attempt.supported && cueSampleRateHz != sampleRateHz,
                ),
            )
            if (attempt.supported) {
                val profile = CueOutputProfile(
                    sessionSampleRateHz = sampleRateHz,
                    mainSampleRateHz = sampleRateHz,
                    cueSampleRateHz = cueSampleRateHz,
                )
                negotiatedProfiles[profileKey] = profile
                last = annotated.copy(
                    userMessage = if (profile.requiresCueResampling) {
                        "MAIN e CUE foram comprovados em saídas físicas distintas. " +
                            "CUE será adaptado automaticamente de ${sampleRateHz} para ${cueSampleRateHz} Hz sem alterar o projeto."
                    } else {
                        messageFor(CuePreflightStatus.SUPPORTED)
                    },
                )
                break
            }
            last = annotated
            if (attempt.status == CuePreflightStatus.CANCELLED ||
                attempt.status == CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT
            ) break
        }

        val verified = last ?: CuePreflightResult(
            CuePreflightStatus.OPEN_FAILED,
            messageFor(CuePreflightStatus.OPEN_FAILED),
            emptyEvidence(main, cue, sampleRateHz).copy(attemptedCueSampleRates = attempted.toList()),
        )
        lastPreflightResult = verified
        return verified
    }

    private fun verifyDeviceConfiguration(
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        mainSampleRateHz: Int,
        cueSampleRateHz: Int,
        keepRunning: () -> Boolean,
    ): CuePreflightResult {
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        var mainPreferredAccepted = false
        var cuePreferredAccepted = false
        val expectedMainKey = AndroidOutputRouteIdentity.physicalKey(main)
        val expectedCueKey = AndroidOutputRouteIdentity.physicalKey(cue)
        val transitions = CueRouteTraceBuffer()

        fun evidence() = CuePreflightEvidence(
            sampleRateHz = mainSampleRateHz,
            expectedMainPhysicalKey = expectedMainKey,
            expectedCuePhysicalKey = expectedCueKey,
            mainAdvertisedSampleRates = main.sampleRates.filter { it > 0 }.distinct().sorted(),
            cueAdvertisedSampleRates = cue.sampleRates.filter { it > 0 }.distinct().sorted(),
            mainAdvertisedChannelCounts = main.channelCounts.filter { it > 0 }.distinct().sorted(),
            cueAdvertisedChannelCounts = cue.channelCounts.filter { it > 0 }.distinct().sorted(),
            mainPreferredAccepted = mainPreferredAccepted,
            cuePreferredAccepted = cuePreferredAccepted,
            mainPreferredReassertedAfterPlay = null,
            cuePreferredReassertedAfterPlay = null,
            mainTrack = mainTrack?.let(::trackEvidence),
            cueTrack = cueTrack?.let(::trackEvidence),
            routedTransitions = transitions.snapshot(),
            attemptedCueSampleRates = listOf(cueSampleRateHz),
        )
        fun result(status: CuePreflightStatus) = CuePreflightResult(status, messageFor(status), evidence())

        return try {
            if (!keepRunning()) return result(CuePreflightStatus.CANCELLED)
            val openedMain = createTrack(mainSampleRateHz)
            val openedCue = createTrack(cueSampleRateHz)
            mainTrack = openedMain
            cueTrack = openedCue
            mainPreferredAccepted = openedMain.setPreferredDevice(main)
            cuePreferredAccepted = openedCue.setPreferredDevice(cue)
            if (!mainPreferredAccepted || !cuePreferredAccepted) {
                result(CuePreflightStatus.PREFERRED_ROUTE_REJECTED)
            } else {
                val trackResult = verifyTracks(
                    mainTrack = openedMain,
                    cueTrack = openedCue,
                    expectedMain = main,
                    expectedCue = cue,
                    sampleRateHz = mainSampleRateHz,
                    keepRunning = keepRunning,
                    routeObserver = { observation ->
                        transitions.record(
                            CueRouteTraceSample(
                                elapsedMs = observation.elapsedNs / 1_000_000L,
                                mainWriteResult = observation.mainWriteResult,
                                cueWriteResult = observation.cueWriteResult,
                                mainPhysicalKeys = observation.mainPhysicalKeys,
                                cuePhysicalKeys = observation.cuePhysicalKeys,
                            ),
                        )
                    },
                    cueSampleRateHz = cueSampleRateHz,
                )
                trackResult.copy(
                    evidence = trackResult.evidence.copy(
                        routedTransitions = transitions.snapshot(),
                        attemptedCueSampleRates = listOf(cueSampleRateHz),
                    ),
                )
            }
        } catch (_: RuntimeException) {
            result(CuePreflightStatus.OPEN_FAILED)
        } finally {
            runCatching { mainTrack?.release() }
            runCatching { cueTrack?.release() }
        }
    }

    private fun emptyEvidence(
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        sampleRateHz: Int,
    ) = CuePreflightEvidence(
        sampleRateHz = sampleRateHz,
        expectedMainPhysicalKey = AndroidOutputRouteIdentity.physicalKey(main),
        expectedCuePhysicalKey = AndroidOutputRouteIdentity.physicalKey(cue),
        mainAdvertisedSampleRates = main.sampleRates.filter { it > 0 }.distinct().sorted(),
        cueAdvertisedSampleRates = cue.sampleRates.filter { it > 0 }.distinct().sorted(),
        mainAdvertisedChannelCounts = main.channelCounts.filter { it > 0 }.distinct().sorted(),
        cueAdvertisedChannelCounts = cue.channelCounts.filter { it > 0 }.distinct().sorted(),
        mainPreferredAccepted = false,
        cuePreferredAccepted = false,
        mainPreferredReassertedAfterPlay = null,
        cuePreferredReassertedAfterPlay = null,
        mainTrack = null,
        cueTrack = null,
        routedTransitions = emptyList(),
    )

    private fun profileKey(main: AudioDeviceInfo, cue: AudioDeviceInfo, sessionSampleRateHz: Int): String =
        "${AndroidOutputRouteIdentity.physicalKey(main)}->${AndroidOutputRouteIdentity.physicalKey(cue)}@${sessionSampleRateHz}"

    fun verifyTracks(
        mainTrack: AudioTrack,
        cueTrack: AudioTrack,
        expectedMain: AudioDeviceInfo,
        expectedCue: AudioDeviceInfo,
        sampleRateHz: Int,
        keepRunning: () -> Boolean,
        mainPreferredAccepted: Boolean = true,
        cuePreferredAccepted: Boolean = true,
        routeObserver: ((CueRouteObservation) -> Unit)? = null,
        cueSampleRateHz: Int = sampleRateHz,
    ): CuePreflightResult {
        val expectedMainKey = AndroidOutputRouteIdentity.physicalKey(expectedMain)
        val expectedCueKey = AndroidOutputRouteIdentity.physicalKey(expectedCue)
        var mainPreferredReasserted: Boolean? = null
        var cuePreferredReasserted: Boolean? = null
        fun evidence() = CuePreflightEvidence(
            sampleRateHz = sampleRateHz,
            expectedMainPhysicalKey = expectedMainKey,
            expectedCuePhysicalKey = expectedCueKey,
            mainAdvertisedSampleRates = expectedMain.sampleRates.filter { it > 0 }.distinct().sorted(),
            cueAdvertisedSampleRates = expectedCue.sampleRates.filter { it > 0 }.distinct().sorted(),
            mainAdvertisedChannelCounts = expectedMain.channelCounts.filter { it > 0 }.distinct().sorted(),
            cueAdvertisedChannelCounts = expectedCue.channelCounts.filter { it > 0 }.distinct().sorted(),
            mainPreferredAccepted = mainPreferredAccepted,
            cuePreferredAccepted = cuePreferredAccepted,
            mainPreferredReassertedAfterPlay = mainPreferredReasserted,
            cuePreferredReassertedAfterPlay = cuePreferredReasserted,
            mainTrack = trackEvidence(mainTrack),
            cueTrack = trackEvidence(cueTrack),
            routedTransitions = emptyList(),
        )
        fun result(status: CuePreflightStatus) = CuePreflightResult(status, messageFor(status), evidence())
        if (expectedMainKey == expectedCueKey) return result(CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT)

        val silence = FloatArray(512 * 2)
        fun adapter(track: AudioTrack) = object : CueProbeOutput {
            override fun feedSilence(): Int =
                track.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING)

            override fun routedPhysicalKeys(): Set<String> =
                AndroidOutputRouteIdentity.routedPhysicalKeys(track)

            override fun clockObservation(): AudioClockObservation? {
                val timestamp = AudioTimestamp()
                return if (track.getTimestamp(timestamp)) {
                    AudioClockObservation(timestamp.framePosition, timestamp.nanoTime)
                } else {
                    null
                }
            }
        }
        return try {
            mainTrack.setVolume(0f)
            cueTrack.setVolume(0f)
            repeat(8) {
                if (mainTrack.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING) < 0 ||
                    cueTrack.write(silence, 0, silence.size, AudioTrack.WRITE_NON_BLOCKING) < 0) {
                    return result(CuePreflightStatus.WRITE_FAILED)
                }
            }
            mainTrack.play()
            cueTrack.play()
            mainPreferredReasserted = mainTrack.setPreferredDevice(expectedMain)
            cuePreferredReasserted = cueTrack.setPreferredDevice(expectedCue)
            if (mainPreferredReasserted != true || cuePreferredReasserted != true) {
                return result(CuePreflightStatus.PREFERRED_ROUTE_REJECTED)
            }
            val failure = CueStartupProbe.verify(
                main = adapter(mainTrack),
                cue = adapter(cueTrack),
                expectedMainPhysicalKey = expectedMainKey,
                expectedCuePhysicalKey = expectedCueKey,
                sampleRateHz = sampleRateHz,
                nowNs = System::nanoTime,
                sleepMs = { Thread.sleep(it) },
                keepRunning = { keepRunning() && !Thread.currentThread().isInterrupted },
                routeObserver = routeObserver,
                cueSampleRateHz = cueSampleRateHz,
            )
            result(failure?.toPreflightStatus() ?: CuePreflightStatus.SUPPORTED)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            result(CuePreflightStatus.CANCELLED)
        } catch (_: RuntimeException) {
            result(CuePreflightStatus.VERIFICATION_FAILED)
        } finally {
            runCatching { mainTrack.pause() }
            runCatching { mainTrack.flush() }
            runCatching { cueTrack.pause() }
            runCatching { cueTrack.flush() }
            runCatching { mainTrack.setVolume(1f) }
            runCatching { cueTrack.setVolume(1f) }
        }
    }

    private fun createTrack(sampleRateHz: Int): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferSize > 0)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            error("AudioTrack not initialized")
        }
        return track
    }

    private fun trackEvidence(track: AudioTrack) = CueTrackConfigurationEvidence(
        state = track.state,
        sampleRateHz = track.sampleRate,
        channelCount = track.channelCount,
        encoding = track.audioFormat,
        bufferCapacityFrames = track.bufferCapacityInFrames,
        bufferSizeFrames = track.bufferSizeInFrames,
        startThresholdFrames = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) track.startThresholdInFrames else null,
        performanceMode = track.performanceMode,
    )

    private fun CueStartupFailure.toPreflightStatus(): CuePreflightStatus = when (this) {
        CueStartupFailure.CANCELLED -> CuePreflightStatus.CANCELLED
        CueStartupFailure.WRITE_FAILED -> CuePreflightStatus.WRITE_FAILED
        CueStartupFailure.MISSING_EFFECTIVE_ROUTE -> CuePreflightStatus.MISSING_EFFECTIVE_ROUTE
        CueStartupFailure.CONVERGED_TO_MAIN -> CuePreflightStatus.CONVERGED_TO_MAIN
        CueStartupFailure.WRONG_OR_MIRRORED_ROUTE -> CuePreflightStatus.WRONG_OR_MIRRORED_ROUTE
        CueStartupFailure.ROUTE_CHANGED -> CuePreflightStatus.ROUTE_CHANGED
        CueStartupFailure.CLOCK_UNSTABLE -> CuePreflightStatus.CLOCK_UNSTABLE
        CueStartupFailure.OFFSET_EXCEEDED -> CuePreflightStatus.OFFSET_EXCEEDED
    }

    private fun messageFor(status: CuePreflightStatus): String = when (status) {
        CuePreflightStatus.SUPPORTED -> "MAIN e CUE foram comprovados em duas saídas físicas distintas."
        CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT -> "MAIN e CUE representam a mesma saída física."
        CuePreflightStatus.CANCELLED -> "A verificação MAIN/CUE foi cancelada; CUE permaneceu silencioso."
        CuePreflightStatus.OPEN_FAILED -> "O Android não conseguiu abrir simultaneamente as duas saídas selecionadas."
        CuePreflightStatus.WRITE_FAILED -> "O Android recusou o fluxo silencioso de verificação MAIN/CUE."
        CuePreflightStatus.PREFERRED_ROUTE_REJECTED -> "O Android recusou a preferência explícita por uma das saídas MAIN/CUE."
        CuePreflightStatus.MISSING_EFFECTIVE_ROUTE -> "O Android não publicou uma rota efetiva para uma das saídas MAIN/CUE."
        CuePreflightStatus.CONVERGED_TO_MAIN -> "O Android encaminhou o CUE para a mesma saída física da MAIN; este dispositivo não comprovou saída dupla independente."
        CuePreflightStatus.WRONG_OR_MIRRORED_ROUTE -> "O Android encaminhou ou espelhou áudio para uma saída física diferente da selecionada."
        CuePreflightStatus.ROUTE_CHANGED -> "Uma rota MAIN/CUE mudou depois de ter sido confirmada."
        CuePreflightStatus.CLOCK_UNSTABLE -> "Os clocks MAIN/CUE não avançaram de forma estável durante a verificação."
        CuePreflightStatus.OFFSET_EXCEEDED -> "O offset inicial entre MAIN e CUE excedeu 12 ms."
        CuePreflightStatus.VERIFICATION_FAILED -> "O Android não conseguiu comprovar sincronização inicial entre MAIN e CUE."
    }
}
