package studio.guitarlab.core.audio

import kotlin.math.roundToInt

/** User-copyable diagnostics. Default output intentionally excludes transient/raw device keys. */
object AudioProbeReportFormatter {
    fun format(
        devices: List<AudioDeviceDescriptor>,
        selectedInputKey: String?,
        selectedOutputKey: String?,
        result: AudioProbeResult?
    ): String = buildString {
        appendLine("GuitarLab Studio — Audio Diagnostics")
        appendLine("Selected input: ${deviceLabel(devices, selectedInputKey)}")
        appendLine("Selected output: ${deviceLabel(devices, selectedOutputKey)}")
        appendLine()
        appendLine("Devices:")
        devices.forEach { device ->
            val directions = buildList {
                if (device.supportsInput) add("IN")
                if (device.supportsOutput) add("OUT")
            }.joinToString("+")
            appendLine("- ${device.name} ${device.transport} $directions")
            appendLine("  type=${device.typeLabel}")
            appendLine("  rates=${device.sampleRatesHz.ifEmpty { listOf(-1) }.joinToString { if (it < 0) "system" else it.toString() }}")
            appendLine("  channels=${device.channelCounts.ifEmpty { listOf(-1) }.joinToString { if (it < 0) "system" else it.toString() }}")
            appendLine("  pcm=${if (device.encodings.isEmpty()) "system" else device.encodings.joinToString()}")
        }
        result?.let { probe ->
            appendLine()
            appendLine("Last probe:")
            appendLine("- operation=${probe.operation}")
            appendLine("- outcome=${when { probe.stopped -> "STOPPED"; probe.success -> "PASS"; else -> "FAIL" }}")
            appendLine("- elapsedMs=${probe.elapsedMs}")
            appendLine("- message=${probe.message}")
            probe.inputConfig?.let { appendLine("- input=${config(it, devices)}") }
            probe.outputConfig?.let { appendLine("- output=${config(it, devices)}") }
            probe.inputStats?.let {
                appendLine("- capturedFrames=${it.frames} peakPct=${(it.peak * 100).roundToInt()} rmsPct=${(it.rms * 100).roundToInt()}")
            }
            appendLine("- outputFrames=${probe.outputFrames}")
            appendLine("- outputUnderruns=${probe.outputUnderruns}")
            probe.warnings.forEach { appendLine("- warning=$it") }
        }
    }.trimEnd()

    private fun deviceLabel(devices: List<AudioDeviceDescriptor>, key: String?): String =
        key?.let { selected -> devices.firstOrNull { it.key == selected }?.name }
            ?: "Auto / none"

    private fun config(config: AudioStreamConfig, devices: List<AudioDeviceDescriptor>): String =
        "requested=${deviceLabel(devices, config.deviceKey)} routed=${deviceLabel(devices, config.routedDeviceKey)} " +
            "${config.sampleRateHz}Hz ${config.channelCount}ch ${config.encoding} buffer=${config.bufferFrames}f " +
            "lowLatency=${config.requestedLowLatency}" +
            (config.inputSourceLabel?.let { " source=$it" } ?: "")
}
