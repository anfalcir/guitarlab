package studio.guitarlab.platform.audio.android

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import studio.guitarlab.core.audio.AudioDeviceDescriptor
import studio.guitarlab.core.audio.AudioTransport
import studio.guitarlab.core.audio.PcmEncoding

internal object AndroidAudioDeviceMapper {
    fun map(device: AudioDeviceInfo): AudioDeviceDescriptor = AudioDeviceDescriptor(
        key = device.id.toString(),
        name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel(device.type),
        typeLabel = typeLabel(device.type),
        transport = transport(device.type),
        supportsInput = device.isSource,
        supportsOutput = device.isSink,
        sampleRatesHz = device.sampleRates.toList().filter { it > 0 }.distinct().sorted(),
        channelCounts = device.channelCounts.toList().filter { it > 0 }.distinct().sorted(),
        encodings = device.encodings.map(::encoding).filterNotNull().distinct()
    )

    fun typeLabel(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Built-in earpiece"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "Analog line"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Digital line"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio device"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB accessory"
        AudioDeviceInfo.TYPE_DOCK -> "Dock"
        AudioDeviceInfo.TYPE_FM -> "FM"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in microphone"
        AudioDeviceInfo.TYPE_FM_TUNER -> "FM tuner"
        AudioDeviceInfo.TYPE_TV_TUNER -> "TV tuner"
        AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
        AudioDeviceInfo.TYPE_AUX_LINE -> "Aux line"
        AudioDeviceInfo.TYPE_IP -> "IP audio"
        AudioDeviceInfo.TYPE_BUS -> "Audio bus"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing aid"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "Safe speaker"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE speaker"
        AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI eARC"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth LE broadcast"
        else -> "Audio device ($type)"
    }

    private fun transport(type: Int): AudioTransport = when (type) {
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_USB_HEADSET -> AudioTransport.USB

        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_MIC,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> AudioTransport.BUILT_IN

        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> AudioTransport.BLUETOOTH

        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC -> AudioTransport.HDMI

        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
        AudioDeviceInfo.TYPE_AUX_LINE -> AudioTransport.WIRED

        else -> AudioTransport.OTHER
    }

    private fun encoding(value: Int): PcmEncoding? = when (value) {
        AudioFormat.ENCODING_PCM_FLOAT -> PcmEncoding.FLOAT_32
        AudioFormat.ENCODING_PCM_16BIT -> PcmEncoding.PCM_16
        else -> null
    }
}
