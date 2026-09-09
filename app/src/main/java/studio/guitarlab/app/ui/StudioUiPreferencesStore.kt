package studio.guitarlab.app.ui

import android.content.Context

class StudioUiPreferencesStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun mixerPinned(): Boolean = preferences.getBoolean(KEY_MIXER_PINNED, false)

    fun setMixerPinned(pinned: Boolean) {
        preferences.edit().putBoolean(KEY_MIXER_PINNED, pinned).apply()
    }

    private companion object {
        const val PREFS_NAME = "studio_ui_preferences"
        const val KEY_MIXER_PINNED = "mixer_pinned"
    }
}
