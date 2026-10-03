package studio.guitarlab.core.audio

import org.junit.Assert.*
import org.junit.Test

class CueStartupProbeTest {
    private class Fixture {
        var elapsed = 0L
        var mainFeeds = 0
        var cueFeeds = 0
        var cueOffset = 0L
        var converges = false
        var missingRoute = false
        var routeAvailableAfterNs = 0L
        var staleClock = false
        var cueWrite = 512
        private fun sink(id: Int) = object : CueProbeOutput {
            override fun feedSilence(): Int {
                if (id == 1) mainFeeds++ else cueFeeds++
                return if (id == 1) 512 else cueWrite
            }
            override fun routedDeviceId(): Int? = if (missingRoute || elapsed < routeAvailableAfterNs) null else if (id == 2 && converges && elapsed >= 64_000_000) 1 else id
            override fun clockObservation(): AudioClockObservation? {
                if (elapsed < 200_000_000) return null
                // Driver publishes one fresh timestamp every 64 ms, not every polling iteration.
                val frames = if (staleClock) 3072L else elapsed / 64_000_000 * 3072
                return AudioClockObservation(frames, 1_000_000_000 + frames * 1_000_000_000 / 48_000 + if (id == 2) cueOffset else 0)
            }
        }
        fun verify(cancelAt: Long = Long.MAX_VALUE) = CueStartupProbe.verify(sink(1), sink(2), 1, 2, 48_000,
            nowNs = { 1_000_000_000 + elapsed }, sleepMs = { elapsed += it * 1_000_000 }, keepRunning = { elapsed < cancelAt })
    }

    @Test fun keepsBothStreamsFedAcrossDelayedAndRepeatedTimestamps() {
        val f = Fixture()
        assertNull(f.verify())
        assertTrue(f.elapsed >= 320_000_000)
        assertEquals(f.mainFeeds, f.cueFeeds)
        assertTrue(f.mainFeeds > 30)
    }
    @Test fun routeSettlementHasItsOwnBoundBeyondTheOldOnePointFiveSecondWindow() {
        val f = Fixture().apply { routeAvailableAfterNs = 2_200_000_000L }
        assertNull(f.verify())
        assertTrue(f.elapsed >= f.routeAvailableAfterNs)
        assertTrue(f.elapsed < CueStartupProbe.TIMEOUT_NS)
    }
    @Test fun partialAndZeroStartupWritesAreRetriedWithoutBlocking() {
        val f = Fixture().apply { cueWrite = 0 }
        // Hardware timestamps are authoritative; a full queue may legitimately accept zero.
        assertNull(f.verify())
    }
    @Test fun missingOrConvergedRoutesNeverAdmitCue() {
        assertEquals(CueStartupFailure.ROUTE_UNCONFIRMED, Fixture().apply { missingRoute = true }.verify())
        assertEquals(CueStartupFailure.ROUTE_CHANGED, Fixture().apply { converges = true }.verify())
    }
    @Test fun staleClockAndLargeOffsetRemainRejected() {
        assertEquals(CueStartupFailure.CLOCK_UNSTABLE, Fixture().apply { staleClock = true }.verify())
        assertEquals(CueStartupFailure.OFFSET_EXCEEDED, Fixture().apply { cueOffset = 30_000_000 }.verify())
    }
    @Test fun cancellationAndWriteErrorsExitWithinTheBound() {
        val f = Fixture()
        assertEquals(CueStartupFailure.CANCELLED, f.verify(cancelAt = 80_000_000))
        assertTrue(f.elapsed < 100_000_000)
        assertEquals(CueStartupFailure.WRITE_FAILED, Fixture().apply { cueWrite = -1 }.verify())
    }
}
