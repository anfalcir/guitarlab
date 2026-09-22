package studio.guitarlab.app.ui

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import studio.guitarlab.core.project.ExternalControlInputEvent
import studio.guitarlab.core.project.MidiControlStreamParser

data class ExternalMidiDeviceStatus(val stableDescriptor: String, val label: String)

/** Android MIDI adapter only. It never reads or changes audio routing. */
class AndroidExternalMidiRuntime(
    context: Context,
    private val onEvent: (ExternalControlInputEvent, String) -> Unit,
    private val onDevicesChanged: (List<ExternalMidiDeviceStatus>) -> Unit,
) : AutoCloseable {
    private val manager = context.applicationContext.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val handler = Handler(Looper.getMainLooper())
    private val opened = ConcurrentHashMap<Int, OpenedMidiDevice>()
    private val openingIds = ConcurrentHashMap.newKeySet<Int>()
    @Volatile private var started = false

    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) { open(device); publishDevices() }
        override fun onDeviceRemoved(device: MidiDeviceInfo) { closeDevice(device.id); publishDevices() }
        override fun onDeviceStatusChanged(status: android.media.midi.MidiDeviceStatus) { publishDevices() }
    }

    @Synchronized fun start() {
        if (started) return
        started = true
        val midi = manager ?: run { onDevicesChanged(emptyList()); return }
        midi.registerDeviceCallback(callback, handler)
        midi.devices.forEach(::open)
        publishDevices()
    }

    @Synchronized fun stop() {
        if (!started) return
        started = false
        manager?.unregisterDeviceCallback(callback)
        opened.keys.toList().forEach(::closeDevice)
        openingIds.clear()
        onDevicesChanged(emptyList())
    }

    override fun close() = stop()

    private fun open(info: MidiDeviceInfo) {
        if (!started || manager == null || info.ports.none { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }) return
        if (opened.containsKey(info.id) || !openingIds.add(info.id)) return
        manager.openDevice(info, { device ->
            openingIds.remove(info.id)
            if (!started || device == null) {
                runCatching { device?.close() }
                publishDevices()
                return@openDevice
            }
            val ports = mutableListOf<MidiOutputPort>()
            info.ports.filter { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }.forEach { portInfo ->
                val port = device.openOutputPort(portInfo.portNumber) ?: return@forEach
                val descriptor = stableDescriptor(info, portInfo.name.orEmpty(), portInfo.portNumber)
                val label = displayLabel(info, portInfo.name.orEmpty())
                val parser = MidiControlStreamParser(descriptor)
                port.connect(object : MidiReceiver() {
                    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                        parser.feed(msg, offset, count).forEach { event -> onEvent(event, label) }
                    }
                })
                ports += port
            }
            if (ports.isEmpty()) {
                runCatching { device.close() }
            } else {
                opened[info.id] = OpenedMidiDevice(device, ports)
            }
            publishDevices()
        }, handler)
    }

    private fun closeDevice(id: Int) {
        opened.remove(id)?.close()
        openingIds.remove(id)
    }

    private fun publishDevices() {
        val midi = manager ?: return onDevicesChanged(emptyList())
        val statuses = midi.devices.flatMap { info ->
            info.ports.filter { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }.map { port ->
                ExternalMidiDeviceStatus(stableDescriptor(info, port.name.orEmpty(), port.portNumber), displayLabel(info, port.name.orEmpty()))
            }
        }.distinctBy { it.stableDescriptor }.sortedBy { it.label.lowercase() }
        onDevicesChanged(statuses)
    }

    private fun stableDescriptor(info: MidiDeviceInfo, portName: String, portNumber: Int): String {
        val properties = info.properties
        val raw = listOf(
            properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER).orEmpty(),
            properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT).orEmpty(),
            properties.getString(MidiDeviceInfo.PROPERTY_NAME).orEmpty(),
            properties.getString(MidiDeviceInfo.PROPERTY_SERIAL_NUMBER).orEmpty(),
            properties.getString(MidiDeviceInfo.PROPERTY_VERSION).orEmpty(),
            info.type.toString(),
            portName,
            portNumber.toString(),
        ).joinToString("|").lowercase()
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        return "midi:$digest"
    }

    private fun displayLabel(info: MidiDeviceInfo, portName: String): String {
        val properties = info.properties
        val product = properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: "Controlador MIDI"
        return if (portName.isBlank() || portName.equals(product, true)) product else "$product • $portName"
    }

    private data class OpenedMidiDevice(val device: MidiDevice, val ports: List<MidiOutputPort>) {
        fun close() {
            ports.forEach { runCatching { it.close() } }
            runCatching { device.close() }
        }
    }
}
