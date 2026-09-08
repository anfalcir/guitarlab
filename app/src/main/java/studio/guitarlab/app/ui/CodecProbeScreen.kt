package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.platform.codec.android.ContentUriSeekableByteSource
import kotlin.math.abs
import kotlin.math.sqrt

private data class CodecProbeUiResult(
    val success: Boolean,
    val text: String,
)

@Composable
fun CodecProbeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CodecProbeUiResult?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        running = true
        result = null
        scope.launch {
            result = withContext(Dispatchers.IO) {
                runCatching {
                    ContentUriSeekableByteSource(context.contentResolver, uri).use { source ->
                        WavPcmDecoder(source).use { decoder ->
                            val metadata = decoder.metadata
                            val probeFrames = minOf(metadata.totalFrames, 48_000L).toInt().coerceAtMost(8_192)
                            val buffer = FloatArray(probeFrames * metadata.channelCount)
                            val readFrames = decoder.readInterleaved(buffer, frameCount = probeFrames)
                            val sampleCount = readFrames * metadata.channelCount
                            var peak = 0f
                            var sumSquares = 0.0
                            repeat(sampleCount) { index ->
                                val value = buffer[index]
                                peak = maxOf(peak, abs(value))
                                sumSquares += value.toDouble() * value
                            }
                            val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f

                            val midpoint = metadata.totalFrames / 2
                            decoder.seekToFrame(midpoint)
                            val seekBuffer = FloatArray(256 * metadata.channelCount)
                            val seekRead = decoder.readInterleaved(seekBuffer, frameCount = 256)
                            val seekEndedAt = decoder.positionFrames
                            val bitDepth = metadata.bitsPerSample?.toString() ?: "n/a"

                            CodecProbeUiResult(
                                success = readFrames > 0 && seekRead >= 0,
                                text = buildString {
                                    appendLine("M3 WAV codec probe")
                                    appendLine("format=${metadata.fileFormat}")
                                    appendLine("sampleRate=${metadata.sampleRateHz}Hz")
                                    appendLine("channels=${metadata.channelCount}")
                                    appendLine("encoding=${metadata.sampleEncoding}")
                                    appendLine("bits=$bitDepth")
                                    appendLine("frames=${metadata.totalFrames}")
                                    appendLine("durationUs=${metadata.durationUs}")
                                    appendLine("decodedFrames=$readFrames")
                                    appendLine("peakPct=${(peak * 100).toInt()}")
                                    appendLine("rmsPct=${(rms * 100).toInt()}")
                                    appendLine("seekRequested=$midpoint")
                                    appendLine("seekRead=$seekRead")
                                    append("seekEndedAt=$seekEndedAt")
                                },
                            )
                        }
                    }
                }.getOrElse { error ->
                    CodecProbeUiResult(
                        success = false,
                        text = "M3 WAV codec probe failed: ${error.message ?: error::class.java.simpleName}",
                    )
                }
            }
            running = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Codec Diagnostics", style = MaterialTheme.typography.headlineMedium)
        Text(
            "M3 development gate. Select a WAV file to validate Android document access, metadata, PCM decoding, channel order path and random seek. Other V1 formats remain intentionally unadvertised until their own gates pass.",
        )
        Button(
            enabled = !running,
            onClick = { launcher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream")) },
        ) {
            Text(if (running) "Running…" else "Select WAV and run probe")
        }
        result?.let { probe ->
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (probe.success) "PASS" else "FAIL",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (probe.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(probe.text)
                }
            }
        }
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}
