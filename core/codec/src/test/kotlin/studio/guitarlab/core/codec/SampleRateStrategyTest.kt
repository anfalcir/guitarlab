package studio.guitarlab.core.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleRateStrategyTest {
    @Test
    fun equalRatesStayBitClockAlignedWithoutSrc() {
        val plan = SampleRateStrategy.plan(48_000, 48_000)
        assertEquals(SampleRateAction.PASSTHROUGH, plan.action)
        assertEquals(1.0, plan.ratio)
    }

    @Test
    fun differingRatesRequireExplicitSrc() {
        val plan = SampleRateStrategy.plan(44_100, 48_000)
        assertEquals(SampleRateAction.RESAMPLE, plan.action)
        assertTrue(plan.ratio > 1.08 && plan.ratio < 1.09)
    }

    @Test
    fun supportMatrixAdvertisesNothingBeforeAndroidGate() {
        assertTrue(CodecSupportMatrix.advertisedImportFormats.isEmpty())
        assertEquals(
            CodecSupportState.IMPLEMENTED_PENDING_ANDROID_GATE,
            CodecSupportMatrix.import.first { it.format == AudioFileFormat.WAV }.state,
        )
    }
}
