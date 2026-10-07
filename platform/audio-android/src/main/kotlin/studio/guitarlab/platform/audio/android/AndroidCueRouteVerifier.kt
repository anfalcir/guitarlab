package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioTimestamp
import android.os.Build
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import studio.guitarlab.core.audio.AudioClockObservation
import studio.guitarlab.core.audio.CueProbeOutput
import studio.guitarlab.core.audio.CueRouteObservation
import studio.guitarlab.core.audio.CueRouteSafetyPolicy
import studio.guitarlab.core.audio.CueStartupFailure
import studio.guitarlab.core.audio.CueStartupProbe

enum class CuePreflightStatus {
    SUPPORTED,
    EXPECTED_PAIR_NOT_DISTINCT,
    CANCELLED,
    OPEN_FAILED,
    WRITE_FAILED,
    PREFERRED_ROUTE_REJECTED,
    COMMUNICATION_DEVICE_UNAVAILABLE,
    COMMUNICATION_SELECTION_REJECTED,
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

data class CueCommunicationEvidence(
    val apiSupported: Boolean,
    val availablePhysicalKeys: List<String>,
    val selectedPhysicalKey: String?,
    val requestAccepted: Boolean,
    val audioModeBefore: Int?,
    val audioModeDuring: Int?,
    val modeRequired: Boolean,
    val duckingRequested: Boolean = false,
    val fidelityQualification: String = "PENDING_PHYSICAL",
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
    val initialOffsetNs: Long? = null,
    val strategy: CueOutputStrategy = CueOutputStrategy.MULTI_DEVICE,
    val communication: CueCommunicationEvidence? = null,
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
        append("; initialOffsetNs=").append(evidence.initialOffsetNs)
        append("; strategy=").append(evidence.strategy.name)
        evidence.communication?.let { communication ->
            append("; communicationApiSupported=").append(communication.apiSupported)
            append("; communicationAvailable=").append(communication.availablePhysicalKeys)
            append("; communicationSelected=").append(communication.selectedPhysicalKey)
            append("; communicationAccepted=").append(communication.requestAccepted)
            append("; communicationModeBefore=").append(communication.audioModeBefore)
            append("; communicationModeDuring=").append(communication.audioModeDuring)
            append("; communicationModeRequired=").append(communication.modeRequired)
            append("; duckingRequested=").append(communication.duckingRequested)
            append("; fidelityQualification=").append(communication.fidelityQualification)
        }
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
    @Volatile private var lastRuntimeCueDiagnostic: String? = null
    private val negotiatedProfiles = ConcurrentHashMap<String, CueOutputProfile>()
    private val runtimeCueSessionSequence = AtomicLong(0L)

    fun lastPreflightResult(): CuePreflightResult? = lastPreflightResult
    fun lastPreflightDiagnostic(): String? = lastPreflightResult?.diagnosticSummary()
    fun lastRuntimeCueDiagnostic(): String? = lastRuntimeCueDiagnostic
    fun clearRuntimeCueDiagnostic() {
        lastRuntimeCueDiagnostic = null
    }

    fun beginRuntimeCueDiagnostic(
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        strategy: CueOutputStrategy,
        mainSampleRateHz: Int,
        cueSampleRateHz: Int,
    ): Long {
        val sessionId = runtimeCueSessionSequence.incrementAndGet()
        lastRuntimeCueDiagnostic = buildString {
            append("sessionId=").append(sessionId)
            append("; state=STARTED")
            append("; startedAtEpochMs=").append(System.currentTimeMillis())
            append("; expectedMain=").append(AndroidOutputRouteIdentity.physicalKey(main))
            append("; expectedCue=").append(AndroidOutputRouteIdentity.physicalKey(cue))
            append("; strategy=").append(strategy.name)
            append("; mainSampleRateHz=").append(mainSampleRateHz)
            append("; cueSampleRateHz=").append(cueSampleRateHz)
        }.take(4_096)
        return sessionId
    }

    fun recordRuntimeCueDiagnostic(sessionId: Long, detail: String) {
        lastRuntimeCueDiagnostic = "sessionId=$sessionId; $detail".take(4_096)
    }

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
        audioManager: AudioManager,
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
            return CuePreflightResult(
                CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT,
                messageFor(CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT),
                emptyEvidence(main, cue, sampleRateHz),
            ).also { lastPreflightResult = it }
        }

        val candidates = CueOutputNegotiationPolicy.candidateCueSampleRates(
            sampleRateHz,
            cue.sampleRates.toList(),
        )
        val communicationDiscovery = AndroidCommunicationCueRouting.discover(audioManager, cue)
        var communicationFailure: CuePreflightResult? = null

        if (communicationDiscovery.apiSupported && communicationDiscovery.matchingDevice != null) {
            val attempted = ArrayList<Int>()
            for (cueRate in candidates.take(MAX_COMMUNICATION_RATE_CANDIDATES)) {
                if (!keepRunning()) {
                    return CuePreflightResult(
                        CuePreflightStatus.CANCELLED,
                        messageFor(CuePreflightStatus.CANCELLED),
                        emptyEvidence(main, cue, sampleRateHz).copy(
                            attemptedCueSampleRates = attempted.toList(),
                            strategy = CueOutputStrategy.COMMUNICATION_SPLIT,
                        ),
                    ).also { lastPreflightResult = it }
                }
                val attempt = verifyCommunicationConfiguration(
                    audioManager = audioManager,
                    main = main,
                    cue = cue,
                    mainSampleRateHz = sampleRateHz,
                    cueSampleRateHz = cueRate,
                    communicationModeRequired = false,
                    keepRunning = keepRunning,
                )
                attempted += cueRate
                val annotated = attempt.copy(
                    evidence = attempt.evidence.copy(
                        attemptedCueSampleRates = attempted.toList(),
                        negotiatedCueSampleRateHz = cueRate.takeIf { attempt.supported },
                        cueResamplingRequired = attempt.supported && cueRate != sampleRateHz,
                        strategy = CueOutputStrategy.COMMUNICATION_SPLIT,
                    ),
                )
                if (attempt.supported) {
                    negotiatedProfiles[profileKey] = CueOutputProfile(
                        sessionSampleRateHz = sampleRateHz,
                        mainSampleRateHz = sampleRateHz,
                        cueSampleRateHz = cueRate,
                        strategy = CueOutputStrategy.COMMUNICATION_SPLIT,
                        communicationModeRequired = false,
                    )
                    val verified = annotated.copy(
                        userMessage = "Saída CUE pronta. O áudio principal e o CUE estão usando saídas separadas.",
                    )
                    lastPreflightResult = verified
                    return verified
                }
                communicationFailure = annotated
                if (attempt.status == CuePreflightStatus.CANCELLED) break
            }
        }

        val attemptedMedia = ArrayList<Int>(candidates.size)
        var lastMedia: CuePreflightResult? = null
        for (cueRate in candidates) {
            if (!keepRunning()) {
                lastMedia = CuePreflightResult(
                    CuePreflightStatus.CANCELLED,
                    messageFor(CuePreflightStatus.CANCELLED),
                    emptyEvidence(main, cue, sampleRateHz).copy(attemptedCueSampleRates = attemptedMedia.toList()),
                )
                break
            }
            val attempt = verifyDeviceConfiguration(main, cue, sampleRateHz, cueRate, keepRunning)
            attemptedMedia += cueRate
            val annotated = attempt.copy(
                evidence = attempt.evidence.copy(
                    attemptedCueSampleRates = attemptedMedia.toList(),
                    negotiatedCueSampleRateHz = cueRate.takeIf { attempt.supported },
                    cueResamplingRequired = attempt.supported && cueRate != sampleRateHz,
                    strategy = CueOutputStrategy.MULTI_DEVICE,
                ),
            )
            if (attempt.supported) {
                negotiatedProfiles[profileKey] = CueOutputProfile(
                    sampleRateHz,
                    sampleRateHz,
                    cueRate,
                    CueOutputStrategy.MULTI_DEVICE,
                )
                val verified = annotated.copy(
                    userMessage = "Saída CUE pronta. O áudio principal e o CUE estão usando saídas separadas.",
                )
                lastPreflightResult = verified
                return verified
            }
            lastMedia = annotated
        }

        val finalResult = communicationFailure ?: lastMedia ?: CuePreflightResult(
            CuePreflightStatus.OPEN_FAILED,
            messageFor(CuePreflightStatus.OPEN_FAILED),
            emptyEvidence(main, cue, sampleRateHz),
        )
        lastPreflightResult = finalResult
        return finalResult
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

    private fun verifyCommunicationConfiguration(
        audioManager: AudioManager,
        main: AudioDeviceInfo,
        cue: AudioDeviceInfo,
        mainSampleRateHz: Int,
        cueSampleRateHz: Int,
        communicationModeRequired: Boolean,
        keepRunning: () -> Boolean,
    ): CuePreflightResult {
        val discovery = AndroidCommunicationCueRouting.discover(audioManager, cue)
        val baseCommunication = CueCommunicationEvidence(
            discovery.apiSupported,
            discovery.availablePhysicalKeys,
            discovery.currentPhysicalKey,
            false,
            audioManager.mode,
            null,
            communicationModeRequired,
        )
        fun early(status: CuePreflightStatus) = CuePreflightResult(
            status,
            messageFor(status),
            emptyEvidence(main, cue, mainSampleRateHz).copy(
                strategy = CueOutputStrategy.COMMUNICATION_SPLIT,
                communication = baseCommunication,
                attemptedCueSampleRates = listOf(cueSampleRateHz),
            ),
        )
        if (!discovery.apiSupported || discovery.matchingDevice == null) {
            return early(CuePreflightStatus.COMMUNICATION_DEVICE_UNAVAILABLE)
        }
        val session = AndroidCommunicationCueRouting.begin(audioManager, cue, communicationModeRequired)
            ?: return early(CuePreflightStatus.COMMUNICATION_SELECTION_REJECTED)
        var mainTrack: AudioTrack? = null
        var cueTrack: AudioTrack? = null
        val transitions = CueRouteTraceBuffer()
        return try {
            if (!keepRunning()) return early(CuePreflightStatus.CANCELLED)
            val openedMain = createTrack(mainSampleRateHz, AudioAttributes.USAGE_MEDIA)
            val openedCue = createTrack(cueSampleRateHz, AudioAttributes.USAGE_VOICE_COMMUNICATION)
            mainTrack = openedMain
            cueTrack = openedCue
            val mainAccepted = openedMain.setPreferredDevice(main)
            val communication = CueCommunicationEvidence(
                true,
                discovery.availablePhysicalKeys,
                AndroidOutputRouteIdentity.physicalKey(session.device),
                true,
                session.modeBefore,
                session.modeDuring,
                communicationModeRequired,
                duckingRequested = false,
                fidelityQualification = "PENDING_PHYSICAL",
            )
            if (!mainAccepted) {
                CuePreflightResult(
                    CuePreflightStatus.PREFERRED_ROUTE_REJECTED,
                    messageFor(CuePreflightStatus.PREFERRED_ROUTE_REJECTED),
                    emptyEvidence(main, cue, mainSampleRateHz).copy(
                        cuePreferredAccepted = true,
                        mainTrack = trackEvidence(openedMain),
                        cueTrack = trackEvidence(openedCue),
                        strategy = CueOutputStrategy.COMMUNICATION_SPLIT,
                        communication = communication,
                    ),
                )
            } else {
                val result = verifyTracks(
                    openedMain,
                    openedCue,
                    main,
                    cue,
                    mainSampleRateHz,
                    keepRunning,
                    mainPreferredAccepted = true,
                    cuePreferredAccepted = true,
                    routeObserver = { o -> transitions.record(CueRouteTraceSample(
                        o.elapsedNs / 1_000_000L, o.mainWriteResult, o.cueWriteResult,
                        o.mainPhysicalKeys, o.cuePhysicalKeys,
                    )) },
                    cueSampleRateHz = cueSampleRateHz,
                    strategy = CueOutputStrategy.COMMUNICATION_SPLIT,
                    communicationEvidence = communication,
                    cueRouteReassert = { session.reassert() },
                )
                result.copy(evidence = result.evidence.copy(routedTransitions = transitions.snapshot()))
            }
        } catch (_: RuntimeException) {
            early(CuePreflightStatus.OPEN_FAILED)
        } finally {
            runCatching { mainTrack?.release() }
            runCatching { cueTrack?.release() }
            session.close()
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
        strategy: CueOutputStrategy = CueOutputStrategy.MULTI_DEVICE,
        communicationEvidence: CueCommunicationEvidence? = null,
        cueRouteReassert: (() -> Boolean)? = null,
    ): CuePreflightResult {
        val expectedMainKey = AndroidOutputRouteIdentity.physicalKey(expectedMain)
        val expectedCueKey = AndroidOutputRouteIdentity.physicalKey(expectedCue)
        var mainPreferredReasserted: Boolean? = null
        var cuePreferredReasserted: Boolean? = null
        var initialOffsetNs: Long? = null
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
            initialOffsetNs = initialOffsetNs,
            strategy = strategy,
            communication = communicationEvidence,
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
            cuePreferredReasserted = cueRouteReassert?.invoke() ?: cueTrack.setPreferredDevice(expectedCue)
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
                maxInitialOffsetNs = if (strategy == CueOutputStrategy.COMMUNICATION_SPLIT) {
                    CueRouteSafetyPolicy.COMMUNICATION_INITIAL_OFFSET_LIMIT_NS
                } else {
                    CueRouteSafetyPolicy.DEFAULT_INITIAL_OFFSET_LIMIT_NS
                },
                initialOffsetObserver = { initialOffsetNs = it },
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

    private fun createTrack(
        sampleRateHz: Int,
        usage: Int = AudioAttributes.USAGE_MEDIA,
    ): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferSize > 0)
        val track = AudioTrack.Builder()
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

    private const val MAX_COMMUNICATION_RATE_CANDIDATES = 2

    private fun messageFor(status: CuePreflightStatus): String = when (status) {
        CuePreflightStatus.SUPPORTED -> "MAIN e CUE foram comprovados em duas saídas físicas distintas."
        CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT -> "MAIN e CUE representam a mesma saída física."
        CuePreflightStatus.CANCELLED -> "A verificação MAIN/CUE foi cancelada; CUE permaneceu silencioso."
        CuePreflightStatus.OPEN_FAILED -> "O Android não conseguiu abrir simultaneamente as duas saídas selecionadas."
        CuePreflightStatus.WRITE_FAILED -> "O Android recusou o fluxo silencioso de verificação MAIN/CUE."
        CuePreflightStatus.PREFERRED_ROUTE_REJECTED -> "O Android recusou a preferência explícita por uma das saídas MAIN/CUE."
        CuePreflightStatus.COMMUNICATION_DEVICE_UNAVAILABLE -> "A saída CUE selecionada não está disponível como dispositivo de comunicação neste Android."
        CuePreflightStatus.COMMUNICATION_SELECTION_REJECTED -> "O Android recusou selecionar a saída CUE como dispositivo de comunicação."
        CuePreflightStatus.MISSING_EFFECTIVE_ROUTE -> "O Android não publicou uma rota efetiva para uma das saídas MAIN/CUE."
        CuePreflightStatus.CONVERGED_TO_MAIN -> "O Android encaminhou o CUE para a mesma saída física da MAIN; este dispositivo não comprovou saída dupla independente."
        CuePreflightStatus.WRONG_OR_MIRRORED_ROUTE -> "O Android encaminhou ou espelhou áudio para uma saída física diferente da selecionada."
        CuePreflightStatus.ROUTE_CHANGED -> "Uma rota MAIN/CUE mudou depois de ter sido confirmada."
        CuePreflightStatus.CLOCK_UNSTABLE -> "Os clocks MAIN/CUE não avançaram de forma estável durante a verificação."
        CuePreflightStatus.OFFSET_EXCEEDED -> "Não foi possível estabilizar a saída CUE. Tente novamente."
        CuePreflightStatus.VERIFICATION_FAILED -> "O Android não conseguiu comprovar sincronização inicial entre MAIN e CUE."
    }
}
