package studio.guitarlab.app.ui

import android.content.Context
import studio.guitarlab.core.project.ExternalControlAction
import studio.guitarlab.core.project.ExternalControlToken

data class ExternalControlStoredMapping(
    val action: ExternalControlAction,
    val tokenKey: String,
    val label: String,
)

class ExternalControlStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    fun setEnabled(value: Boolean) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }
    fun hidEnabled(): Boolean = prefs.getBoolean(KEY_HID_ENABLED, false)
    fun setHidEnabled(value: Boolean) { prefs.edit().putBoolean(KEY_HID_ENABLED, value).apply() }

    fun mappings(): List<ExternalControlStoredMapping> = ExternalControlAction.entries.mapNotNull { action ->
        val key = prefs.getString(mappingKey(action), null)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        ExternalControlStoredMapping(action, key, prefs.getString(labelKey(action), null).orEmpty().ifBlank { key })
    }

    fun actionFor(token: ExternalControlToken): ExternalControlAction? {
        val key = token.stableKey()
        return mappings().firstOrNull { it.tokenKey == key }?.action
    }

    fun setMapping(action: ExternalControlAction, token: ExternalControlToken, label: String) {
        val tokenKey = token.stableKey()
        val editor = prefs.edit()
        // One physical gesture maps to one action. This makes dispatch deterministic after migration/reconnect.
        ExternalControlAction.entries.forEach { existing ->
            if (prefs.getString(mappingKey(existing), null) == tokenKey) {
                editor.remove(mappingKey(existing)).remove(labelKey(existing))
            }
        }
        editor.putString(mappingKey(action), tokenKey).putString(labelKey(action), label.take(120)).apply()
    }

    fun clearMapping(action: ExternalControlAction) {
        prefs.edit().remove(mappingKey(action)).remove(labelKey(action)).apply()
    }

    private fun mappingKey(action: ExternalControlAction) = "mapping.${action.name}"
    private fun labelKey(action: ExternalControlAction) = "mapping.${action.name}.label"

    private companion object {
        const val PREFS = "external-control-v1"
        const val KEY_ENABLED = "enabled"
        const val KEY_HID_ENABLED = "hid-enabled"
    }
}
