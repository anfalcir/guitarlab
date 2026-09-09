package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.abs
import kotlin.math.sqrt
import studio.guitarlab.core.codec.SampleRateStrategy
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.platform.codec.android.AndroidAudioDocumentSourceFactory

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
                    AndroidAudioDocumentSourceFactory.open(context, uri).use { source ->
                        WavPcmDecoder(source).use { decoder ->
                            val metadata = decoder.metadata
                            val probeFrames = minOf(metadata.totalFrames, 8_192L).toInt()
                            require(probeFrames > 0) { "O WAV não contém frames completos de áudio." }

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
                            val seekRequestFrames = minOf(256L, metadata.totalFrames - midpoint).toInt()
                            val seekBuffer = FloatArray(maxOf(1, seekRequestFrames) * metadata.channelCount)
                            val seekRead = if (seekRequestFrames > 0) {
                                decoder.readInterleaved(seekBuffer, frameCount = seekRequestFrames)
                            } else {
                                0
                            }
                            val seekEndedAt = decoder.positionFrames
                            val expectedSeekEnd = midpoint + seekRead
                            val seekExact = seekEndedAt == expectedSeekEnd
                            val initialDecodeExact = readFrames == probeFrames
                            val bitDepth = metadata.bitsPerSample?.toString() ?: "n/d"
                            val sampleRatePlan = SampleRateStrategy.plan(metadata.sampleRateHz, 48_000)
                            val pass = initialDecodeExact && seekExact

                            CodecProbeUiResult(
                                success = pass,
                                text = buildString {
                                    appendLine("Acesso: ${source.accessMode}")
                                    appendLine("Tamanho: ${source.sizeBytes} bytes")
                                    appendLine("Formato: ${metadata.fileFormat}")
                                    appendLine("Taxa: ${metadata.sampleRateHz} Hz")
                                    appendLine("Canais: ${metadata.channelCount}")
                                    appendLine("Codificação: ${metadata.sampleEncoding}")
                                    appendLine("Bits: $bitDepth")
                                    appendLine("Frames: ${metadata.totalFrames}")
                                    appendLine("Duração: ${metadata.durationUs} µs")
                                    appendLine("Leitura: $readFrames/$probeFrames frames")
                                    appendLine("Pico: ${(peak * 100).toInt()}%")
                                    appendLine("RMS: ${(rms * 100).toInt()}%")
                                    appendLine("Plano de taxa: ${sampleRatePlan.action} ${sampleRatePlan.sourceRateHz}→${sampleRatePlan.targetRateHz}")
                                    appendLine("Seek solicitado: $midpoint")
                                    appendLine("Seek lido: $seekRead/$seekRequestFrames")
                                    appendLine("Seek final: $seekEndedAt · esperado: $expectedSeekEnd")
                                    append("Verificações: leitura exata=$initialDecodeExact · seek exato=$seekExact")
                                },
                            )
                        }
                    }
                }.getOrElse { error ->
                    CodecProbeUiResult(
                        success = false,
                        text = "A análise falhou com segurança: ${error.message ?: error::class.java.simpleName}",
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Diagnóstico de arquivos", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Analise um WAV para conferir leitura, metadados e posicionamento preciso.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onBack)
        }

        Button(
            enabled = !running,
            onClick = { launcher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream")) },
        ) {
            Text(if (running) "Analisando…" else "Selecionar WAV e analisar")
        }

        result?.let { probe ->
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (probe.success) "APROVADO" else "FALHA",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (probe.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(probe.text)
                }
            }
        }
    }
}
