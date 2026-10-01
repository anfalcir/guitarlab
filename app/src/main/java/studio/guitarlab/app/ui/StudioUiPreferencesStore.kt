package studio.guitarlab.app.ui

import android.content.Context

class StudioUiPreferencesStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Mixer visibility is a durable workspace preference, not a temporary/pinned mode. */
    fun mixerVisible(): Boolean = preferences.getBoolean(
        KEY_MIXER_VISIBLE,
        preferences.getBoolean(LEGACY_KEY_MIXER_PINNED, false),
    )

    fun setMixerVisible(visible: Boolean) {
        preferences.edit()
            .putBoolean(KEY_MIXER_VISIBLE, visible)
            .remove(LEGACY_KEY_MIXER_PINNED)
            .apply()
    }

    fun mixerPinned(): Boolean = preferences.getBoolean(KEY_MIXER_PINNED, true)

    fun setMixerPinned(pinned: Boolean) {
        preferences.edit().putBoolean(KEY_MIXER_PINNED, pinned).apply()
    }

    fun mixerExpanded(): Boolean = preferences.getBoolean(KEY_MIXER_EXPANDED, false)

    fun setMixerExpanded(expanded: Boolean) {
        preferences.edit().putBoolean(KEY_MIXER_EXPANDED, expanded).apply()
    }

    private companion object {
        const val PREFS_NAME = "studio_ui_preferences"
        const val KEY_MIXER_PINNED = "mixer_dock_pinned"
        const val KEY_MIXER_EXPANDED = "mixer_expanded"
        const val KEY_MIXER_VISIBLE = "mixer_visible"
        const val LEGACY_KEY_MIXER_PINNED = "mixer_pinned"
    }
}
