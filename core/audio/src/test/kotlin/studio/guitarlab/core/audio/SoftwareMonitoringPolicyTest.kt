package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoftwareMonitoringPolicyTest {
    @Test fun offNeverMonitors() {
        assertFalse(SoftwareMonitoringPolicy.shouldMonitor(MonitoringMode.OFF, AudioTransport.BUILT_IN, AudioTransport.WIRED))
    }

    @Test fun onAlwaysRequestsSoftwareMonitoring() {
        assertTrue(SoftwareMonitoringPolicy.shouldMonitor(MonitoringMode.ON, AudioTransport.USB, AudioTransport.USB))
    }

    @Test fun autoAvoidsUsbDoubleMonitoringAndBluetoothLatency() {
        assertFalse(SoftwareMonitoringPolicy.shouldMonitor(MonitoringMode.AUTO, AudioTransport.USB, AudioTransport.WIRED))
        assertFalse(SoftwareMonitoringPolicy.shouldMonitor(MonitoringMode.AUTO, AudioTransport.BUILT_IN, AudioTransport.BLUETOOTH))
    }

    @Test fun autoAllowsBuiltInInputToWiredHeadphones() {
        assertTrue(SoftwareMonitoringPolicy.shouldMonitor(MonitoringMode.AUTO, AudioTransport.BUILT_IN, AudioTransport.WIRED))
    }
}
