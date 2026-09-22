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
}
