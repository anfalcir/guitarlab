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
import studio.guitarlab.core.audio.LatencyCalibration
import studio.guitarlab.core.audio.LatencyCalibrationPolicy

class StudioLatencyCalibrationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("studio-latency-calibration", Context.MODE_PRIVATE)

    fun save(inputSignature: String?, outputSignature: String?, calibration: LatencyCalibration) {
        if (inputSignature.isNullOrBlank() || outputSignature.isNullOrBlank()) return
        val key = key(inputSignature, outputSignature, calibration.sampleRateHz)
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
        if (inputSignature.isNullOrBlank() || outputSignature.isNullOrBlank()) return null
        val key = key(inputSignature, outputSignature, sampleRateHz)
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

    fun clear(inputSignature: String?, outputSignature: String?, sampleRateHz: Int) {
        val prefix = key(inputSignature, outputSignature, sampleRateHz)
        prefs.edit().also { editor ->
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        }.apply()
    }

    private fun key(input: String?, output: String?, rate: Int): String =
        "${input ?: "auto-in"}|${output ?: "auto-out"}|$rate".hashCode().toUInt().toString(16)
}

class AndroidLatencyCalibrationEngine(private val context: Context) {
    suspend fun calibrate(
        sampleRateHz: Int,
        inputDevice: AudioDeviceInfo?,
        outputDevice: AudioDeviceInfo?,
        attempts: Int = 5,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): LatencyCalibration = withContext(Dispatchers.IO) {
        require(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Permissão de microfone necessária para medir a latência."
        }
        require(sampleRateHz in 8_000..192_000)
        require(attempts in 3..9)
        val measurements = mutableListOf<Long>()
        val confidences = mutableListOf<Float>()
        val startedMs = System.currentTimeMillis()
        repeat(attempts) { index ->
            onProgress(index + 1, attempts)
            val pass = measurePass(sampleRateHz, inputDevice, outputDevice)
            measurements += pass.first
            confidences += pass.second
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

    private fun measurePass(sampleRateHz: Int, inputDevice: AudioDeviceInfo?, outputDevice: AudioDeviceInfo?): Pair<Long, Float> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Permissão de microfone necessária para medir a latência.")
        }
        val template = stimulus(STIMULUS_FRAMES)
        val preRollFrames = sampleRateHz / 10
        val captureFrames = sampleRateHz
        val recorderBufferBytes = max(
            AudioRecord.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT),
            captureFrames * Float.SIZE_BYTES / 2,
        )
        require(recorderBufferBytes > 0) { "A entrada não suporta calibração em $sampleRateHz Hz." }
        val recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
            .setBufferSizeInBytes(recorderBufferBytes)
            .build()
        val outMin = AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        require(outMin > 0) { "A saída não suporta calibração em $sampleRateHz Hz." }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(outMin, (preRollFrames + STIMULUS_FRAMES + sampleRateHz / 3) * 2 * Float.SIZE_BYTES))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        try {
            require(recorder.state == AudioRecord.STATE_INITIALIZED && track.state == AudioTrack.STATE_INITIALIZED) { "Não foi possível inicializar as rotas para calibração." }
            if (inputDevice != null) require(recorder.setPreferredDevice(inputDevice)) { "A entrada selecionada recusou a calibração." }
            if (outputDevice != null) require(track.setPreferredDevice(outputDevice)) { "A saída selecionada recusou a calibração." }
            val captured = FloatArray(captureFrames)
            val outputMono = FloatArray(preRollFrames + STIMULUS_FRAMES + sampleRateHz / 3)
            template.copyInto(outputMono, destinationOffset = preRollFrames)
            val outputStereo = FloatArray(outputMono.size * 2)
            outputMono.forEachIndexed { i, sample -> outputStereo[i * 2] = sample; outputStereo[i * 2 + 1] = sample }

            recorder.startRecording()
            val writer = Thread {
                track.play()
                var offset = 0
                while (offset < outputStereo.size) {
                    val written = track.write(outputStereo, offset, outputStereo.size - offset, AudioTrack.WRITE_BLOCKING)
                    if (written <= 0) break
                    offset += written
                }
            }
            writer.start()
            var read = 0
            while (read < captured.size) {
                val amount = recorder.read(captured, read, captured.size - read, AudioRecord.READ_BLOCKING)
                if (amount <= 0) break
                read += amount
            }
            writer.join(1500L)
            val inputTimestamp = AudioTimestamp()
            val outputTimestamp = AudioTimestamp()
            val hasInputTs = recorder.getTimestamp(inputTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS
            val hasOutputTs = track.getTimestamp(outputTimestamp)
            require(hasInputTs && hasOutputTs) { "O Android não forneceu timestamps de áudio estáveis para esta rota." }
            val match = correlate(captured, read, template)
            require(match.second >= .30f) { "Sinal de loopback não detectado. Conecte/ative o retorno da saída para a entrada e repita." }
            val inputFrame = match.first.toLong()
            val outputFrame = preRollFrames.toLong()
            val inputNs = inputTimestamp.nanoTime + ((inputFrame - inputTimestamp.framePosition) * 1_000_000_000.0 / sampleRateHz).roundToLong()
            val outputNs = outputTimestamp.nanoTime + ((outputFrame - outputTimestamp.framePosition) * 1_000_000_000.0 / sampleRateHz).roundToLong()
            val latencyFrames = (((inputNs - outputNs).coerceAtLeast(0L)) * sampleRateHz / 1_000_000_000.0).roundToLong()
            require(latencyFrames in 0..sampleRateHz.toLong()) { "A medição de latência ficou fora da faixa confiável." }
            return latencyFrames to match.second
        } finally {
            runCatching { recorder.stop() }; runCatching { track.pause() }; runCatching { track.flush() }
            recorder.release(); track.release()
        }
    }

    private fun stimulus(size: Int): FloatArray {
        var state = 0x13579BDF
        return FloatArray(size) {
            state = state xor (state shl 13); state = state xor (state ushr 17); state = state xor (state shl 5)
            if ((state and 1) == 0) 0.62f else -0.62f
        }
    }

    private fun correlate(captured: FloatArray, capturedSize: Int, template: FloatArray): Pair<Int, Float> {
        if (capturedSize <= template.size) return 0 to 0f
        var templateEnergy = 0.0
        template.forEach { templateEnergy += it * it }
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
            val score = if (energy > 1e-9) abs(dot) / sqrt(energy * templateEnergy / step) else 0.0
            if (score > bestScore) { bestScore = score; bestIndex = offset }
            offset += step
        }
        return bestIndex to bestScore.coerceIn(0.0, 1.0).toFloat()
    }

    companion object { private const val STIMULUS_FRAMES = 1024 }
}

fun LatencyCalibration.describe(): String {
    val latencyMs = latencyFrames * 1000.0 / sampleRateHz
    val jitterMs = jitterFrames * 1000.0 / sampleRateHz
    return String.format(Locale.US, "%.1f ms · jitter %.1f ms · confiança %.0f%%", latencyMs, jitterMs, confidence * 100f)
}
