package studio.guitarlab.app.ui

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import studio.guitarlab.core.audio.MonitoringMode

data class StudioAudioDeviceChoice(
    val signature: String,
    val label: String,
    val deviceId: Int,
)

class StudioAudioRoutingStore(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun inputChoices(): List<StudioAudioDeviceChoice> =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).map(::toChoice).sortedBy { it.label.lowercase() }

    fun outputChoices(): List<StudioAudioDeviceChoice> =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map(::toChoice).sortedBy { it.label.lowercase() }

    fun selectedInputSignature(): String? = preferences.getString(KEY_INPUT_SIGNATURE, null)
    fun selectedOutputSignature(): String? = preferences.getString(KEY_OUTPUT_SIGNATURE, null)

    fun selectInput(signature: String?) {
        preferences.edit().putString(KEY_INPUT_SIGNATURE, signature).apply()
    }

    fun selectOutput(signature: String?) {
        preferences.edit().putString(KEY_OUTPUT_SIGNATURE, signature).apply()
    }

    fun monitoringMode(): MonitoringMode {
        val stored = preferences.getString(KEY_MONITORING_MODE, MonitoringMode.AUTO.name)
        return runCatching { MonitoringMode.valueOf(stored ?: MonitoringMode.AUTO.name) }.getOrDefault(MonitoringMode.AUTO)
    }

    fun selectMonitoringMode(mode: MonitoringMode) {
        preferences.edit().putString(KEY_MONITORING_MODE, mode.name).apply()
    }

    fun resolveSelectedInputDeviceId(): Int? = resolveSelectedInputDevice()?.id
    fun resolveSelectedOutputDeviceId(): Int? = resolveSelectedOutputDevice()?.id

    fun resolveSelectedInputDevice(): AudioDeviceInfo? =
        resolveSelectedDevice(AudioManager.GET_DEVICES_INPUTS, selectedInputSignature())

    fun resolveSelectedOutputDevice(): AudioDeviceInfo? =
        resolveSelectedDevice(AudioManager.GET_DEVICES_OUTPUTS, selectedOutputSignature())

    fun isSelectedInputUnavailable(): Boolean =
        !selectedInputSignature().isNullOrBlank() && resolveSelectedInputDevice() == null

    fun isSelectedOutputUnavailable(): Boolean =
        !selectedOutputSignature().isNullOrBlank() && resolveSelectedOutputDevice() == null

    private fun resolveSelectedDevice(deviceFlag: Int, signature: String?): AudioDeviceInfo? {
        if (signature.isNullOrBlank()) return null
        return audioManager.getDevices(deviceFlag).firstOrNull { toChoice(it).signature == signature }
    }

    private fun toChoice(device: AudioDeviceInfo): StudioAudioDeviceChoice {
        val product = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Dispositivo de áudio"
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address.orEmpty() else ""
        val label = if (address.isBlank()) product else "$product • $address"
        return StudioAudioDeviceChoice(
            signature = buildString {
                append(device.type)
                append('|')
                append(product)
                append('|')
                append(address)
            },
            label = label,
            deviceId = device.id,
        )
    }

    private companion object {
        const val PREFS_NAME = "studio_audio_routing"
        const val KEY_INPUT_SIGNATURE = "input_signature"
        const val KEY_OUTPUT_SIGNATURE = "output_signature"
        const val KEY_MONITORING_MODE = "monitoring_mode"
    }
}
