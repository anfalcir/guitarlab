package studio.guitarlab.platform.audio.android

import android.media.AudioDeviceInfo
import android.media.AudioTrack
import android.os.Build

/**
 * Canonical physical identity for an Android output route.
 *
 * AudioDeviceInfo.id identifies a logical endpoint and is intentionally not used as physical
 * identity. OEM/HAL implementations may expose one physical USB or built-in destination through
 * several logical endpoint ids, and API 36 may report more than one routed logical device for one
 * AudioTrack. Safety therefore compares normalized physical identities while still rejecting
 * mirroring to any additional physical destination.
 */
internal object AndroidOutputRouteIdentity {
    fun expectedPairDistinct(main: AudioDeviceInfo, cue: AudioDeviceInfo): Boolean =
        physicalKey(main) != physicalKey(cue)

    fun canonicalRoutedDeviceId(track: AudioTrack, expected: AudioDeviceInfo): Int? {
        val expectedKey = physicalKey(expected)
        val routedKeys = routedPhysicalKeys(track)
        return if (routesOnlyToExpected(expectedKey, routedKeys)) expected.id else null
    }

    fun pairRemainsDistinct(
        mainTrack: AudioTrack,
        cueTrack: AudioTrack,
        expectedMain: AudioDeviceInfo,
        expectedCue: AudioDeviceInfo,
    ): Boolean {
        val mainKey = physicalKey(expectedMain)
        val cueKey = physicalKey(expectedCue)
        if (mainKey == cueKey) return false
        return routesOnlyToExpected(mainKey, routedPhysicalKeys(mainTrack)) &&
            routesOnlyToExpected(cueKey, routedPhysicalKeys(cueTrack))
    }

    fun routedPhysicalKeys(track: AudioTrack): Set<String> {
        val routed = if (Build.VERSION.SDK_INT >= 36) {
            track.routedDevices
        } else {
            listOfNotNull(track.routedDevice)
        }
        return routed.mapTo(linkedSetOf(), ::physicalKey)
    }

    internal fun routesOnlyToExpected(expectedKey: String, routedKeys: Collection<String>): Boolean =
        routedKeys.isNotEmpty() && routedKeys.all { it == expectedKey }

    internal fun physicalKey(device: AudioDeviceInfo): String {
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address.orEmpty() else ""
        return physicalKey(device.type, device.productName?.toString().orEmpty(), address)
    }

    internal fun physicalKey(type: Int, productName: String, address: String): String {
        val product = normalize(productName)
        return when {
            type in USB_TYPES -> "usb|$product|${usbPhysicalAddress(address)}"
            type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "builtin-speaker|$product"
            else -> "raw|$type|$product|${normalize(address)}"
        }
    }

    private fun usbPhysicalAddress(address: String): String {
        val normalized = normalize(address)
        if (normalized.isBlank()) return ""
        Regex("(?:^|[;,\\s])card\\s*=\\s*([^;,\\s]+)", RegexOption.IGNORE_CASE)
            .find(normalized)?.groupValues?.getOrNull(1)?.let { return "card=$it" }
        return normalized
            .replace(Regex("(?:[;,\\s]+)?device\\s*=\\s*[^;,\\s]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:[;,\\s]+)?endpoint\\s*=\\s*[^;,\\s]+", RegexOption.IGNORE_CASE), "")
            .trim(' ', ';', ',')
    }

    private fun normalize(value: String): String = value.trim().lowercase()

    private val USB_TYPES = setOf(
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_USB_HEADSET,
    )
}
