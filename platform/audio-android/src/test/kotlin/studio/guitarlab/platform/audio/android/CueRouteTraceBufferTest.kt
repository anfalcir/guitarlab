package studio.guitarlab.platform.audio.android

import org.junit.Assert.assertEquals
import org.junit.Test

class CueRouteTraceBufferTest {
    @Test
    fun retainsOnlyBoundedRouteTransitionsInOrder() {
        val buffer = CueRouteTraceBuffer(capacity = 3)
        fun sample(index: Int, cue: String) = CueRouteTraceSample(
            elapsedMs = index.toLong(),
            mainWriteResult = 512,
            cueWriteResult = 512,
            mainPhysicalKeys = setOf("main"),
            cuePhysicalKeys = setOf(cue),
        )

        buffer.record(sample(0, "cue-a"))
        buffer.record(sample(1, "cue-a")) // Same route is not duplicated.
        buffer.record(sample(2, "cue-b"))
        buffer.record(sample(3, "cue-c"))
        buffer.record(sample(4, "cue-d"))

        assertEquals(listOf(2L, 3L, 4L), buffer.snapshot().map { it.elapsedMs })
        assertEquals(listOf("cue-b", "cue-c", "cue-d"), buffer.snapshot().map { it.cuePhysicalKeys.single() })
    }
}
