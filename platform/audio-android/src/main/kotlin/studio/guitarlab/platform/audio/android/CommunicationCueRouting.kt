package studio.guitarlab.platform.audio.android

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

internal data class CommunicationCueDiscovery(
    val apiSupported: Boolean,
    val availablePhysicalKeys: List<String>,
    val matchingDevice: AudioDeviceInfo?,
    val currentPhysicalKey: String?,
)

internal object CommunicationCuePolicy {
    val modeCandidates: List<Boolean> = listOf(false)

    fun shouldAttemptAfterMedia(status: CuePreflightStatus): Boolean = when (status) {
        CuePreflightStatus.CANCELLED,
        CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT -> false
        else -> true
    }
}

internal class CommunicationCueSession internal constructor(
    private val audioManager: AudioManager,
    val device: AudioDeviceInfo,
    val modeRequired: Boolean,
    val modeBefore: Int,
    val modeDuring: Int,
    private val previousCommunicationDevice: AudioDeviceInfo?,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    fun reassert(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || closed.get()) return false
        return runCatching {
            if (modeRequired && audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
            audioManager.setCommunicationDevice(device)
        }.getOrDefault(false)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching { audioManager.clearCommunicationDevice() }
        val previous = previousCommunicationDevice
        if (previous != null) {
            val stillAvailable = runCatching {
                audioManager.availableCommunicationDevices.any { it.id == previous.id }
            }.getOrDefault(false)
            if (stillAvailable) runCatching { audioManager.setCommunicationDevice(previous) }
        }
        if (modeRequired && audioManager.mode != modeBefore) {
            runCatching { audioManager.mode = modeBefore }
        }
    }
}

internal object AndroidCommunicationCueRouting {
    fun discover(audioManager: AudioManager, cue: AudioDeviceInfo): CommunicationCueDiscovery {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return CommunicationCueDiscovery(false, emptyList(), null, null)
        }
        val available = runCatching { audioManager.availableCommunicationDevices }.getOrDefault(emptyList())
        val expectedKey = AndroidOutputRouteIdentity.physicalKey(cue)
        val matching = available.firstOrNull { AndroidOutputRouteIdentity.physicalKey(it) == expectedKey }
        val current = runCatching { audioManager.communicationDevice }.getOrNull()
        return CommunicationCueDiscovery(
            apiSupported = true,
            availablePhysicalKeys = available.map(AndroidOutputRouteIdentity::physicalKey).distinct().sorted(),
            matchingDevice = matching,
            currentPhysicalKey = current?.let(AndroidOutputRouteIdentity::physicalKey),
        )
    }

    fun begin(audioManager: AudioManager, cue: AudioDeviceInfo, modeRequired: Boolean): CommunicationCueSession? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val device = discover(audioManager, cue).matchingDevice ?: return null
        val previousMode = audioManager.mode
        val previousDevice = runCatching { audioManager.communicationDevice }.getOrNull()
        return try {
            if (modeRequired && previousMode != AudioManager.MODE_IN_COMMUNICATION) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
            if (!audioManager.setCommunicationDevice(device)) {
                if (modeRequired && audioManager.mode != previousMode) audioManager.mode = previousMode
                null
            } else {
                CommunicationCueSession(
                    audioManager, device, modeRequired, previousMode, audioManager.mode, previousDevice,
                )
            }
        } catch (_: RuntimeException) {
            runCatching { audioManager.clearCommunicationDevice() }
            if (modeRequired && audioManager.mode != previousMode) runCatching { audioManager.mode = previousMode }
            null
        }
    }
}
