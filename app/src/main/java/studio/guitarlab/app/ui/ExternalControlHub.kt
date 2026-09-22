package studio.guitarlab.app.ui

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.guitarlab.core.project.ExternalControlAction
import studio.guitarlab.core.project.ExternalControlInputEvent
import studio.guitarlab.core.project.ExternalControlPhase
import studio.guitarlab.core.project.ExternalControlSource
import studio.guitarlab.core.project.ExternalControlToken
import studio.guitarlab.core.project.ExternalControlTriggerGate
import studio.guitarlab.core.project.ExternalControlType

data class ExternalControlHubState(
    val enabled: Boolean = false,
    val hidEnabled: Boolean = false,
    val midiDevices: List<ExternalMidiDeviceStatus> = emptyList(),
    val mappings: List<ExternalControlStoredMapping> = emptyList(),
    val learningAction: ExternalControlAction? = null,
    val lastLearnedLabel: String? = null,
)

/** Application-local event hub. It emits app actions only after mapping + debounce. */
object ExternalControlHub {
    private val _state = MutableStateFlow(ExternalControlHubState())
    val state: StateFlow<ExternalControlHubState> = _state.asStateFlow()
    private val _actions = MutableSharedFlow<ExternalControlAction>(extraBufferCapacity = 32)
    val actions: SharedFlow<ExternalControlAction> = _actions.asSharedFlow()
    private val gate = ExternalControlTriggerGate()
    private var appContext: Context? = null
    private var store: ExternalControlStore? = null
    private var midi: AndroidExternalMidiRuntime? = null
    @Volatile private var foreground = false

    @Synchronized fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            store = ExternalControlStore(context)
            midi = AndroidExternalMidiRuntime(context, ::onInput) { devices ->
                _state.value = _state.value.copy(midiDevices = devices)
            }
        }
        refreshState()
        if (store?.enabled() == true) midi?.start() else midi?.stop()
    }

    @Synchronized fun setForeground(value: Boolean) {
        foreground = value
        if (!value) gate.clear()
    }

    @Synchronized fun setEnabled(context: Context, enabled: Boolean) {
        initialize(context)
        store?.setEnabled(enabled)
        gate.clear()
        if (enabled) midi?.start() else midi?.stop()
        if (!enabled) _state.value = _state.value.copy(learningAction = null)
        refreshState()
    }

    @Synchronized fun setHidEnabled(context: Context, enabled: Boolean) {
        initialize(context)
        store?.setHidEnabled(enabled)
        gate.clear()
        refreshState()
    }

    @Synchronized fun beginLearn(context: Context, action: ExternalControlAction) {
        initialize(context)
        if (store?.enabled() != true) return
        gate.clear()
        _state.value = _state.value.copy(learningAction = action, lastLearnedLabel = null)
    }

    @Synchronized fun cancelLearn() { _state.value = _state.value.copy(learningAction = null) }

    @Synchronized fun clearMapping(context: Context, action: ExternalControlAction) {
        initialize(context)
        store?.clearMapping(action)
        refreshState()
    }

    fun hidToken(event: KeyEvent): ExternalControlToken? {
        if (event.keyCode == KeyEvent.KEYCODE_UNKNOWN) return null
        val input = InputDevice.getDevice(event.deviceId)
        val raw = listOf(
            input?.vendorId ?: 0,
            input?.productId ?: 0,
            input?.name.orEmpty(),
            input?.descriptor.orEmpty(),
            input?.sources ?: 0,
        ).joinToString("|").lowercase()
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        return ExternalControlToken(ExternalControlSource.HID, "hid:$digest", ExternalControlType.HID_KEY, number = event.keyCode)
    }

    fun shouldCaptureHid(context: Context, token: ExternalControlToken): Boolean {
        initialize(context)
        val localStore = store ?: return false
        return localStore.enabled() && localStore.hidEnabled() &&
            (_state.value.learningAction != null || localStore.actionFor(token) != null)
    }

    fun onHidEvent(context: Context, token: ExternalControlToken, event: KeyEvent) {
        initialize(context)
        val phase = when (event.action) {
            KeyEvent.ACTION_DOWN -> ExternalControlPhase.PRESS
            KeyEvent.ACTION_UP -> ExternalControlPhase.RELEASE
            else -> return
        }
        onInput(ExternalControlInputEvent(token, phase), "Pedal/tecla ${KeyEvent.keyCodeToString(event.keyCode)}")
    }

    @Synchronized private fun onInput(event: ExternalControlInputEvent, label: String) {
        val localStore = store ?: return
        if (!localStore.enabled() || !foreground) return
        val acceptedPress = gate.accept(event, System.currentTimeMillis())
        if (!acceptedPress) return
        val learning = _state.value.learningAction
        if (learning != null) {
            localStore.setMapping(learning, event.token, label)
            _state.value = _state.value.copy(learningAction = null, lastLearnedLabel = label)
            refreshState()
            return
        }
        localStore.actionFor(event.token)?.let { _actions.tryEmit(it) }
    }

    @Synchronized private fun refreshState() {
        val local = store ?: return
        _state.value = _state.value.copy(
            enabled = local.enabled(),
            hidEnabled = local.hidEnabled(),
            mappings = local.mappings(),
        )
    }
}
