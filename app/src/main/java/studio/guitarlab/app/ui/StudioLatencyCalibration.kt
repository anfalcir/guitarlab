package studio.guitarlab.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import studio.guitarlab.core.audio.AudioClockAnchorPolicy
import studio.guitarlab.core.audio.AudioClockObservation
import studio.guitarlab.core.audio.LatencyCalibration
import studio.guitarlab.core.audio.LatencyCalibrationScopeKeyPolicy
import studio.guitarlab.core.audio.LatencyFineAdjustmentPolicy
import studio.guitarlab.core.audio.LatencyCalibrationStimulusPolicy
import studio.guitarlab.core.audio.LatencyCalibrationPolicy

class StudioLatencyCalibrationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("studio-latency-calibration", Context.MODE_PRIVATE)

    fun save(inputSignature: String?, outputSignature: String?, calibration: LatencyCalibration) {
        val key = key(inputSignature, outputSignature, calibration.sampleRateHz) ?: return
        prefs.edit()
            .putLong("$key.frames", calibration.latencyFrames)
            .putLong("$key.jitter", calibration.jitterFrames)
            .putString("$key.drift", calibration.driftPpm.toString())
            .putFloat("$key.confidence", calibration.confidence)
            .putInt("$key.attempts", calibration.attempts)
            .putBoolean("$key.accepted", calibration.accepted)
            .putLong("$key.measured", calibration.measuredAtEpochMs)
            .apply()
    }

    fun find(inputSignature: String?, outputSignature: String?, sampleRateHz: Int): LatencyCalibration? {
        val currentKey = key(inputSignature, outputSignature, sampleRateHz) ?: return null
        // H23b deliberately does not auto-read the legacy 32-bit hashed key. The legacy key cannot
        // prove the originating route tuple, so accepting it would violate the exact-route lookup
        // contract in the (rare but real) event of a hash collision. Recalibration is safer.
        return readCalibration(currentKey, sampleRateHz)
    }

    fun fineAdjustmentFrames(inputSignature: String?, outputSignature: String?, sampleRateHz: Int): Long {
        val currentKey = key(inputSignature, outputSignature, sampleRateHz) ?: return 0L
        return LatencyFineAdjustmentPolicy.clampFrames(prefs.getLong("$currentKey.fine", 0L), sampleRateHz)
    }

    fun saveFineAdjustmentFrames(inputSignature: String?, outputSignature: String?, sampleRateHz: Int, frames: Long) {
        val key = key(inputSignature, outputSignature, sampleRateHz) ?: return
        prefs.edit()
            .putLong("$key.fine", LatencyFineAdjustmentPolicy.clampFrames(frames, sampleRateHz))
            .apply()
    }

    fun clear(inputSignature: String?, outputSignature: String?, sampleRateHz: Int) {
        val currentKey = key(inputSignature, outputSignature, sampleRateHz) ?: return
        prefs.edit().also { editor ->
            prefs.all.keys.filter { it.startsWith(currentKey) }.forEach(editor::remove)
        }.apply()
    }

    private fun readCalibration(key: String, sampleRateHz: Int): LatencyCalibration? {
        if (!prefs.contains("$key.frames")) return null
        return LatencyCalibration(
            latencyFrames = prefs.getLong("$key.frames", 0L),
            jitterFrames = prefs.getLong("$key.jitter", 0L),
            driftPpm = prefs.getString("$key.drift", "0")?.toDoubleOrNull() ?: 0.0,
            confidence = prefs.getFloat("$key.confidence", 0f),
            sampleRateHz = sampleRateHz,
            attempts = prefs.getInt("$key.attempts", 0),
            accepted = prefs.getBoolean("$key.accepted", false),
            measuredAtEpochMs = prefs.getLong("$key.measured", 0L),
        )
    }

    private fun key(input: String?, output: String?, rate: Int): String? =
        LatencyCalibrationScopeKeyPolicy.storageKey(input, output, rate)

}

data class DigitalTimingVerification(
    val sampleRateHz: Int,
    val sessionDeltaFrames: Long,
    val inputJitterNs: Long,
    val outputJitterNs: Long,
    val inputObservations: Int,
    val outputObservations: Int,
)

class AndroidLatencyCalibrationEngine(private val context: Context) {
    suspend fun calibrate(
        sampleRateHz: Int,
        inputDevice: AudioDeviceInfo?,
        outputDevice: AudioDeviceInfo?,
        attempts: Int = 5,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): LatencyCalibration = withContext(Dispatchers.IO) {
        requireRecordPermission()
        require(sampleRateHz in 8_000..192_000)
        require(attempts in 3..9)
        val input = requireNotNull(inputDevice) { "A entrada selecionada não está mais disponível." }
        val output = requireNotNull(outputDevice) { "A saída selecionada não está mais disponível." }
        val measurements = mutableListOf<Long>()
        val confidences = mutableListOf<Float>()
        val startedMs = System.currentTimeMillis()
        var gainIndex = 0
        repeat(attempts) { index ->
            onProgress(index + 1, attempts)
            var pass: CalibrationPass
            while (true) {
                pass = measurePass(sampleRateHz, input, output, LatencyCalibrationStimulusPolicy.adaptiveGains[gainIndex])
                if (pass.confidence >= LatencyCalibrationStimulusPolicy.CORRELATION_THRESHOLD) break
                if (gainIndex >= LatencyCalibrationStimulusPolicy.adaptiveGains.lastIndex) {
                    error("Sinal de loopback não detectado nem no nível seguro máximo. Verifique o retorno físico/digital entre saída e entrada.")
                }
                gainIndex += 1
            }
            measurements += pass.latencyFrames
            confidences += pass.confidence
            // Keep later passes quiet when possible, but raise one step after a marginal match.
            if (pass.confidence < .55f && gainIndex < LatencyCalibrationStimulusPolicy.adaptiveGains.lastIndex) gainIndex += 1
            delay(80L)
        }
        LatencyCalibrationPolicy.evaluate(
            measurementsFrames = measurements,
            confidences = confidences,
            sampleRateHz = sampleRateHz,
            elapsedMs = (System.currentTimeMillis() - startedMs).coerceAtLeast(1L),
            measuredAtEpochMs = System.currentTimeMillis(),
        )
    }

    /**
     * Silent digital verification. It validates the selected effective routes and stable Android
     * audio clocks using PCM zero only. It intentionally does NOT produce or persist a physical
     * round-trip latency value because silence cannot measure the analog/DSP path.
     */
    suspend fun verifyDigitalTiming(
        sampleRateHz: Int,
        inputDevice: AudioDeviceInfo?,
        outputDevice: AudioDeviceInfo?,
    ): DigitalTimingVerification = withContext(Dispatchers.IO) {
        requireRecordPermission()
        require(sampleRateHz in 8_000..192_000)
        val input = requireNotNull(inputDevice) { "A entrada selecionada não está mais disponível." }
        val output = requireNotNull(outputDevice) { "A saída selecionada não está mais disponível." }
        val frames = (sampleRateHz / 2).coerceAtLeast(2_048)
        val recorder = buildRecorder(sampleRateHz, frames)
        val track = buildTrack(sampleRateHz, frames)
        var captureSession: CaptureSession? = null
        try {
            require(recorder.setPreferredDevice(input)) { "A entrada selecionada recusou a verificação digital." }
            require(track.setPreferredDevice(output)) { "A saída selecionada recusou a verificação digital." }
            val captured = FloatArray(frames)
            recorder.startRecording()
            val capture = startCaptureThread(recorder, captured)
            captureSession = capture
            writeAll(track, FloatArray(frames * 2))
            track.play()
            awaitExactRoute("entrada", input) { recorder.routedDevice }
            awaitExactRoute("saída", output) { track.routedDevice }
            val inputAnchor = stableInputAnchor(recorder, sampleRateHz)
                ?: error("A entrada não forneceu timestamps digitais estáveis.")
            val outputAnchor = stableOutputAnchor(track, sampleRateHz)
                ?: error("A saída não forneceu timestamps digitais estáveis.")
            capture.thread.join(DIGITAL_VERIFY_TIMEOUT_MS)
            require(!capture.thread.isAlive) { "A verificação digital não concluiu a captura no tempo esperado." }
            require(capture.framesRead[0] >= sampleRateHz / 10) { "A entrada não forneceu áudio suficiente para validar o clock digital." }
            val deltaNs = inputAnchor.streamOriginMonotonicNs - outputAnchor.streamOriginMonotonicNs
            val deltaFrames = (deltaNs.toDouble() * sampleRateHz.toDouble() / 1_000_000_000.0).roundToLong()
            DigitalTimingVerification(
                sampleRateHz = sampleRateHz,
                sessionDeltaFrames = deltaFrames,
                inputJitterNs = inputAnchor.jitterNs,
                outputJitterNs = outputAnchor.jitterNs,
                inputObservations = inputAnchor.observations,
                outputObservations = outputAnchor.observations,
            )
        } finally {
            release(recorder, track, captureSession?.thread)
        }
    }

    private data class CalibrationPass(val latencyFrames: Long, val confidence: Float)

    private fun measurePass(
        sampleRateHz: Int,
        inputDevice: AudioDeviceInfo,
        outputDevice: AudioDeviceInfo,
        gain: Float,
    ): CalibrationPass {
        requireRecordPermission()
        val template = LatencyCalibrationStimulusPolicy.generate(sampleRateHz, gain)
        val routeWarmupFrames = sampleRateHz * 3 / 10
        val preRollFrames = sampleRateHz / 20
        val tailFrames = sampleRateHz / 3
        val captureFrames = sampleRateHz
        val outputFrames = routeWarmupFrames + preRollFrames + template.size + tailFrames
        val recorder = buildRecorder(sampleRateHz, captureFrames)
        val track = buildTrack(sampleRateHz, outputFrames)
        var captureSession: CaptureSession? = null
        try {
            require(recorder.setPreferredDevice(inputDevice)) { "A entrada selecionada recusou a calibração." }
            require(track.setPreferredDevice(outputDevice)) { "A saída selecionada recusou a calibração." }
            val captured = FloatArray(captureFrames)
            recorder.startRecording()
            val capture = startCaptureThread(recorder, captured)
            captureSession = capture

            // Route activation is performed with digital zero only. No calibration chirp is emitted
            // until Android confirms that both effective routes are exactly the selected endpoints.
            writeAll(track, FloatArray(routeWarmupFrames * 2))
            track.play()
            awaitExactRoute("entrada", inputDevice) { recorder.routedDevice }
            awaitExactRoute("saída", outputDevice) { track.routedDevice }
            val inputAnchor = stableInputAnchor(recorder, sampleRateHz)
            val outputAnchor = stableOutputAnchor(track, sampleRateHz)
            require(inputAnchor != null && outputAnchor != null) { "O Android não forneceu timestamps de áudio estáveis para esta rota." }

            val signalMono = FloatArray(preRollFrames + template.size + tailFrames)
            template.copyInto(signalMono, destinationOffset = preRollFrames)
            val signalStereo = FloatArray(signalMono.size * 2)
            signalMono.forEachIndexed { i, sample ->
                signalStereo[i * 2] = sample
                signalStereo[i * 2 + 1] = sample
            }
            writeAll(track, signalStereo)
            capture.thread.join(CAPTURE_TIMEOUT_MS)
            require(!capture.thread.isAlive) { "A captura da calibração não terminou no tempo esperado." }
            require(capture.framesRead[0] > template.size) { "A entrada não forneceu áudio suficiente para analisar o loopback." }

            val match = correlate(captured, capture.framesRead[0], template)
            val inputFrame = match.first.toLong()
            val outputFrame = (routeWarmupFrames + preRollFrames).toLong()
            val inputNs = inputAnchor.streamOriginMonotonicNs + (inputFrame * 1_000_000_000.0 / sampleRateHz).roundToLong()
            val outputNs = outputAnchor.streamOriginMonotonicNs + (outputFrame * 1_000_000_000.0 / sampleRateHz).roundToLong()
            val latencyFrames = (((inputNs - outputNs).coerceAtLeast(0L)) * sampleRateHz / 1_000_000_000.0).roundToLong()
            require(latencyFrames in 0..sampleRateHz.toLong()) { "A medição de latência ficou fora da faixa confiável." }
            return CalibrationPass(latencyFrames, match.second)
        } finally {
            release(recorder, track, captureSession?.thread)
        }
    }

    private fun buildRecorder(sampleRateHz: Int, captureFrames: Int): AudioRecord {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Permissão de microfone necessária para analisar a rota de áudio.")
        }
        val recorderBufferBytes = max(
            AudioRecord.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT),
            captureFrames * Float.SIZE_BYTES,
        )
        require(recorderBufferBytes > 0) { "A entrada não suporta operação em $sampleRateHz Hz." }
        val recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(recorderBufferBytes)
            .build()
        require(recorder.state == AudioRecord.STATE_INITIALIZED) { "Não foi possível inicializar a entrada de áudio." }
        return recorder
    }

    private fun buildTrack(sampleRateHz: Int, outputFrames: Int): AudioTrack {
        val outMin = AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        require(outMin > 0) { "A saída não suporta operação em $sampleRateHz Hz." }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(outMin, outputFrames * 2 * Float.SIZE_BYTES))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        require(track.state == AudioTrack.STATE_INITIALIZED) { "Não foi possível inicializar a saída de áudio." }
        return track
    }

    private data class CaptureSession(val thread: Thread, val framesRead: IntArray)

    private fun startCaptureThread(recorder: AudioRecord, target: FloatArray): CaptureSession {
        val readFrames = IntArray(1)
        val thread = Thread {
            var read = 0
            while (read < target.size) {
                val amount = recorder.read(target, read, target.size - read, AudioRecord.READ_BLOCKING)
                if (amount <= 0) break
                read += amount
            }
            readFrames[0] = read
        }
        thread.start()
        return CaptureSession(thread, readFrames)
    }

    private fun writeAll(track: AudioTrack, samples: FloatArray) {
        var offset = 0
        while (offset < samples.size) {
            val written = track.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
            require(written > 0) { "A saída de áudio interrompeu a escrita durante a operação." }
            offset += written
        }
    }

    private fun awaitExactRoute(label: String, expected: AudioDeviceInfo, current: () -> AudioDeviceInfo?) {
        repeat(ROUTE_CONFIRM_POLLS) { poll ->
            val routed = current()
            if (routed != null && samePhysicalRoute(routed, expected)) return
            if (poll < ROUTE_CONFIRM_POLLS - 1) Thread.sleep(ROUTE_CONFIRM_POLL_MS)
        }
        error("A $label efetiva não corresponde à rota selecionada. A operação foi interrompida em silêncio, antes de qualquer sinal de calibração.")
    }

    private fun samePhysicalRoute(actual: AudioDeviceInfo, expected: AudioDeviceInfo): Boolean =
        actual.id == expected.id // Exact identity is safe here: both objects come from the same live enumeration.

    private fun stableInputAnchor(recorder: AudioRecord, sampleRateHz: Int) = AudioClockAnchorPolicy.estimate(
        observations = buildList {
            repeat(6) { index ->
                val timestamp = AudioTimestamp()
                if (recorder.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS) {
                    add(AudioClockObservation(timestamp.framePosition, timestamp.nanoTime))
                }
                if (index < 5) Thread.sleep(3L)
            }
        },
        sampleRateHz = sampleRateHz,
    )

    private fun stableOutputAnchor(track: AudioTrack, sampleRateHz: Int) = AudioClockAnchorPolicy.estimate(
        observations = buildList {
            repeat(6) { index ->
                val timestamp = AudioTimestamp()
                if (track.getTimestamp(timestamp)) add(AudioClockObservation(timestamp.framePosition, timestamp.nanoTime))
                if (index < 5) Thread.sleep(3L)
            }
        },
        sampleRateHz = sampleRateHz,
    )

    private fun correlate(captured: FloatArray, capturedSize: Int, template: FloatArray): Pair<Int, Float> {
        if (capturedSize <= template.size) return 0 to 0f
        var templateEnergy = 0.0
        template.indices.step(2).forEach { i -> templateEnergy += template[i] * template[i] }
        var bestIndex = 0
        var bestScore = 0.0
        val step = 2
        var offset = 0
        while (offset + template.size <= capturedSize) {
            var dot = 0.0
            var energy = 0.0
            var i = 0
            while (i < template.size) {
                val sample = captured[offset + i]
                dot += sample * template[i]
                energy += sample * sample
                i += step
            }
            val score = if (energy > 1e-9 && templateEnergy > 1e-9) abs(dot) / sqrt(energy * templateEnergy) else 0.0
            if (score > bestScore) {
                bestScore = score
                bestIndex = offset
            }
            offset += step
        }
        return bestIndex to bestScore.coerceIn(0.0, 1.0).toFloat()
    }

    private fun requireRecordPermission() {
        require(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Permissão de microfone necessária para analisar a rota de áudio."
        }
    }

    private fun release(recorder: AudioRecord, track: AudioTrack, captureThread: Thread? = null) {
        runCatching { recorder.stop() }
        runCatching { track.pause() }
        runCatching { captureThread?.join(250L) }
        runCatching { track.flush() }
        recorder.release()
        track.release()
    }

    companion object {
        private const val ROUTE_CONFIRM_POLLS = 40
        private const val ROUTE_CONFIRM_POLL_MS = 5L
        private const val CAPTURE_TIMEOUT_MS = 2_000L
        private const val DIGITAL_VERIFY_TIMEOUT_MS = 1_000L
    }
}

fun DigitalTimingVerification.describe(): String {
    val deltaMs = sessionDeltaFrames * 1000.0 / sampleRateHz.toDouble()
    val inputJitterMs = inputJitterNs / 1_000_000.0
    val outputJitterMs = outputJitterNs / 1_000_000.0
    return String.format(
        Locale.US,
        "clocks estáveis · delta %+.2f ms · jitter entrada %.2f ms · saída %.2f ms · %d/%d observações",
        deltaMs,
        inputJitterMs,
        outputJitterMs,
        inputObservations,
        outputObservations,
    )
}

fun LatencyCalibration.describe(): String {
    val latencyMs = latencyFrames * 1000.0 / sampleRateHz
    val jitterMs = jitterFrames * 1000.0 / sampleRateHz
    return String.format(
        Locale.US,
        "%.1f ms · jitter %.1f ms · drift %+.0f ppm · confiança %.0f%% · %d tentativas",
        latencyMs,
        jitterMs,
        driftPpm,
        confidence * 100f,
        attempts,
    )
}
