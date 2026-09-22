package studio.guitarlab.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioAudioRoutePolicyTest {
    @Test
    fun duplicateUsbLogicalEndpointsCollapseToOnePhysicalChoice() {
        val device = choice(id = 41, type = 11, rates = listOf(44_100), channels = listOf(2), signature = "11|MK-300|")
        val headset = choice(id = 42, type = 22, rates = listOf(48_000), channels = listOf(1, 2), signature = "22|MK-300|")

        val result = StudioAudioRoutePolicy.canonicalizeOutputs(listOf(device, headset))

        assertEquals(1, result.size)
        assertEquals("MK-300", result.single().label)
        assertEquals(listOf(41, 42), result.single().candidateDeviceIds)
        assertTrue(result.single().legacySignatures.containsAll(listOf("11|MK-300|", "22|MK-300|")))
        assertTrue(result.single().legacySignatures.any { it.startsWith("route2:usb:") })
        assertTrue(result.single().signature.startsWith("route3:usb:"))
    }

    @Test
    fun legacyEndpointSignatureMigratesToCanonicalPhysicalRoute() {
        val raw = listOf(
            choice(id = 41, type = 11, signature = "11|MK-300|card=2"),
            choice(id = 42, type = 22, signature = "22|MK-300|card=2"),
        )

        val first = StudioAudioRoutePolicy.canonicalSignature(raw, "11|MK-300|card=2")
        val second = StudioAudioRoutePolicy.canonicalSignature(raw, "22|MK-300|card=2")

        assertEquals(first, second)
        assertTrue(first.orEmpty().startsWith("route3:usb:"))
    }

    @Test
    fun differentUsbAddressesRemainDistinctPhysicalRoutes() {
        val first = choice(id = 41, type = 11, address = "card=2;device=0", signature = "a")
        val second = choice(id = 42, type = 22, address = "card=3;device=0", signature = "b")

        val result = StudioAudioRoutePolicy.canonicalizeOutputs(listOf(first, second))

        assertEquals(2, result.size)
        assertNotEquals(result[0].signature, result[1].signature)
    }


    @Test
    fun builtInSpeakerLogicalEndpointsCollapseToOnePhysicalChoice() {
        val raw = listOf(
            choice(id = 1, type = 2, product = "SM-X230", address = "", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|"),
            choice(id = 2, type = 2, product = "SM-X230", address = "0", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|0"),
            choice(id = 3, type = 2, product = "SM-X230", address = "back", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|back"),
            choice(id = 4, type = 2, product = "SM-X230", address = "bottom", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|bottom"),
        )

        val result = StudioAudioRoutePolicy.canonicalizeOutputs(raw)

        assertEquals(1, result.size)
        assertEquals("Alto-falante do tablet", result.single().label)
        assertEquals(listOf(1, 2, 3, 4), result.single().candidateDeviceIds)
        assertTrue(result.single().signature.startsWith("route3:builtin-speaker:"))
    }

    @Test
    fun legacyBuiltInSpeakerEndpointMigratesToCanonicalRoute() {
        val raw = listOf(
            choice(id = 2, type = 2, product = "SM-X230", address = "0", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|0"),
            choice(id = 4, type = 2, product = "SM-X230", address = "bottom", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|bottom"),
        )

        val first = StudioAudioRoutePolicy.canonicalSignature(raw, "2|SM-X230|0")
        val second = StudioAudioRoutePolicy.canonicalSignature(raw, "2|SM-X230|bottom")

        assertEquals(first, second)
        assertTrue(first.orEmpty().startsWith("route3:builtin-speaker:"))
    }

    @Test
    fun earpieceAndBuiltInSpeakerRemainDistinctRoutes() {
        val speaker = choice(id = 1, type = 2, product = "Phone", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "speaker")
        val earpiece = choice(id = 2, type = 1, product = "Phone", family = "type:1", signature = "earpiece")

        val result = StudioAudioRoutePolicy.canonicalizeOutputs(listOf(speaker, earpiece))

        assertEquals(2, result.size)
        assertEquals(setOf("Phone", "Alto-falante do tablet"), result.map { it.label }.toSet())
        assertTrue(result.any { it.signature == "earpiece" })
        assertTrue(result.any { it.signature.startsWith("route3:builtin-speaker:") })
    }

    @Test
    fun nonUsbProfilesAreNeverCollapsedByMatchingLabel() {
        val a2dp = choice(id = 7, type = 8, family = "type:8", signature = "a2dp")
        val sco = choice(id = 8, type = 7, family = "type:7", signature = "sco")

        val result = StudioAudioRoutePolicy.canonicalizeOutputs(listOf(a2dp, sco))

        assertEquals(2, result.size)
        assertEquals(setOf("a2dp", "sco"), result.map { it.signature }.toSet())
    }

    @Test
    fun compatibilityRankingPrefersStudioStereo48kEndpointWithoutDependingOnOrder() {
        val weak = choice(id = 1, type = 11, rates = listOf(44_100), channels = listOf(1))
        val studio = choice(id = 2, type = 22, rates = listOf(48_000), channels = listOf(2))

        assertTrue(StudioAudioRoutePolicy.compatibilityScore(studio) > StudioAudioRoutePolicy.compatibilityScore(weak))
    }


    @Test
    fun builtInMicrophoneLogicalEndpointsCollapseToOneFriendlyPhysicalChoice() {
        val raw = listOf(
            choice(id = 10, type = 15, product = "SM-X230", address = "", family = StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY, signature = "15|SM-X230|"),
            choice(id = 11, type = 21, product = "SM-X230", address = "bottom", family = StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY, signature = "21|SM-X230|bottom"),
            choice(id = 12, type = 21, product = "SM-X230", address = "back", family = StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY, signature = "21|SM-X230|back"),
        )

        val result = StudioAudioRoutePolicy.canonicalizeInputs(raw)

        assertEquals(1, result.size)
        assertEquals("Microfone do tablet", result.single().label)
        assertEquals(listOf(10, 11, 12), result.single().candidateDeviceIds)
        assertTrue(result.single().signature.startsWith("route3:builtin-mic:"))
    }

    @Test
    fun systemOnlyRemoteSubmixNeverAppearsInUserFacingChoices() {
        val hidden = choice(id = 90, type = 25, product = "remote-submix", address = "hsp:24927u:10213m:7", family = StudioAudioRoutePolicy.HIDDEN_SYSTEM_FAMILY, signature = "hidden")
        val microphone = choice(id = 10, type = 15, product = "SM-X230", family = StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY, signature = "mic")
        val speaker = choice(id = 20, type = 2, product = "SM-X230", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "speaker")

        assertEquals(listOf("Microfone do tablet"), StudioAudioRoutePolicy.canonicalizeInputs(listOf(hidden, microphone)).map { it.label })
        assertEquals(listOf("Alto-falante do tablet"), StudioAudioRoutePolicy.canonicalizeOutputs(listOf(hidden, speaker)).map { it.label })
    }

    @Test
    fun usbLogicalEndpointsOnSameCardCollapseEvenWhenEndpointAddressDiffers() {
        val first = choice(id = 41, type = 11, address = "card=2;device=0", signature = "a")
        val second = choice(id = 42, type = 22, address = "card=2;device=1", signature = "b")

        val result = StudioAudioRoutePolicy.canonicalizeOutputs(listOf(first, second))

        assertEquals(1, result.size)
        assertEquals("MK-300", result.single().label)
        assertEquals(listOf(41, 42), result.single().candidateDeviceIds)
    }

    @Test
    fun oldRoute2SignatureMigratesToRoute3WithoutLosingSelection() {
        val raw = listOf(
            choice(id = 2, type = 2, product = "SM-X230", address = "0", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|0"),
            choice(id = 4, type = 2, product = "SM-X230", address = "bottom", family = StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY, signature = "2|SM-X230|bottom"),
        )
        val route2 = StudioAudioRoutePolicy.canonicalizeOutputs(raw).single().legacySignatures.first { it.startsWith("route2:") }

        val migrated = StudioAudioRoutePolicy.canonicalSignature(raw, route2)

        assertTrue(migrated.orEmpty().startsWith("route3:builtin-speaker:"))
    }

    private fun choice(
        id: Int,
        type: Int,
        rates: List<Int> = emptyList(),
        channels: List<Int> = emptyList(),
        address: String = "",
        family: String = StudioAudioRoutePolicy.USB_FAMILY,
        product: String = "MK-300",
        signature: String = "$type|$product|$address",
    ) = StudioAudioDeviceChoice(
        signature = signature,
        label = if (address.isBlank()) product else "$product • $address",
        deviceId = id,
        type = type,
        channelCounts = channels,
        sampleRates = rates,
        productName = product,
        address = address,
        transportFamily = family,
        legacySignatures = listOf(signature),
    )
}
