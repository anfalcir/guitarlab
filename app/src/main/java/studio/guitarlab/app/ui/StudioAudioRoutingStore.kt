package studio.guitarlab.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import studio.guitarlab.core.audio.MonitoringMode

data class StudioRouteHealth(
    val selectedInputAvailable: Boolean,
    val selectedOutputAvailable: Boolean,
    val selectedCueOutputAvailable: Boolean,
    val cueOutputDistinctFromMain: Boolean,
    val effectiveInput: StudioAudioDeviceChoice?,
    val effectiveOutput: StudioAudioDeviceChoice?,
    val effectiveCueOutput: StudioAudioDeviceChoice?,
) {
    val usbDeviceDetected: Boolean get() = listOfNotNull(effectiveInput, effectiveOutput, effectiveCueOutput).any {
        it.transportFamily == StudioAudioRoutePolicy.USB_FAMILY
    }
}

class StudioAudioRoutingStore(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun inputChoices(): List<StudioAudioDeviceChoice> = StudioAudioRoutePolicy.canonicalizeInputs(rawInputChoices())

    fun outputChoices(): List<StudioAudioDeviceChoice> = StudioAudioRoutePolicy.canonicalizeOutputs(rawOutputChoices())

    fun selectedInputSignature(): String? {
        val stored = preferences.getString(KEY_INPUT_SIGNATURE, null) ?: return null
        val raw = rawInputChoices()
        val canonical = StudioAudioRoutePolicy.canonicalInputSignature(raw, stored)
        if (canonical != null) {
            if (canonical != stored) preferences.edit().putString(KEY_INPUT_SIGNATURE, canonical).apply()
            return canonical
        }
        // If the old selection still exists but is now intentionally hidden (for example
        // remote-submix), drop it instead of exposing or silently reusing a system-only route.
        if (raw.any { stored == it.signature || stored in it.legacySignatures }) {
            preferences.edit().remove(KEY_INPUT_SIGNATURE).apply()
            return null
        }
        return stored
    }

    fun selectedOutputSignature(): String? =
        selectedCanonicalOutputSignature(KEY_OUTPUT_SIGNATURE)

    /**
     * Secondary output is explicit-only. A null value means CUE is disabled, never "automatic".
     * MAIN and CUE may not resolve to the same canonical physical route.
     */
    fun selectedCueOutputSignature(): String? =
        selectedCanonicalOutputSignature(KEY_CUE_OUTPUT_SIGNATURE)

    private fun selectedCanonicalOutputSignature(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null
        val canonical = StudioAudioRoutePolicy.canonicalSignature(rawOutputChoices(), stored) ?: return stored
        if (canonical != stored) preferences.edit().putString(key, canonical).apply()
        return canonical
    }

    /** Sanitized semantic identity for diagnostics; excludes transient Android numeric ids and raw addresses. */
    fun selectedInputDiagnosticIdentity(): String? = selectedInputSignature()?.let { signature ->
        inputChoices().firstOrNull { it.signature == signature }?.let { "${it.transportFamily}:${it.label}" } ?: "entrada-selecionada-indisponível"
    }

    /** Sanitized semantic identity for diagnostics; excludes transient Android numeric ids and raw addresses. */
    fun selectedOutputDiagnosticIdentity(): String? = selectedOutputSignature()?.let { signature ->
        outputChoices().firstOrNull { it.signature == signature }?.let { "${it.transportFamily}:${it.label}" } ?: "saída-selecionada-indisponível"
    }

    /** Sanitized semantic identity of the explicit secondary/CUE route. */
    fun selectedCueOutputDiagnosticIdentity(): String? = selectedCueOutputSignature()?.let { signature ->
        outputChoices().firstOrNull { it.signature == signature }?.let { "${it.transportFamily}:${it.label}" } ?: "cue-selecionado-indisponível"
    }

    fun selectInput(signature: String?) {
        preferences.edit().putString(KEY_INPUT_SIGNATURE, signature).apply()
    }

    fun selectOutput(signature: String?) {
        preferences.edit().putString(KEY_OUTPUT_SIGNATURE, signature).apply()
    }

    fun selectCueOutput(signature: String?) {
        preferences.edit().putString(KEY_CUE_OUTPUT_SIGNATURE, signature).apply()
    }

    fun monitoringMode(): MonitoringMode {
        val stored = preferences.getString(KEY_MONITORING_MODE, MonitoringMode.AUTO.name)
        return runCatching { MonitoringMode.valueOf(stored ?: MonitoringMode.AUTO.name) }.getOrDefault(MonitoringMode.AUTO)
    }

    fun selectMonitoringMode(mode: MonitoringMode) {
        preferences.edit().putString(KEY_MONITORING_MODE, mode.name).apply()
    }

    fun resolveSelectedInputDeviceId(): Int? = resolveSelectedInputDevice()?.id
    fun resolveSelectedOutputDeviceId(): Int? = resolveSelectedOutputDevice()?.id
    fun resolveSelectedCueOutputDeviceId(): Int? = resolveSelectedCueOutputDevice()?.id

    fun resolveSelectedInputDevice(): AudioDeviceInfo? {
        val stored = selectedInputSignature() ?: return null
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()
        val raw = devices.map { toChoice(it, RouteDirection.INPUT) }
        val canonical = StudioAudioRoutePolicy.canonicalInputSignature(raw, stored) ?: return null
        val candidateIds = StudioAudioRoutePolicy.candidateInputIdsFor(raw, canonical).toSet()
        if (candidateIds.isEmpty()) return null
        val candidates = devices.filter { it.id in candidateIds }
        if (candidates.size == 1) return candidates.first()

        INPUT_ROUTE_CACHE[canonical]?.let { cachedId ->
            candidates.firstOrNull { it.id == cachedId }?.let { return it }
            INPUT_ROUTE_CACHE.remove(canonical, cachedId)
        }
        val choicesById = raw.associateBy { it.deviceId }
        val resolved = candidates.maxByOrNull { device ->
            choicesById[device.id]?.let(StudioAudioRoutePolicy::compatibilityScore) ?: 0
        }
        if (resolved != null) INPUT_ROUTE_CACHE[canonical] = resolved.id
        return resolved
    }

    /**
     * Resolves a logical user-facing output to an Android endpoint that is proven routable.
     * Duplicate USB endpoint groups are verified with an inaudible short AudioTrack and
     * AudioRouting.routedDevice; endpoint ordering is never used as a correctness signal.
     */
    fun resolveSelectedOutputDevice(): AudioDeviceInfo? {
        val selected = selectedOutputSignature() ?: return null
        return resolveOutputDevice(selected, OUTPUT_ROUTE_CACHE)
    }

    /**
     * CUE is fail-closed: it exists only when explicitly selected, physically resolvable and
     * canonically distinct from MAIN. There is deliberately no automatic CUE fallback.
     */
    fun resolveSelectedCueOutputDevice(): AudioDeviceInfo? {
        val selected = selectedCueOutputSignature() ?: return null
        if (selected == selectedOutputSignature()) return null
        return resolveOutputDevice(selected, CUE_OUTPUT_ROUTE_CACHE)
    }

    private fun resolveOutputDevice(
        selectedSignature: String,
        routeCache: ConcurrentHashMap<String, Int>,
    ): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        val raw = devices.map { toChoice(it, RouteDirection.OUTPUT) }
        val canonical = StudioAudioRoutePolicy.canonicalSignature(raw, selectedSignature) ?: return null
        val candidateIds = StudioAudioRoutePolicy.candidateIdsFor(raw, canonical).toSet()
        if (candidateIds.isEmpty()) return null
        val candidates = devices.filter { it.id in candidateIds }
        if (candidates.size == 1) return candidates.first()

        routeCache[canonical]?.let { cachedId ->
            candidates.firstOrNull { it.id == cachedId }?.let { return it }
            routeCache.remove(canonical, cachedId)
        }

        val choicesById = raw.associateBy { it.deviceId }
        val ordered = candidates.sortedByDescending { device ->
            choicesById[device.id]?.let(StudioAudioRoutePolicy::compatibilityScore) ?: 0
        }
        val resolved = probeRoutableOutput(ordered, candidateIds)
        if (resolved != null) routeCache[canonical] = resolved.id
        return resolved
    }

    fun isSelectedInputUnavailable(): Boolean =
        !selectedInputSignature().isNullOrBlank() && resolveSelectedInputDevice() == null

    fun isSelectedOutputUnavailable(): Boolean {
        val selected = selectedOutputSignature() ?: return false
        return outputChoices().none { it.signature == selected }
    }

    fun isSelectedCueOutputUnavailable(): Boolean {
        val selected = selectedCueOutputSignature() ?: return false
        if (selected == selectedOutputSignature()) return true
        return outputChoices().none { it.signature == selected }
    }

    fun routeHealth(): StudioRouteHealth {
        val selectedInput = selectedInputSignature()
        val input = selectedInput?.let { signature -> inputChoices().firstOrNull { it.signature == signature } }
        val selectedOutput = selectedOutputSignature()
        val output = selectedOutput?.let { signature -> outputChoices().firstOrNull { it.signature == signature } }
        val selectedCueOutput = selectedCueOutputSignature()
        val cueDistinct = selectedCueOutput == null || selectedCueOutput != selectedOutput
        val cueOutput = selectedCueOutput
            ?.takeIf { cueDistinct }
            ?.let { signature -> outputChoices().firstOrNull { it.signature == signature } }
        return StudioRouteHealth(
            selectedInputAvailable = selectedInput.isNullOrBlank() || input != null,
            selectedOutputAvailable = selectedOutput.isNullOrBlank() || output != null,
            selectedCueOutputAvailable = selectedCueOutput.isNullOrBlank() || (cueOutput != null && cueDistinct),
            cueOutputDistinctFromMain = cueDistinct,
            effectiveInput = input,
            effectiveOutput = output,
            effectiveCueOutput = cueOutput,
        )
    }

    private fun rawInputChoices(): List<StudioAudioDeviceChoice> =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).map { toChoice(it, RouteDirection.INPUT) }

    private fun rawOutputChoices(): List<StudioAudioDeviceChoice> =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { toChoice(it, RouteDirection.OUTPUT) }

    private fun probeRoutableOutput(candidates: List<AudioDeviceInfo>, candidateIds: Set<Int>): AudioDeviceInfo? {
        candidates.forEach { requested ->
            probeCandidate(requested, candidateIds)?.let { return it }
        }
        return null
    }

    private fun probeCandidate(requested: AudioDeviceInfo, candidateIds: Set<Int>): AudioDeviceInfo? {
        // Do not reject mono-looking logical endpoints up front. Android may expose several
        // logical speaker endpoints for one physical output and still route a stereo AudioTrack
        // through another endpoint in the same canonical group. routedDevice is authoritative.
        val rates = buildList {
            if (requested.sampleRates.isEmpty() || 48_000 in requested.sampleRates) add(48_000)
            if (requested.sampleRates.isEmpty() || 44_100 in requested.sampleRates) add(44_100)
            requested.sampleRates.firstOrNull { it > 0 }?.let(::add)
        }.distinct().ifEmpty { listOf(48_000) }
        val channelCount = 2

        for (sampleRate in rates) {
            val mask = AudioFormat.CHANNEL_OUT_STEREO
            val minBytes = AudioTrack.getMinBufferSize(sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT)
            if (minBytes <= 0) continue
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(mask)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(max(minBytes, PROBE_FRAMES * channelCount * 2 * 2))
                    .build()
            }.getOrNull() ?: continue
            try {
                if (track.state != AudioTrack.STATE_INITIALIZED) continue
                track.setVolume(0f)
                if (!track.setPreferredDevice(requested)) continue
                track.play()
                val silence = ShortArray(PROBE_FRAMES * channelCount)
                if (track.write(silence, 0, silence.size, AudioTrack.WRITE_BLOCKING) <= 0) continue
                val deadline = System.nanoTime() + PROBE_TIMEOUT_NS
                while (System.nanoTime() < deadline) {
                    val routed = track.routedDevice
                    if (routed != null && routed.id in candidateIds) {
                        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == routed.id }
                    }
                    try {
                        Thread.sleep(PROBE_POLL_MS)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return null
                    }
                }
            } catch (_: RuntimeException) {
                // Try the next candidate/configuration. Explicit output selection must fail closed.
            } finally {
                runCatching { track.pause() }
                runCatching { track.flush() }
                runCatching { track.release() }
            }
        }
        return null
    }

    private fun toChoice(device: AudioDeviceInfo, direction: RouteDirection): StudioAudioDeviceChoice {
        val product = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Dispositivo de áudio"
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address.orEmpty() else ""
        val family = when (direction) {
            RouteDirection.INPUT -> inputTransportFamily(device.type, product)
            RouteDirection.OUTPUT -> outputTransportFamily(device.type, product)
        }
        val label = friendlyDeviceLabel(direction, device.type, family, product)
        val legacySignature = buildString {
            append(device.type)
            append('|')
            append(product)
            append('|')
            append(address)
        }
        return StudioAudioDeviceChoice(
            signature = legacySignature,
            label = label,
            deviceId = device.id,
            type = device.type,
            channelCounts = device.channelCounts.toList().filter { it > 0 }.distinct().sorted(),
            sampleRates = device.sampleRates.toList().filter { it > 0 }.distinct().sorted(),
            productName = product,
            address = address,
            transportFamily = family,
            legacySignatures = listOf(legacySignature),
        )
    }

    private fun inputTransportFamily(type: Int, product: String): String = when {
        type in USB_DEVICE_TYPES -> StudioAudioRoutePolicy.USB_FAMILY
        type == AudioDeviceInfo.TYPE_BUILTIN_MIC -> StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY
        type == AudioDeviceInfo.TYPE_REMOTE_SUBMIX || type == AudioDeviceInfo.TYPE_TELEPHONY -> StudioAudioRoutePolicy.HIDDEN_SYSTEM_FAMILY
        type == AudioDeviceInfo.TYPE_BUS && isThisAndroidDevice(product) -> StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY
        else -> "type:$type"
    }

    private fun outputTransportFamily(type: Int, product: String): String = when {
        type in USB_DEVICE_TYPES -> StudioAudioRoutePolicy.USB_FAMILY
        type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY
        type == AudioDeviceInfo.TYPE_REMOTE_SUBMIX || type == AudioDeviceInfo.TYPE_TELEPHONY -> StudioAudioRoutePolicy.HIDDEN_SYSTEM_FAMILY
        type == AudioDeviceInfo.TYPE_BUS && isThisAndroidDevice(product) -> StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY
        else -> "type:$type"
    }

    private fun isThisAndroidDevice(product: String): Boolean {
        val normalized = product.trim().lowercase()
        if (normalized.isBlank()) return false
        return listOf(Build.MODEL, Build.DEVICE, Build.PRODUCT)
            .map { it.orEmpty().trim().lowercase() }
            .filter { it.isNotBlank() }
            .any { it == normalized }
    }

    private fun friendlyDeviceLabel(direction: RouteDirection, type: Int, family: String, product: String): String = when (family) {
        StudioAudioRoutePolicy.BUILTIN_MIC_FAMILY -> "Microfone do tablet"
        StudioAudioRoutePolicy.BUILTIN_SPEAKER_FAMILY -> "Alto-falante do tablet"
        StudioAudioRoutePolicy.USB_FAMILY -> product.ifBlank { "Interface USB" }
        StudioAudioRoutePolicy.HIDDEN_SYSTEM_FAMILY -> "Rota interna do sistema"
        else -> when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Auricular do tablet"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> if (direction == RouteDirection.INPUT) "Microfone do headset" else "Fone com fio"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth · $product"
            AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI · $product"
            else -> product
        }
    }

    private enum class RouteDirection { INPUT, OUTPUT }

    private val USB_DEVICE_TYPES = setOf(
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_USB_HEADSET,
    )

    private companion object {
        const val PREFS_NAME = "studio_audio_routing"
        const val KEY_INPUT_SIGNATURE = "input_signature"
        const val KEY_OUTPUT_SIGNATURE = "output_signature"
        const val KEY_CUE_OUTPUT_SIGNATURE = "cue_output_signature"
        const val KEY_MONITORING_MODE = "monitoring_mode"
        const val PROBE_FRAMES = 256
        const val PROBE_POLL_MS = 8L
        const val PROBE_TIMEOUT_NS = 160_000_000L
        val INPUT_ROUTE_CACHE = ConcurrentHashMap<String, Int>()
        val OUTPUT_ROUTE_CACHE = ConcurrentHashMap<String, Int>()
        val CUE_OUTPUT_ROUTE_CACHE = ConcurrentHashMap<String, Int>()
    }
}
