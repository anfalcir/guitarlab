package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioProbeReportFormatterTest {
    @Test
    fun reportContainsSemanticRouteAndOutcomeWithoutTransientKeys() {
        val device = AudioDeviceDescriptor(
            key = "transient-device-7",
            name = "USB Interface",
            typeLabel = "USB audio device",
            transport = AudioTransport.USB,
            supportsInput = true,
            supportsOutput = true,
            sampleRatesHz = listOf(44_100),
            channelCounts = listOf(1, 2),
            encodings = listOf(PcmEncoding.PCM_16)
        )
        val result = AudioProbeResult(
            operation = AudioProbeOperation.RECORD,
            success = true,
            elapsedMs = 100,
            inputConfig = AudioStreamConfig("transient-device-7", "transient-device-7", 44_100, 1, PcmEncoding.PCM_16, 256, false, "UNPROCESSED"),
            message = "ok"
        )

        val report = AudioProbeReportFormatter.format(listOf(device), "transient-device-7", "transient-device-7", result)
        assertTrue(report.contains("USB Interface"))
        assertTrue(report.contains("outcome=PASS"))
        assertTrue(report.contains("routed=USB Interface"))
        assertFalse(report.contains("transient-device-7"))
    }
}
