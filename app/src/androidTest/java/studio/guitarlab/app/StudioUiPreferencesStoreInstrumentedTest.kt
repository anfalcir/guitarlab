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
    fun minimumPersistsIndependentlyAndLegacyHeightStartsComplete() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = context.getSharedPreferences("studio_ui_preferences", android.content.Context.MODE_PRIVATE)
        val previous = raw.all
        try {
            raw.edit().clear().putBoolean("mixer_expanded", false).putBoolean("mixer_dock_pinned", false)
                .putBoolean("mixer_pinned", true).commit()
            val store = StudioUiPreferencesStore(context)
            assertTrue("Legacy visibility must survive", store.mixerVisible())
            assertFalse("Old height flag must not hide controls on upgrade", store.mixerMinimal())
            store.setMixerMinimal(true)
            store.setMixerVisible(false)
            assertTrue(StudioUiPreferencesStore(context).mixerMinimal())
            assertFalse(StudioUiPreferencesStore(context).mixerVisible())
            store.setMixerVisible(true)
            assertTrue(StudioUiPreferencesStore(context).mixerMinimal())
            assertFalse(raw.contains("mixer_dock_pinned"))
            assertFalse(raw.contains("mixer_expanded"))
        } finally {
            raw.edit().clear().also { editor ->
                previous.forEach { (key, value) -> if (value is Boolean) editor.putBoolean(key, value) }
            }.commit()
        }
    }
}
