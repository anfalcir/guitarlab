package studio.guitarlab.platform.audio.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AndroidOutputRouteIdentityTest {
    @Test
    fun usbLogicalEndpointsOnSameCardShareOnePhysicalIdentity() {
        val usbDevice = AndroidOutputRouteIdentity.physicalKey(11, "USB-Audio - MK300", "card=2;device=0")
        val usbHeadset = AndroidOutputRouteIdentity.physicalKey(22, "USB-Audio - MK300", "card=2;device=1")
        assertEquals(usbDevice, usbHeadset)
    }

    @Test
    fun differentUsbCardsRemainDistinct() {
        val first = AndroidOutputRouteIdentity.physicalKey(11, "USB-Audio - MK300", "card=2;device=0")
        val second = AndroidOutputRouteIdentity.physicalKey(22, "USB-Audio - MK300", "card=3;device=0")
        assertNotEquals(first, second)
    }

    @Test
    fun builtInSpeakerLogicalAddressesDoNotCreateFakePhysicalRoutes() {
        val first = AndroidOutputRouteIdentity.physicalKey(2, "SM-X230", "")
        val second = AndroidOutputRouteIdentity.physicalKey(2, "SM-X230", "bottom")
        assertEquals(first, second)
    }

    @Test
    fun headsetAndHeadphonesModesShareTheSamePhysicalJack() {
        val headset = AndroidOutputRouteIdentity.physicalKey(3, "Wired", "")
        val headphones = AndroidOutputRouteIdentity.physicalKey(4, "Wired", "")
        assertEquals(headset, headphones)
    }

    @Test
    fun routeSetMustContainOnlyTheExpectedPhysicalDestination() {
        val expected = AndroidOutputRouteIdentity.physicalKey(11, "USB-Audio - MK300", "card=2;device=0")
        val alias = AndroidOutputRouteIdentity.physicalKey(22, "USB-Audio - MK300", "card=2;device=1")
        val wired = AndroidOutputRouteIdentity.physicalKey(4, "Wired", "")

        assertTrue(AndroidOutputRouteIdentity.routesOnlyToExpected(expected, listOf(expected, alias)))
        assertFalse(AndroidOutputRouteIdentity.routesOnlyToExpected(expected, emptyList()))
        assertFalse(AndroidOutputRouteIdentity.routesOnlyToExpected(expected, listOf(expected, wired)))
    }
}
