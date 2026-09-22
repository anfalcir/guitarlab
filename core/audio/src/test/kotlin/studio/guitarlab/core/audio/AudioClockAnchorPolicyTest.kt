package studio.guitarlab.core.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioClockAnchorPolicyTest {
    @Test fun `stable observations recover stream origin`() {
        val rate = 48_000
        val origin = 10_000_000_000L
        val observations = listOf(
            AudioClockObservation(0, origin + 100_000),
            AudioClockObservation(480, origin + 10_000_000L - 100_000),
            AudioClockObservation(960, origin + 20_000_000L + 120_000),
            AudioClockObservation(1_440, origin + 30_000_000L - 80_000),
        )
        val anchor = assertNotNull(AudioClockAnchorPolicy.estimate(observations, rate))
        assertTrue(abs(anchor.streamOriginMonotonicNs - origin) <= 200_000L)
        assertEquals(4, anchor.observations)
    }

    @Test fun `single timestamp is never treated as stable`() {
        assertNull(AudioClockAnchorPolicy.estimate(listOf(AudioClockObservation(0, 1_000L)), 48_000))
    }

    @Test fun `repeated stale timestamp is not mistaken for independent evidence`() {
        val stale = AudioClockObservation(120L, 9_000_000_000L)
        assertNull(AudioClockAnchorPolicy.estimate(List(6) { stale }, 48_000))
    }

    @Test fun `stale startup samples are ignored until both frame and clock advance`() {
        val origin = 3_000_000_000L
        val observations = listOf(
            AudioClockObservation(0L, origin),
            AudioClockObservation(0L, origin),
            AudioClockObservation(0L, origin + 1_000_000L),
            AudioClockObservation(480L, origin + 10_000_000L),
            AudioClockObservation(960L, origin + 20_000_000L),
        )
        val anchor = assertNotNull(AudioClockAnchorPolicy.estimate(observations, 48_000))
        assertEquals(3, anchor.observations)
        assertEquals(origin, anchor.streamOriginMonotonicNs)
    }

    @Test fun `backwards frame or monotonic clock invalidates anchor`() {
        val origin = 4_000_000_000L
        assertNull(
            AudioClockAnchorPolicy.estimate(
                listOf(
                    AudioClockObservation(0L, origin),
                    AudioClockObservation(480L, origin + 10_000_000L),
                    AudioClockObservation(240L, origin + 20_000_000L),
                ),
                48_000,
            )
        )
        assertNull(
            AudioClockAnchorPolicy.estimate(
                listOf(
                    AudioClockObservation(0L, origin),
                    AudioClockObservation(480L, origin + 10_000_000L),
                    AudioClockObservation(960L, origin + 9_000_000L),
                ),
                48_000,
            )
        )
    }

    @Test fun `wildly inconsistent timestamp origins fail closed`() {
        val observations = listOf(
            AudioClockObservation(0, 1_000_000_000L),
            AudioClockObservation(480, 1_100_000_000L),
            AudioClockObservation(960, 1_020_000_000L),
        )
        assertNull(AudioClockAnchorPolicy.estimate(observations, 48_000, maxJitterNs = 2_000_000L))
    }

    @Test fun `anchor conversion supports 44 1 48 88 2 and 96 kHz`() {
        val origin = 6_000_000_000L
        val cases = listOf(
            44_100 to 441L,
            48_000 to 480L,
            88_200 to 882L,
            96_000 to 960L,
        )
        cases.forEach { (rate, framesIn10Ms) ->
            val anchor = assertNotNull(
                AudioClockAnchorPolicy.estimate(
                    listOf(
                        AudioClockObservation(0L, origin),
                        AudioClockObservation(framesIn10Ms, origin + 10_000_000L),
                        AudioClockObservation(framesIn10Ms * 2L, origin + 20_000_000L),
                    ),
                    rate,
                )
            )
            assertEquals(origin, anchor.streamOriginMonotonicNs)
        }
    }
}
