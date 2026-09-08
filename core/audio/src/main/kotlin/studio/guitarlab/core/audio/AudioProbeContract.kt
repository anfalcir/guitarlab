package studio.guitarlab.core.audio

import kotlinx.coroutines.flow.Flow

/**
 * Platform-neutral contracts used by the M2 USB Audio Probe.
 *
 * The application/UI only sees these values. Android framework types stay in the
 * platform implementation so the future realtime Oboe engine can replace the
 * probe implementation without leaking platform details into the product layer.
 */

enum class AudioDirection {
    INPUT,
    OUTPUT
}

enum class AudioTransport {
    USB,
    BUILT_IN,
    BLUETOOTH,
    HDMI,
    WIRED,
    OTHER
}

enum class PcmEncoding {
    FLOAT_32,
    PCM_16
}

data class AudioDeviceDescriptor(
    val key: String,
    val name: String,
    val typeLabel: String,
    val transport: AudioTransport,
    val supportsInput: Boolean,
    val supportsOutput: Boolean,
    val sampleRatesHz: List<Int> = emptyList(),
    val channelCounts: List<Int> = emptyList(),
    val encodings: List<PcmEncoding> = emptyList()
) {
    fun supports(direction: AudioDirection): Boolean = when (direction) {
        AudioDirection.INPUT -> supportsInput
        AudioDirection.OUTPUT -> supportsOutput
    }
}

data class AudioStreamConfig(
    val deviceKey: String?,
    val routedDeviceKey: String? = null,
    val sampleRateHz: Int,
    val channelCount: Int,
    val encoding: PcmEncoding,
    val bufferFrames: Int,
    val requestedLowLatency: Boolean,
    val inputSourceLabel: String? = null
)

enum class AudioProbeOperation {
    PLAYBACK,
    RECORD,
    DUPLEX
}

data class AudioSignalStats(
    val frames: Long = 0,
    val peak: Float = 0f,
    val rms: Float = 0f
)

data class AudioProbeResult(
    val operation: AudioProbeOperation,
    val success: Boolean,
    val stopped: Boolean = false,
    val elapsedMs: Long,
    val inputConfig: AudioStreamConfig? = null,
    val outputConfig: AudioStreamConfig? = null,
    val inputStats: AudioSignalStats? = null,
    val outputFrames: Long = 0,
    val outputUnderruns: Int = 0,
    val message: String,
    val warnings: List<String> = emptyList()
)

interface AudioProbeEngine : AutoCloseable {
    val deviceUpdates: Flow<List<AudioDeviceDescriptor>>

    suspend fun refreshDevices(): List<AudioDeviceDescriptor>

    suspend fun runPlaybackTest(
        outputDeviceKey: String?,
        durationMs: Long = 1_500
    ): AudioProbeResult

    suspend fun runRecordTest(
        inputDeviceKey: String?,
        durationMs: Long = 3_000
    ): AudioProbeResult

    suspend fun runDuplexTest(
        inputDeviceKey: String?,
        outputDeviceKey: String?,
        durationMs: Long = 5_000
    ): AudioProbeResult

    fun stopCurrentTest()
}
