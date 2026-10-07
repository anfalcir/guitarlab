package studio.guitarlab.platform.audio.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CueRuntimeAlignmentTest {
    @Test
    fun warmupFeedsOnlyWhileOutstandingAudioIsBelowTarget() {
        assertTrue(CueRuntimeWarmupPolicy.shouldFeed(512, 0, 1024))
        assertTrue(CueRuntimeWarmupPolicy.shouldFeed(1536, 1024, 1024))
        assertFalse(CueRuntimeWarmupPolicy.shouldFeed(2048, 1024, 1024))
    }

    @Test
    fun warmupRequiresBothAcceptedFramesToBePresentedBeforeItIsDrained() {
        assertFalse(CueRuntimeWarmupPolicy.drained(2048, 2047))
        assertTrue(CueRuntimeWarmupPolicy.drained(2048, 2048))
        assertTrue(CueRuntimeWarmupPolicy.drained(2048, 4096))
    }

    @Test
    fun fixedPipelineOffsetBecomesBaselineInsteadOfFalseDrift() {
        val monitor = CueRelativeDriftMonitor(
            minPresentedFrames = 2_000,
            maxRelativeDriftFrames = 2_646,
            failureGraceNs = 750_000_000L,
        )
        val preArm = monitor.observe(1_000, 5_000, 0L)
        assertFalse(preArm.armed)
        assertNull(preArm.baselineDeltaFrames)

        val armed = monitor.observe(3_000, 7_000, 10L)
        assertTrue(armed.armed)
        assertEquals(4_000L, armed.baselineDeltaFrames)
        assertEquals(0L, armed.relativeDriftFrames)

        val stable = monitor.observe(20_000, 24_100, 500_000_000L)
        assertFalse(stable.unsafe)
        assertFalse(stable.failed)
        assertEquals(100L, stable.relativeDriftFrames)
    }

    @Test
    fun onlySustainedMovementAwayFromBaselineFails() {
        val monitor = CueRelativeDriftMonitor(
            minPresentedFrames = 1_000,
            maxRelativeDriftFrames = 2_000,
            failureGraceNs = 750_000_000L,
        )
        monitor.observe(2_000, 2_500, 0L)

        val firstUnsafe = monitor.observe(10_000, 12_600, 100_000_000L)
        assertTrue(firstUnsafe.unsafe)
        assertFalse(firstUnsafe.failed)
        assertEquals(2_100L, firstUnsafe.relativeDriftFrames)

        val recovered = monitor.observe(20_000, 20_600, 500_000_000L)
        assertFalse(recovered.unsafe)
        assertFalse(recovered.failed)

        val secondUnsafe = monitor.observe(30_000, 32_700, 600_000_000L)
        assertTrue(secondUnsafe.unsafe)
        assertFalse(secondUnsafe.failed)

        val failed = monitor.observe(50_000, 52_700, 1_350_000_000L)
        assertTrue(failed.failed)
    }

    @Test
    fun resetRequiresANewBaselineAfterSeek() {
        val monitor = CueRelativeDriftMonitor(1_000, 2_000, 100L)
        monitor.observe(2_000, 2_500, 0L)
        monitor.reset()
        val afterReset = monitor.observe(500, 4_000, 10L)
        assertFalse(afterReset.armed)
        assertNull(afterReset.baselineDeltaFrames)
    }
}
