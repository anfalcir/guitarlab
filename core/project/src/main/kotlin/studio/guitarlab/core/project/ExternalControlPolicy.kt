package studio.guitarlab.core.project

enum class ExternalControlAction {
    PLAY_STOP,
    RECORD_TOGGLE,
    RETURN_TO_START,
    LOOP_TOGGLE,
    UNDO,
    REDO,
}

data class ExternalControlCommandHandlers(
    val playStop: () -> Unit,
    val recordToggle: () -> Unit,
    val returnToStart: () -> Unit,
    val loopToggle: () -> Unit,
    val undo: () -> Unit,
    val redo: () -> Unit,
)

/** One parity-preserving dispatcher shared by on-screen commands and external-control integration. */
object ExternalControlCommandDispatcher {
    fun dispatch(action: ExternalControlAction, handlers: ExternalControlCommandHandlers) {
        when (action) {
            ExternalControlAction.PLAY_STOP -> handlers.playStop()
            ExternalControlAction.RECORD_TOGGLE -> handlers.recordToggle()
            ExternalControlAction.RETURN_TO_START -> handlers.returnToStart()
            ExternalControlAction.LOOP_TOGGLE -> handlers.loopToggle()
            ExternalControlAction.UNDO -> handlers.undo()
            ExternalControlAction.REDO -> handlers.redo()
        }
    }
}

enum class ExternalControlSource { MIDI, HID }
enum class ExternalControlType { MIDI_NOTE, MIDI_CC, HID_KEY }
enum class ExternalControlPhase { PRESS, RELEASE }

data class ExternalControlToken(
    val source: ExternalControlSource,
    val deviceDescriptor: String,
    val type: ExternalControlType,
    val channel: Int? = null,
    val number: Int,
) {
    init {
        require(deviceDescriptor.isNotBlank())
        require(channel == null || channel in 0..15)
        require(number >= 0)
    }

    fun stableKey(): String = listOf(
        source.name,
        deviceDescriptor.trim().lowercase(),
        type.name,
        channel?.toString() ?: "-",
        number.toString(),
    ).joinToString("|")
}

data class ExternalControlInputEvent(
    val token: ExternalControlToken,
    val phase: ExternalControlPhase,
)

/** Stateful gate that converts press/release streams into one deterministic trigger per gesture. */
class ExternalControlTriggerGate(private val minimumIntervalMs: Long = 120L) {
    init { require(minimumIntervalMs >= 0L) }
    private val held = mutableSetOf<String>()
    private val lastTriggerAt = mutableMapOf<String, Long>()

    @Synchronized
    fun accept(event: ExternalControlInputEvent, nowEpochMs: Long): Boolean {
        val key = event.token.stableKey()
        return when (event.phase) {
            ExternalControlPhase.RELEASE -> {
                held.remove(key)
                false
            }
            ExternalControlPhase.PRESS -> {
                if (!held.add(key)) return false
                val prior = lastTriggerAt[key]
                if (prior != null && nowEpochMs - prior < minimumIntervalMs) return false
                lastTriggerAt[key] = nowEpochMs
                true
            }
        }
    }

    @Synchronized fun clear() { held.clear(); lastTriggerAt.clear() }
}

/** Incremental MIDI channel-message parser. Unsupported/system traffic is ignored fail-closed. */
class MidiControlStreamParser(private val deviceDescriptor: String) {
    init { require(deviceDescriptor.isNotBlank()) }

    private var runningStatus: Int? = null
    private val pendingData = ArrayList<Int>(2)
    private var inSysEx = false
    private var systemDataBytesRemaining = 0

    @Synchronized
    fun feed(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size - offset): List<ExternalControlInputEvent> {
        require(offset >= 0 && count >= 0 && offset + count <= bytes.size)
        val events = mutableListOf<ExternalControlInputEvent>()
        for (index in offset until offset + count) {
            val value = bytes[index].toInt() and 0xFF
            if (value >= 0xF8) continue // realtime may appear anywhere and never disrupts running status
            if (inSysEx) {
                if (value == 0xF7) inSysEx = false
                continue
            }
            if (systemDataBytesRemaining > 0 && value < 0x80) {
                systemDataBytesRemaining--
                continue
            }
            if (value >= 0x80) {
                when {
                    value == 0xF0 -> {
                        inSysEx = true
                        runningStatus = null
                        pendingData.clear()
                        systemDataBytesRemaining = 0
                    }
                    value in 0xF1..0xF7 -> {
                        runningStatus = null
                        pendingData.clear()
                        systemDataBytesRemaining = when (value) {
                            0xF1, 0xF3 -> 1
                            0xF2 -> 2
                            else -> 0
                        }
                    }
                    else -> {
                        runningStatus = value
                        pendingData.clear()
                    }
                }
                continue
            }

            val status = runningStatus ?: continue
            pendingData += value
            val needed = dataBytesFor(status)
            if (needed <= 0) {
                pendingData.clear()
                continue
            }
            if (pendingData.size >= needed) {
                decode(status, pendingData, events)
                pendingData.clear()
            }
        }
        return events
    }

    private fun dataBytesFor(status: Int): Int = when (status and 0xF0) {
        0x80, 0x90, 0xA0, 0xB0, 0xE0 -> 2
        0xC0, 0xD0 -> 1
        else -> 0
    }

    private fun decode(status: Int, data: List<Int>, out: MutableList<ExternalControlInputEvent>) {
        val family = status and 0xF0
        val channel = status and 0x0F
        when (family) {
            0x80 -> out += event(ExternalControlType.MIDI_NOTE, channel, data[0], ExternalControlPhase.RELEASE)
            0x90 -> out += event(
                ExternalControlType.MIDI_NOTE,
                channel,
                data[0],
                if (data[1] == 0) ExternalControlPhase.RELEASE else ExternalControlPhase.PRESS,
            )
            0xB0 -> out += event(
                ExternalControlType.MIDI_CC,
                channel,
                data[0],
                if (data[1] >= 64) ExternalControlPhase.PRESS else ExternalControlPhase.RELEASE,
            )
        }
    }

    private fun event(type: ExternalControlType, channel: Int, number: Int, phase: ExternalControlPhase) =
        ExternalControlInputEvent(
            token = ExternalControlToken(ExternalControlSource.MIDI, deviceDescriptor, type, channel, number),
            phase = phase,
        )
}
