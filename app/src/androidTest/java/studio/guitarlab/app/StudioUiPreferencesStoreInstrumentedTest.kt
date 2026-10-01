package studio.guitarlab.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.StudioUiPreferencesStore

@RunWith(AndroidJUnit4::class)
class StudioUiPreferencesStoreInstrumentedTest {
    @Test
    fun mixerVisibilityPersistsAcrossStoreInstances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = StudioUiPreferencesStore(context)
        try {
            first.setMixerVisible(true)
            assertTrue(StudioUiPreferencesStore(context).mixerVisible())
            first.setMixerVisible(false)
            assertFalse(StudioUiPreferencesStore(context).mixerVisible())
        } finally {
            first.setMixerVisible(false)
        }
    }
    @Test
    fun pinAndHeightPersistIndependentlyOfVisibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = StudioUiPreferencesStore(context)
        val previousVisible = store.mixerVisible()
        val previousPinned = store.mixerPinned()
        val previousExpanded = store.mixerExpanded()
        try {
            store.setMixerPinned(true)
            store.setMixerExpanded(true)
            store.setMixerVisible(false)
            val reopened = StudioUiPreferencesStore(context)
            assertFalse(reopened.mixerVisible())
            assertTrue(reopened.mixerPinned())
            assertTrue(reopened.mixerExpanded())
            reopened.setMixerPinned(false)
            assertFalse(StudioUiPreferencesStore(context).mixerPinned())
        } finally {
            store.setMixerVisible(previousVisible)
            store.setMixerPinned(previousPinned)
            store.setMixerExpanded(previousExpanded)
        }
    }

}
