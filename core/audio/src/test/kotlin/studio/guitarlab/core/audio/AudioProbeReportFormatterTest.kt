package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertTrue

class AudioProbeReportFormatterTest {
    @Test
    fun reportContainsSelectedRouteAndOutcome() {
        val device = AudioDeviceDescriptor(
            key = "7",
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
            inputConfig = AudioStreamConfig("7", "7", 44_100, 1, PcmEncoding.PCM_16, 256, false, "UNPROCESSED"),
            message = "ok"
        )

        val report = AudioProbeReportFormatter.format(listOf(device), "7", "7", result)
        assertTrue(report.contains("USB Interface [7]"))
        assertTrue(report.contains("outcome=PASS"))
        assertTrue(report.contains("routed=7"))
    }
}
