package studio.guitarlab.core.audio

enum class MonitoringMode {
    OFF,
    AUTO,
    ON,
}

/** Conservative software-monitoring policy. AUTO prioritizes avoiding feedback/double monitoring. */
object SoftwareMonitoringPolicy {
    fun shouldMonitor(
        mode: MonitoringMode,
        inputTransport: AudioTransport?,
        outputTransport: AudioTransport?,
    ): Boolean = when (mode) {
        MonitoringMode.OFF -> false
        MonitoringMode.ON -> true
        MonitoringMode.AUTO -> {
            // USB guitar interfaces commonly provide direct monitoring; Bluetooth adds
            // unusable latency for live guitar; built-in speakers risk acoustic feedback.
            inputTransport != AudioTransport.USB && outputTransport == AudioTransport.WIRED
        }
    }
}
