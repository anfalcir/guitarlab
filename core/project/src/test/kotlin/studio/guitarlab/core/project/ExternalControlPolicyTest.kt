package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalControlPolicyTest {
    @Test fun fragmentedNoteAndRunningStatusAreParsed() {
        val parser = MidiControlStreamParser("mvave-chocolate")
        assertTrue(parser.feed(byteArrayOf(0x90.toByte(), 60)).isEmpty())
        val first = parser.feed(byteArrayOf(100)).single()
        assertEquals(ExternalControlPhase.PRESS, first.phase)
        assertEquals(60, first.token.number)
        val running = parser.feed(byteArrayOf(61, 100, 61, 0))
        assertEquals(listOf(ExternalControlPhase.PRESS, ExternalControlPhase.RELEASE), running.map { it.phase })
        assertEquals(listOf(61, 61), running.map { it.token.number })
    }

    @Test fun noteOnVelocityZeroIsReleaseAndNoteOffIsRelease() {
        val parser = MidiControlStreamParser("controller")
        val events = parser.feed(byteArrayOf(0x90.toByte(), 7, 0, 0x80.toByte(), 8, 100))
        assertEquals(2, events.size)
        assertTrue(events.all { it.phase == ExternalControlPhase.RELEASE })
    }

    @Test fun ccUsesDeterministicThresholdAndRealtimeDoesNotBreakRunningStatus() {
        val parser = MidiControlStreamParser("controller")
        val events = parser.feed(byteArrayOf(0xB0.toByte(), 20, 127.toByte(), 0xF8.toByte(), 20, 0))
        assertEquals(listOf(ExternalControlPhase.PRESS, ExternalControlPhase.RELEASE), events.map { it.phase })
        assertTrue(events.all { it.token.type == ExternalControlType.MIDI_CC })
    }

    @Test fun sysexAndUnsupportedMessagesAreIgnoredWithoutCreatingGhostControls() {
        val parser = MidiControlStreamParser("controller")
        val events = parser.feed(byteArrayOf(
            0xF0.toByte(), 1, 2, 3, 0xF7.toByte(),
            0xC0.toByte(), 9,
            0xD0.toByte(), 10,
            0x90.toByte(), 64, 100,
        ))
        assertEquals(1, events.size)
        assertEquals(64, events.single().token.number)
    }

    @Test fun heldAndBouncedEventsTriggerOnlyOnceUntilRelease() {
        val token = ExternalControlToken(ExternalControlSource.HID, "pedal", ExternalControlType.HID_KEY, number = 66)
        val gate = ExternalControlTriggerGate(minimumIntervalMs = 120)
        assertTrue(gate.accept(ExternalControlInputEvent(token, ExternalControlPhase.PRESS), 1_000))
        assertFalse(gate.accept(ExternalControlInputEvent(token, ExternalControlPhase.PRESS), 1_010))
        assertFalse(gate.accept(ExternalControlInputEvent(token, ExternalControlPhase.RELEASE), 1_020))
        assertFalse(gate.accept(ExternalControlInputEvent(token, ExternalControlPhase.PRESS), 1_050))
        assertFalse(gate.accept(ExternalControlInputEvent(token, ExternalControlPhase.RELEASE), 1_060))
        assertTrue(gate.accept(ExternalControlInputEvent(token, ExternalControlPhase.PRESS), 1_130))
    }

    @Test fun stableTokenNeverContainsTransientAndroidNumericDeviceIdConcept() {
        val a = ExternalControlToken(ExternalControlSource.MIDI, "m-vave|chocolate|port-a", ExternalControlType.MIDI_NOTE, 0, 10)
        val b = ExternalControlToken(ExternalControlSource.MIDI, "m-vave|chocolate|port-a", ExternalControlType.MIDI_NOTE, 0, 10)
        assertEquals(a.stableKey(), b.stableKey())
    }
    @Test fun commandDispatcherInvokesExactlyTheMatchingExistingCommand() {
        val counts = linkedMapOf<ExternalControlAction, Int>().withDefault { 0 }
        val handlers = ExternalControlCommandHandlers(
            playStop = { counts[ExternalControlAction.PLAY_STOP] = counts.getValue(ExternalControlAction.PLAY_STOP) + 1 },
            recordToggle = { counts[ExternalControlAction.RECORD_TOGGLE] = counts.getValue(ExternalControlAction.RECORD_TOGGLE) + 1 },
            returnToStart = { counts[ExternalControlAction.RETURN_TO_START] = counts.getValue(ExternalControlAction.RETURN_TO_START) + 1 },
            loopToggle = { counts[ExternalControlAction.LOOP_TOGGLE] = counts.getValue(ExternalControlAction.LOOP_TOGGLE) + 1 },
            undo = { counts[ExternalControlAction.UNDO] = counts.getValue(ExternalControlAction.UNDO) + 1 },
            redo = { counts[ExternalControlAction.REDO] = counts.getValue(ExternalControlAction.REDO) + 1 },
        )
        ExternalControlAction.entries.forEach { action ->
            counts.clear()
            ExternalControlCommandDispatcher.dispatch(action, handlers)
            assertEquals(1, counts[action])
            assertEquals(1, counts.values.sum())
        }
    }

}
