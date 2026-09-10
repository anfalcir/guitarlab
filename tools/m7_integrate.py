from pathlib import Path

ROOT = Path('.')

def read(path): return (ROOT / path).read_text()
def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)
def replace(path, old, new, count=1):
    text = read(path)
    found = text.count(old)
    if found < count:
        raise SystemExit(f"{path}: anchor not found enough times ({found} < {count})")
    write(path, text.replace(old, new, count))

# ---- model: additive, backwards-compatible M7 editing metadata ----
replace('core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt',
'''    val sourceEncoding: String? = null,
    val sourceTotalFrames: Long? = null,
)''',
'''    val sourceEncoding: String? = null,
    val sourceTotalFrames: Long? = null,
    /** Editing/proxy domain. Defaults keep schema-1 projects readable. */
    val editingSampleRateHz: Int? = null,
    val editingTotalFrames: Long? = null,
    val fadeInFrames: Long = 0,
    val fadeOutFrames: Long = 0,
)''')

# ---- fade policy shared by realtime and offline render ----
write('core/audio/src/main/kotlin/studio/guitarlab/core/audio/ClipFadePolicy.kt', r'''package studio.guitarlab.core.audio

import kotlin.math.min

/** Deterministic linear clip-envelope shared by realtime playback and offline master rendering. */
object ClipFadePolicy {
    fun gain(localFrame: Long, clipLengthFrames: Long, fadeInFrames: Long, fadeOutFrames: Long): Float {
        if (clipLengthFrames <= 0L) return 0f
        val frame = localFrame.coerceIn(0L, clipLengthFrames - 1L)
        val fadeIn = fadeInFrames.coerceIn(0L, clipLengthFrames)
        val fadeOut = fadeOutFrames.coerceIn(0L, clipLengthFrames)
        var gain = 1f
        if (fadeIn > 0L && frame < fadeIn) gain = min(gain, frame.toFloat() / fadeIn.toFloat())
        val remaining = clipLengthFrames - 1L - frame
        if (fadeOut > 0L && remaining < fadeOut) gain = min(gain, remaining.toFloat() / fadeOut.toFloat())
        return gain.coerceIn(0f, 1f)
    }
}
''')
write('core/audio/src/test/kotlin/studio/guitarlab/core/audio/ClipFadePolicyTest.kt', r'''package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipFadePolicyTest {
    @Test fun fadeInAndOutAreBoundedAndDeterministic() {
        assertEquals(0f, ClipFadePolicy.gain(0, 100, 10, 10))
        assertTrue(ClipFadePolicy.gain(5, 100, 10, 10) in 0.49f..0.51f)
        assertEquals(1f, ClipFadePolicy.gain(50, 100, 10, 10))
        assertTrue(ClipFadePolicy.gain(95, 100, 10, 10) < 0.5f)
    }
}
''')

# ---- robust bounded-memory band-limited WAV sample-rate conversion ----
write('core/codec/src/main/kotlin/studio/guitarlab/core/codec/WavSampleRateConverter.kt', r'''package studio.guitarlab.core.codec

import java.io.File
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Offline, bounded-memory windowed-sinc sample-rate converter for project editing proxies.
 * Immutable imported sources are never rewritten; callers store this output as a derived proxy.
 */
object WavSampleRateConverter {
    data class Result(
        val sourceRateHz: Int,
        val targetRateHz: Int,
        val sourceFrames: Long,
        val targetFrames: Long,
        val channelCount: Int,
    )

    fun convert(input: File, output: File, targetRateHz: Int): Result {
        FileSeekableByteSource(input).use { source ->
            val decoder = WavPcmDecoder(source)
            decoder.use {
                val meta = decoder.metadata
                require(meta.channelCount in 1..2) { "Resampling supports mono/stereo WAV editing media." }
                require(targetRateHz in 8_000..192_000) { "Unsupported target sample rate: $targetRateHz" }
                if (meta.sampleRateHz == targetRateHz) {
                    input.inputStream().use { i -> output.outputStream().use { o -> i.copyTo(o) } }
                    return Result(meta.sampleRateHz, targetRateHz, meta.totalFrames, meta.totalFrames, meta.channelCount)
                }
                val targetFrames = (meta.totalFrames.toDouble() * targetRateHz / meta.sampleRateHz).roundToLong().coerceAtLeast(1L)
                FloatWavFileWriter(output, targetRateHz, meta.channelCount).use { writer ->
                    var outStart = 0L
                    while (outStart < targetFrames) {
                        val outCount = min(OUTPUT_CHUNK.toLong(), targetFrames - outStart).toInt()
                        val firstPos = outStart.toDouble() * meta.sampleRateHz / targetRateHz
                        val lastPos = (outStart + outCount - 1L).toDouble() * meta.sampleRateHz / targetRateHz
                        val sourceStart = (floor(firstPos).toLong() - RADIUS).coerceAtLeast(0L)
                        val sourceEnd = (ceil(lastPos).toLong() + RADIUS + 1L).coerceAtMost(meta.totalFrames)
                        val sourceCount = (sourceEnd - sourceStart).toInt()
                        val sourceSamples = FloatArray(sourceCount * meta.channelCount)
                        decoder.seekToFrame(sourceStart)
                        val actual = decoder.readInterleaved(sourceSamples, frameCount = sourceCount)
                        val out = FloatArray(outCount * meta.channelCount)
                        val cutoff = min(1.0, targetRateHz.toDouble() / meta.sampleRateHz.toDouble())
                        for (outIndex in 0 until outCount) {
                            val sourcePos = (outStart + outIndex).toDouble() * meta.sampleRateHz / targetRateHz
                            val center = floor(sourcePos).toLong()
                            for (channel in 0 until meta.channelCount) {
                                var sum = 0.0
                                var weights = 0.0
                                for (tap in -RADIUS..RADIUS) {
                                    val absolute = center + tap
                                    if (absolute < sourceStart || absolute >= sourceStart + actual) continue
                                    val distance = sourcePos - absolute.toDouble()
                                    val x = distance * cutoff
                                    val sinc = if (kotlin.math.abs(x) < 1e-12) 1.0 else sin(PI * x) / (PI * x)
                                    val normalizedDistance = kotlin.math.abs(distance) / (RADIUS + 1.0)
                                    if (normalizedDistance >= 1.0) continue
                                    val window = 0.5 * (1.0 + cos(PI * normalizedDistance))
                                    val weight = sinc * window * cutoff
                                    val local = (absolute - sourceStart).toInt()
                                    sum += sourceSamples[local * meta.channelCount + channel] * weight
                                    weights += weight
                                }
                                out[outIndex * meta.channelCount + channel] =
                                    if (kotlin.math.abs(weights) > 1e-12) (sum / weights).toFloat().coerceIn(-1f, 1f) else 0f
                            }
                        }
                        writer.writeInterleaved(out, outCount)
                        outStart += outCount
                    }
                }
                return Result(meta.sampleRateHz, targetRateHz, meta.totalFrames, targetFrames, meta.channelCount)
            }
        }
    }

    private const val RADIUS = 16
    private const val OUTPUT_CHUNK = 4096
}
''')
write('core/codec/src/test/kotlin/studio/guitarlab/core/codec/WavSampleRateConverterTest.kt', r'''package studio.guitarlab.core.codec

import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.io.path.createTempDirectory

class WavSampleRateConverterTest {
    @Test fun resamples44100To48000WithStableDurationAndTone() {
        val dir = createTempDirectory("guitarlab-resample-").toFile()
        val input = File(dir, "in.wav")
        val output = File(dir, "out.wav")
        val rate = 44_100
        val frames = rate
        FloatWavFileWriter(input, rate, 1).use { writer ->
            val samples = FloatArray(frames) { i -> (0.6 * sin(2.0 * PI * 1000.0 * i / rate)).toFloat() }
            writer.writeInterleaved(samples, frames)
        }
        val result = WavSampleRateConverter.convert(input, output, 48_000)
        assertEquals(48_000, result.targetRateHz)
        assertTrue(abs(result.targetFrames - 48_000L) <= 1L)
        FileSeekableByteSource(output).use { source ->
            val decoder = WavPcmDecoder(source)
            assertEquals(48_000, decoder.metadata.sampleRateHz)
            assertTrue(abs(decoder.metadata.totalFrames - 48_000L) <= 1L)
            val probe = FloatArray(4_800)
            decoder.readInterleaved(probe, frameCount = probe.size)
            val rms = kotlin.math.sqrt(probe.map { it * it }.average()).toFloat()
            assertTrue(rms in 0.35f..0.5f)
        }
        dir.deleteRecursively()
    }

    @Test fun downsamplingKeepsExpectedDuration() {
        val dir = createTempDirectory("guitarlab-resample-down-").toFile()
        val input = File(dir, "in.wav")
        val output = File(dir, "out.wav")
        FloatWavFileWriter(input, 48_000, 2).use { writer ->
            val samples = FloatArray(48_000 * 2) { i -> if (i % 2 == 0) 0.25f else -0.25f }
            writer.writeInterleaved(samples, 48_000)
        }
        val result = WavSampleRateConverter.convert(input, output, 44_100)
        assertTrue(abs(result.targetFrames - 44_100L) <= 1L)
        assertEquals(2, result.channelCount)
        dir.deleteRecursively()
    }
}
''')

# ---- editor fade/crossfade semantics ----
replace('core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectClipEditor.kt',
'''    /** Non-destructive trim: source media is never rewritten. */''',
'''    fun setClipFades(
        project: GuitarProject,
        clipId: String,
        fadeInFrames: Long,
        fadeOutFrames: Long,
        nowEpochMs: Long,
    ): GuitarProject {
        require(fadeInFrames >= 0L && fadeOutFrames >= 0L) { "Fade durations must be non-negative." }
        val source = project.clips.firstOrNull { it.id == clipId } ?: error("Clip '$clipId' not found.")
        require(fadeInFrames <= source.lengthFrames && fadeOutFrames <= source.lengthFrames) { "Fade exceeds clip duration." }
        return project.copy(
            clips = project.clips.map { if (it.id == clipId) it.copy(fadeInFrames = fadeInFrames, fadeOutFrames = fadeOutFrames) else it },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun crossfadeOverlappingClips(project: GuitarProject, firstClipId: String, secondClipId: String, nowEpochMs: Long): GuitarProject {
        val a = project.clips.firstOrNull { it.id == firstClipId } ?: error("Clip '$firstClipId' not found.")
        val b = project.clips.firstOrNull { it.id == secondClipId } ?: error("Clip '$secondClipId' not found.")
        require(a.trackId == b.trackId) { "Crossfade requires clips on the same track." }
        val ordered = listOf(a, b).sortedBy { it.startFrame }
        val left = ordered[0]
        val right = ordered[1]
        val overlapStart = maxOf(left.startFrame, right.startFrame)
        val overlapEnd = minOf(left.startFrame + left.lengthFrames, right.startFrame + right.lengthFrames)
        require(overlapEnd > overlapStart) { "Crossfade requires overlapping clips." }
        val overlap = overlapEnd - overlapStart
        return project.copy(
            clips = project.clips.map { clip ->
                when (clip.id) {
                    left.id -> clip.copy(fadeOutFrames = overlap.coerceAtMost(clip.lengthFrames))
                    right.id -> clip.copy(fadeInFrames = overlap.coerceAtMost(clip.lengthFrames))
                    else -> clip
                }
            },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    /** Non-destructive trim: source media is never rewritten. */''')
replace('core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectClipEditor.kt',
'''        target.sourceTotalFrames?.let { total ->
            require(sourceStartFrame + lengthFrames <= total) { "Trim exceeds immutable source bounds." }
        }''',
'''        (target.editingTotalFrames ?: target.sourceTotalFrames)?.let { total ->
            require(sourceStartFrame + lengthFrames <= total) { "Trim exceeds editing-media bounds." }
        }''', 1)
replace('core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectClipEditor.kt',
'''        target.sourceTotalFrames?.let { total ->
            require(newSourceStart + newLength <= total) { "Trim exceeds immutable source bounds." }
        }''',
'''        (target.editingTotalFrames ?: target.sourceTotalFrames)?.let { total ->
            require(newSourceStart + newLength <= total) { "Trim exceeds editing-media bounds." }
        }''', 1)

# ---- realtime render: fade envelope + bounded scratch buffers ----
p='platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt'
replace(p, 'import studio.guitarlab.core.audio.PlaybackClockPolicy\n', 'import studio.guitarlab.core.audio.PlaybackClockPolicy\nimport studio.guitarlab.core.audio.ClipFadePolicy\n')
replace(p, '''    val pan: Float = 0f,
    val muted: Boolean = false,
)''', '''    val pan: Float = 0f,
    val muted: Boolean = false,
    val fadeInFrames: Long = 0,
    val fadeOutFrames: Long = 0,
)''')
replace(p, '''        private val stereoGain = TrackMixPolicy.channelGains(clip.gainDb, clip.pan)
''', '''        private val stereoGain = TrackMixPolicy.channelGains(clip.gainDb, clip.pan)
        private var scratch = FloatArray(0)
''')
replace(p, '''            val temp = FloatArray(requested * channels)
            val decoded = decoder.readInterleaved(temp, frameCount = requested)
''', '''            val requiredSamples = requested * channels
            if (scratch.size < requiredSamples) scratch = FloatArray(requiredSamples)
            val decoded = decoder.readInterleaved(scratch, frameCount = requested)
''')
replace(p, '''                    val sample = temp[frame]
                    destinationStereo[dst] += sample * stereoGain.left
                    destinationStereo[dst + 1] += sample * stereoGain.right
''', '''                    val localFrame = overlapStart - clip.timelineStartFrame + frame
                    val envelope = ClipFadePolicy.gain(localFrame, clip.lengthFrames, clip.fadeInFrames, clip.fadeOutFrames)
                    val sample = scratch[frame] * envelope
                    destinationStereo[dst] += sample * stereoGain.left
                    destinationStereo[dst + 1] += sample * stereoGain.right
''')
replace(p, '''                    val src = frame * 2
                    destinationStereo[dst] += temp[src] * stereoGain.left
                    destinationStereo[dst + 1] += temp[src + 1] * stereoGain.right
''', '''                    val src = frame * 2
                    val localFrame = overlapStart - clip.timelineStartFrame + frame
                    val envelope = ClipFadePolicy.gain(localFrame, clip.lengthFrames, clip.fadeInFrames, clip.fadeOutFrames)
                    destinationStereo[dst] += scratch[src] * stereoGain.left * envelope
                    destinationStereo[dst + 1] += scratch[src + 1] * stereoGain.right * envelope
''')

# ---- offline renderer parity + bounded scratch ----
p='platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/StudioMasterRenderer.kt'
replace(p, 'import studio.guitarlab.core.audio.TrackMixPolicy\n', 'import studio.guitarlab.core.audio.TrackMixPolicy\nimport studio.guitarlab.core.audio.ClipFadePolicy\n')
replace(p, '''    val gainDb: Float = 0f,
)''', '''    val gainDb: Float = 0f,
    val fadeInFrames: Long = 0,
    val fadeOutFrames: Long = 0,
)''', 1)
replace(p, '''        private val clipGain = TrackMixPolicy.channelGains(clip.gainDb, 0f)
''', '''        private val clipGain = TrackMixPolicy.channelGains(clip.gainDb, 0f)
        private var scratch = FloatArray(0)
''')
replace(p, '''            val temp = FloatArray(requested * channels)
            val decoded = decoder.readInterleaved(temp, frameCount = requested)
''', '''            val requiredSamples = requested * channels
            if (scratch.size < requiredSamples) scratch = FloatArray(requiredSamples)
            val decoded = decoder.readInterleaved(scratch, frameCount = requested)
''')
replace(p, '''                    val sample = temp[frame]
                    destination[dst] += sample * clipGain.left
                    destination[dst + 1] += sample * clipGain.right
''', '''                    val localFrame = overlapStart - clip.timelineStartFrame + frame
                    val envelope = ClipFadePolicy.gain(localFrame, clip.lengthFrames, clip.fadeInFrames, clip.fadeOutFrames)
                    val sample = scratch[frame] * envelope
                    destination[dst] += sample * clipGain.left
                    destination[dst + 1] += sample * clipGain.right
''')
replace(p, '''                    destination[dst] += temp[frame * 2] * clipGain.left
                    destination[dst + 1] += temp[frame * 2 + 1] * clipGain.right
''', '''                    val localFrame = overlapStart - clip.timelineStartFrame + frame
                    val envelope = ClipFadePolicy.gain(localFrame, clip.lengthFrames, clip.fadeInFrames, clip.fadeOutFrames)
                    destination[dst] += scratch[frame * 2] * clipGain.left * envelope
                    destination[dst + 1] += scratch[frame * 2 + 1] * clipGain.right * envelope
''')

# ---- Home export service: same backend semantics as Studio ----
write('app/src/main/java/studio/guitarlab/app/ui/ProjectExportService.kt', r'''package studio.guitarlab.app.ui

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleWriter
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.platform.audio.android.StudioMasterRenderClip
import studio.guitarlab.platform.audio.android.StudioMasterRenderRequest
import studio.guitarlab.platform.audio.android.StudioMasterRenderTrack
import studio.guitarlab.platform.audio.android.StudioMasterRenderer
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

class ProjectExportService(private val context: Context) {
    private val repository = FileProjectRepository(context.filesDir)
    private val mediaStore = ProjectManagedMediaStore(context.filesDir)

    suspend fun saveProject(projectId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val output = context.contentResolver.openOutputStream(uri, "w") ?: error("O Android não conseguiu criar o arquivo do projeto.")
        output.use { ProjectBundleWriter().write(project, mediaStore.projectDirectoryForExport(project.id), it) }
    }

    suspend fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val rate = project.sampleRate.fixedHz ?: project.clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
            ?: error("Projeto sem taxa de amostragem exportável.")
        val request = buildRequest(project, rate)
        val floatWav = File.createTempFile("guitarlab-home-master-", ".wav", context.cacheDir)
        val encoded = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else File.createTempFile("guitarlab-home-master-", ".${format.extension}", context.cacheDir)
        try {
            StudioMasterRenderer.renderFloatWav(request, floatWav)
            if (format != MasterExportFormat.WAV_FLOAT32) AndroidMasterAudioEncoder.encode(floatWav, encoded, format)
            val source = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else encoded
            val output = context.contentResolver.openOutputStream(uri, "w") ?: error("O Android não conseguiu criar o arquivo exportado.")
            output.use { target -> source.inputStream().buffered().use { it.copyTo(target) } }
        } finally {
            floatWav.delete()
            if (encoded != floatWav) encoded.delete()
        }
    }

    private fun buildRequest(project: GuitarProject, rate: Int): StudioMasterRenderRequest {
        val anySolo = project.tracks.any { it.solo }
        val audibleTracks = project.tracks.filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }
        val ids = audibleTracks.map { it.id }.toSet()
        val clips = project.clips.mapNotNull { clip ->
            if (clip.muted || clip.trackId !in ids) return@mapNotNull null
            val path = editingPath(clip) ?: return@mapNotNull null
            val editingRate = clip.editingSampleRateHz ?: clip.sourceSampleRateHz
            require(editingRate == rate) { "Há clipe sem conversão para a taxa do projeto." }
            StudioMasterRenderClip(
                file = mediaStore.resolveEditable(project.id, path),
                trackId = clip.trackId,
                timelineStartFrame = clip.startFrame,
                sourceStartFrame = clip.sourceStartFrame,
                lengthFrames = clip.lengthFrames,
                gainDb = clip.gainDb,
                fadeInFrames = clip.fadeInFrames,
                fadeOutFrames = clip.fadeOutFrames,
            )
        }
        require(clips.isNotEmpty()) { "Não há áudio audível para exportar." }
        return StudioMasterRenderRequest(
            sampleRateHz = rate,
            projectEndFrame = TimelineControlPolicy.projectEndFrame(project),
            clips = clips,
            trackMixes = audibleTracks.map { StudioMasterRenderTrack(it.id, it.gainDb, it.pan) },
            masterGainDb = project.masterGainDb,
        )
    }

    private fun editingPath(clip: AudioClip): String? = clip.managedEditProxyPath ?: clip.managedSourcePath
}
''')

# ---- Home ViewModel: rename + export operations ----
p='app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt'
replace(p, 'import studio.guitarlab.core.project.ProjectBundleReader\n', 'import studio.guitarlab.core.project.ProjectBundleReader\nimport studio.guitarlab.platform.codec.android.MasterExportFormat\n')
replace(p, '''    val error: String? = null,
)''', '''    val error: String? = null,
    val exportBusy: Boolean = false,
    val message: String? = null,
)''')
replace(p, '''    private val factory = ProjectFactory()
''', '''    private val factory = ProjectFactory()
    private val exportService = ProjectExportService(application)
''')
replace(p, '''    fun duplicateProject(project: GuitarProject) {''', r'''    fun renameProject(projectId: String, name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank() || normalized.length > 80) {
            _state.update { it.copy(error = "O nome do projeto deve ter entre 1 e 80 caracteres.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val project = repository.load(projectId) ?: error("Projeto não encontrado.")
                    repository.save(project.copy(name = normalized, updatedAtEpochMs = System.currentTimeMillis()))
                }
            }.onSuccess { refresh() }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível renomear o projeto.") } }
        }
    }

    fun saveProjectPackage(projectId: String, uri: Uri) = launchExport("Projeto GuitarLab salvo com sucesso") {
        exportService.saveProject(projectId, uri)
    }

    fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) = launchExport("Master ${format.name} exportado com sucesso") {
        exportService.exportMaster(projectId, uri, format)
    }

    fun clearMessage() { _state.update { it.copy(message = null, error = null) } }

    private fun launchExport(success: String, action: suspend () -> Unit) {
        if (_state.value.exportBusy) return
        viewModelScope.launch {
            _state.update { it.copy(exportBusy = true, error = null, message = null) }
            runCatching { action() }
                .onSuccess { _state.update { it.copy(exportBusy = false, message = success) } }
                .onFailure { error -> _state.update { it.copy(exportBusy = false, error = error.message ?: "Não foi possível exportar o projeto.") } }
        }
    }

    fun duplicateProject(project: GuitarProject) {''')

# ---- Home UI rewrite with shared dialogs and SAF launchers ----
write('app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt', r'''package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.platform.codec.android.MasterExportFormat

@Composable
fun HomeScreen(viewModel: HomeViewModel, onNewProject: () -> Unit, onOpenProject: (String) -> Unit, onSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renameProject by remember { mutableStateOf<GuitarProject?>(null) }
    var exportProject by remember { mutableStateOf<GuitarProject?>(null) }
    var pendingProjectId by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val projectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.importProject(uri, onOpenProject) }
    val projectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.saveProjectPackage(id, uri) }; pendingProjectId = null }
    val wavLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.exportMaster(id, uri, MasterExportFormat.WAV_FLOAT32) }; pendingProjectId = null }
    val flacLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.exportMaster(id, uri, MasterExportFormat.FLAC) }; pendingProjectId = null }
    val mp3Launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.exportMaster(id, uri, MasterExportFormat.MP3) }; pendingProjectId = null }

    LaunchedEffect(state.message, state.error) {
        val message = state.error ?: state.message
        if (message != null) { snackbar.showSnackbar(message); viewModel.clearMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("GuitarLab", style = MaterialTheme.typography.headlineLarge)
                    Text("Pratique · grave · compare", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppIconButton(icon = Icons.Default.Tune, contentDescription = "Opções", onClick = onSettings)
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f), tonalElevation = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Seu espaço para tocar e evoluir", style = MaterialTheme.typography.headlineSmall)
                        Text("Abra um projeto e trabalhe direto na música, com timeline, mixer e edição no mesmo fluxo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.padding(start = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { projectPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.FolderOpen, null); Text("Abrir projeto", Modifier.padding(start = 6.dp)) }
                        Button(onClick = onNewProject, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.Add, null); Text("Novo projeto", Modifier.padding(start = 6.dp)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) { Text("Projetos recentes", style = MaterialTheme.typography.titleLarge); Text("Continue de onde parou", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (state.projects.isNotEmpty()) Text("${state.projects.size} no total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.projects.isEmpty() -> EmptyProjectsState(onNewProject, Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.projects, key = { it.id }) { project ->
                        ProjectRow(project, { onOpenProject(project.id) }, { renameProject = project }, { exportProject = project }, { viewModel.duplicateProject(project) }, { viewModel.deleteProject(project.id) })
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        if (state.exportBusy) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }

    renameProject?.let { project -> RenameProjectDialog(project.name, { renameProject = null }) { name -> viewModel.renameProject(project.id, name); renameProject = null } }
    exportProject?.let { project ->
        val safe = project.name.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "GuitarLab" }
        SaveAndExportDialog(project.name, state.exportBusy, { if (!state.exportBusy) exportProject = null },
            onSaveProject = { exportProject = null; pendingProjectId = project.id; projectLauncher.launch("$safe.guitarlab") },
            onWav = { exportProject = null; pendingProjectId = project.id; wavLauncher.launch("$safe-master.wav") },
            onFlac = { exportProject = null; pendingProjectId = project.id; flacLauncher.launch("$safe-master.flac") },
            onMp3 = { exportProject = null; pendingProjectId = project.id; mp3Launcher.launch("$safe-master.mp3") })
    }
}

@Composable private fun EmptyProjectsState(onNewProject: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Nenhum projeto ainda", style = MaterialTheme.typography.titleLarge); Text("Crie um projeto vazio ou abra um arquivo .guitarlab salvo anteriormente.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onNewProject) { Text("Criar primeiro projeto") } }
    }
}

@Composable private fun ProjectRow(project: GuitarProject, onOpen: () -> Unit, onRename: () -> Unit, onExport: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Surface(Modifier.fillMaxWidth().clip(shape).clickable(onClick = onOpen), shape = shape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.MusicNote, null, Modifier.padding(12.dp)) }
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(project.updatedAtEpochMs))
                Text("$modified  ·  ${project.tracks.size} ${if (project.tracks.size == 1) "pista" else "pistas"}  ·  ${project.clips.size} ${if (project.clips.size == 1) "clipe" else "clipes"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                AppIconButton(Icons.Default.MoreVert, "Mais ações") { menuOpen = true }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Renomear") }, { menuOpen = false; onRename() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem({ Text("Salvar e exportar") }, { menuOpen = false; onExport() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    DropdownMenuItem({ Text("Duplicar") }, { menuOpen = false; onDuplicate() }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    DropdownMenuItem({ Text("Excluir") }, { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}
''')

# Make the Studio dialogs shared by package (same modal implementation used by Home).
p='app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt'
replace(p, 'private fun RenameProjectDialog(', 'fun RenameProjectDialog(')
replace(p, 'private fun SaveAndExportDialog(', 'fun SaveAndExportDialog(')

# ---- StudioViewModel: resample at ingest, editing-rate readiness, fades/crossfade, render metadata ----
p='app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt'
replace(p, 'import studio.guitarlab.core.codec.StereoWavChannelSplitter\n', 'import studio.guitarlab.core.codec.StereoWavChannelSplitter\nimport studio.guitarlab.core.codec.WavSampleRateConverter\n')
# Replace post-transcode metadata/envelope block with optional resample.
old='''                        val metadata = FileSeekableByteSource(editingFile).use { WavMetadataReader().read(it) }
                        require(metadata.totalFrames > 0) { "O arquivo selecionado não contém áudio completo." }
                        val clipId = UUID.randomUUID().toString()
                        val envelope = FileSeekableByteSource(editingFile).use { source ->
                            WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                        }
                        waveformCache.write(current.id, clipId, envelope)
                        val sourceBits = if (originalFormat == AudioImportFormat.WAV_PCM) metadata.bitsPerSample else null
                        val sourceEncoding = if (originalFormat == AudioImportFormat.WAV_PCM) metadata.sampleEncoding.name else "COMPRESSED"
                        val clip = AudioClip(
'''
new='''                        val sourceMetadata = FileSeekableByteSource(editingFile).use { WavMetadataReader().read(it) }
                        require(sourceMetadata.totalFrames > 0) { "O arquivo selecionado não contém áudio completo." }
                        val existingRate = current.clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
                        val targetRate = current.sampleRate.fixedHz ?: existingRate ?: when (sourceMetadata.sampleRateHz) {
                            44_100, 48_000, 88_200, 96_000 -> sourceMetadata.sampleRateHz
                            else -> 48_000
                        }
                        var finalEditingFile = editingFile
                        var finalProxyPath = proxyPath
                        var resampleTemp: File? = null
                        if (sourceMetadata.sampleRateHz != targetRate) {
                            resampleTemp = File.createTempFile("guitarlab-resample-", ".wav", getApplication<Application>().cacheDir)
                            WavSampleRateConverter.convert(editingFile, resampleTemp, targetRate)
                            val resampledProxy = resampleTemp.inputStream().buffered().use { mediaStore.ingestEditProxy(current.id, "${displayName}-sr${targetRate}.wav", it) }
                            finalProxyPath?.let { oldPath -> if (oldPath != resampledProxy.relativePath) mediaStore.discardUncommitted(current.id, oldPath) }
                            finalProxyPath = resampledProxy.relativePath
                            proxyPath = finalProxyPath
                            finalEditingFile = resampledProxy.file
                        }
                        resampleTemp?.delete()
                        val metadata = FileSeekableByteSource(finalEditingFile).use { WavMetadataReader().read(it) }
                        val clipId = UUID.randomUUID().toString()
                        val envelope = FileSeekableByteSource(finalEditingFile).use { source -> WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS) }
                        waveformCache.write(current.id, clipId, envelope)
                        val sourceBits = if (originalFormat == AudioImportFormat.WAV_PCM) sourceMetadata.bitsPerSample else null
                        val sourceEncoding = if (originalFormat == AudioImportFormat.WAV_PCM) sourceMetadata.sampleEncoding.name else "COMPRESSED"
                        val clip = AudioClip(
'''
replace(p, old, new)
replace(p, '''                            managedEditProxyPath = proxyPath,
''', '''                            managedEditProxyPath = finalProxyPath,
''', 1)
replace(p, '''                            sourceSampleRateHz = metadata.sampleRateHz,
                            sourceChannelCount = metadata.channelCount,
                            sourceBitsPerSample = sourceBits,
                            sourceEncoding = sourceEncoding,
                            sourceTotalFrames = metadata.totalFrames,
''', '''                            sourceSampleRateHz = sourceMetadata.sampleRateHz,
                            sourceChannelCount = sourceMetadata.channelCount,
                            sourceBitsPerSample = sourceBits,
                            sourceEncoding = sourceEncoding,
                            sourceTotalFrames = sourceMetadata.totalFrames,
                            editingSampleRateHz = metadata.sampleRateHz,
                            editingTotalFrames = metadata.totalFrames,
''')
# Import status should report project/editing rate, not raw source rate only.
replace(p, '''                    importStatus = "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${outcome.sampleRateHz} Hz",
''', '''                    importStatus = if (imported.sourceSampleRateHz != imported.editingSampleRateHz) "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${imported.sourceSampleRateHz} → ${imported.editingSampleRateHz} Hz" else "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${outcome.sampleRateHz} Hz",
''')
# Stereo derived clips are edit proxies at existing edit rate.
replace(p, '''                                sourceEncoding = "IEEE_FLOAT · canal L derivado",
                            )''', '''                                sourceEncoding = "IEEE_FLOAT · canal L derivado",
                                editingSampleRateHz = sourceClip.editingSampleRateHz ?: sourceClip.sourceSampleRateHz,
                                editingTotalFrames = sourceClip.editingTotalFrames ?: sourceClip.lengthFrames,
                            )''')
replace(p, '''                                sourceEncoding = "IEEE_FLOAT · canal R derivado",
                            )''', '''                                sourceEncoding = "IEEE_FLOAT · canal R derivado",
                                editingSampleRateHz = sourceClip.editingSampleRateHz ?: sourceClip.sourceSampleRateHz,
                                editingTotalFrames = sourceClip.editingTotalFrames ?: sourceClip.lengthFrames,
                            )''')
# Playback clip fade metadata, both normal and record backing constructors.
replace(p, '''                        pan = 0f,
                        muted = false,
                    )''', '''                        pan = 0f,
                        muted = false,
                        fadeInFrames = clip.fadeInFrames,
                        fadeOutFrames = clip.fadeOutFrames,
                    )''', 1)
replace(p, '''                    gainDb = clip.gainDb,
                )''', '''                    gainDb = clip.gainDb,
                    fadeInFrames = clip.fadeInFrames,
                    fadeOutFrames = clip.fadeOutFrames,
                )''', 1)
# Recording backing mismatch check must use editing rate.
replace(p, '''                if (clip.muted || clip.sourceSampleRateHz != sampleRateHz) return@mapNotNull null
''', '''                if (clip.muted || (clip.editingSampleRateHz ?: clip.sourceSampleRateHz) != sampleRateHz) return@mapNotNull null
''')
# Readiness based on editing proxy rate.
replace(p, '''        val firstRate = audible.first().sourceSampleRateHz ?: return PlaybackReadiness(false, reason = "A taxa de amostragem do áudio é desconhecida.")
        val projectRate = project.sampleRate.fixedHz ?: firstRate
        if (audible.any { it.sourceSampleRateHz != projectRate }) {
            return PlaybackReadiness(false, reason = "Há clipes com taxa de amostragem diferente da taxa do projeto.")
        }
''', '''        val firstRate = audible.first().let { it.editingSampleRateHz ?: it.sourceSampleRateHz } ?: return PlaybackReadiness(false, reason = "A taxa de amostragem do áudio é desconhecida.")
        val projectRate = project.sampleRate.fixedHz ?: firstRate
        if (audible.any { (it.editingSampleRateHz ?: it.sourceSampleRateHz) != projectRate }) {
            return PlaybackReadiness(false, reason = "Há clipes sem proxy convertido para a taxa do projeto.")
        }
''')
# Offline render fades.
replace(p, '''                gainDb = clip.gainDb,
            )''', '''                gainDb = clip.gainDb,
                fadeInFrames = clip.fadeInFrames,
                fadeOutFrames = clip.fadeOutFrames,
            )''', 1)
# Add fade/crossfade public actions before moveClipToTrack.
replace(p, '''    fun moveClipToTrack(clipId: String, targetTrackId: String) {''', r'''    fun setClipFades(clipId: String, fadeInFrames: Long, fadeOutFrames: Long) {
        editClip("Fades do clipe atualizados") { current ->
            ProjectClipEditor.setClipFades(current, clipId, fadeInFrames, fadeOutFrames, System.currentTimeMillis())
        }
    }

    fun crossfadeWithNext(clipId: String) {
        val project = _state.value.project ?: return
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return
        val next = project.clips.filter { it.trackId == clip.trackId && it.id != clip.id && it.startFrame >= clip.startFrame }
            .minByOrNull { it.startFrame } ?: run {
                _state.value = _state.value.copy(error = "Não há outro clipe sobreposto à direita para crossfade.")
                return
            }
        editClip("Crossfade aplicado") { current -> ProjectClipEditor.crossfadeOverlappingClips(current, clipId, next.id, System.currentTimeMillis()) }
    }

    fun moveClipToTrack(clipId: String, targetTrackId: String) {''')

# ---- Clip UI: fade dialog and crossfade action ----
p='app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt'
# add state + callbacks
replace(p, '''    var settingsTrackId by remember { mutableStateOf<String?>(null) }
''', '''    var settingsTrackId by remember { mutableStateOf<String?>(null) }
    var fadeClipId by remember { mutableStateOf<String?>(null) }
''')
replace(p, '''                onSplitStereo = viewModel::separateStereoClip,
''', '''                onSplitStereo = viewModel::separateStereoClip,
                onOpenFades = { fadeClipId = it },
                onCrossfade = viewModel::crossfadeWithNext,
''')
# Add fade dialog before stereo prompt
anchor='''    state.stereoImportPrompt?.let { prompt ->'''
insert=r'''    fadeClipId?.let { id ->
        project?.clips?.firstOrNull { it.id == id }?.let { clip ->
            ClipFadeDialog(
                clip = clip,
                sampleRateHz = clip.editingSampleRateHz ?: clip.sourceSampleRateHz ?: project.sampleRate.fixedHz ?: 48_000,
                onDismiss = { fadeClipId = null },
                onApply = { fadeIn, fadeOut -> viewModel.setClipFades(clip.id, fadeIn, fadeOut); fadeClipId = null },
            )
        }
    }

    state.stereoImportPrompt?.let { prompt ->'''
replace(p, anchor, insert)
# Function signature callbacks
replace(p, '''    onSplitStereo: (String) -> Unit,
    onReorderTrack:''', '''    onSplitStereo: (String) -> Unit,
    onOpenFades: (String) -> Unit,
    onCrossfade: (String) -> Unit,
    onReorderTrack:''')
# Find subsequent TrackLane call callback args; there is likely TrackLane function call with onSplitStereo. replace all remaining relevant occurrence if exact.
replace(p, '''                                onSplitStereo = onSplitStereo,
''', '''                                onSplitStereo = onSplitStereo,
                                onOpenFades = onOpenFades,
                                onCrossfade = onCrossfade,
''')
# TrackLane signature expected onSplitStereo nearby.
replace(p, '''    onSplitStereo: (String) -> Unit,
    onTrackDragStart:''', '''    onSplitStereo: (String) -> Unit,
    onOpenFades: (String) -> Unit,
    onCrossfade: (String) -> Unit,
    onTrackDragStart:''')
# Menu actions after stereo split.
replace(p, '''                                        if (clip.sourceChannelCount == 2) {
                                            ClipMenuItem(Icons.Default.CallSplit, "Separar estéreo em 2 pistas mono$suffix") { clipMenuExpanded = false; onSplitStereo(clip.id) }
                                        }
                                        ClipMenuItem(Icons.Default.ContentCut, "Cortar$suffix")''', '''                                        if (clip.sourceChannelCount == 2) {
                                            ClipMenuItem(Icons.Default.CallSplit, "Separar estéreo em 2 pistas mono$suffix") { clipMenuExpanded = false; onSplitStereo(clip.id) }
                                        }
                                        ClipMenuItem(Icons.Default.Edit, "Fades…$suffix") { clipMenuExpanded = false; onOpenFades(clip.id) }
                                        ClipMenuItem(Icons.Default.CallSplit, "Crossfade com próximo$suffix") { clipMenuExpanded = false; onCrossfade(clip.id) }
                                        ClipMenuItem(Icons.Default.ContentCut, "Cortar$suffix")''')
# Add Fade dialog composable near end before a known function.
write_text=read(p)
fade_dialog=r'''
@Composable
private fun ClipFadeDialog(
    clip: AudioClip,
    sampleRateHz: Int,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
) {
    fun framesToMs(frames: Long): Float = frames * 1000f / sampleRateHz.coerceAtLeast(1)
    fun msToFrames(ms: Float): Long = (ms * sampleRateHz / 1000f).toLong().coerceAtLeast(0L)
    val maxMs = (clip.lengthFrames * 1000f / sampleRateHz.coerceAtLeast(1)).coerceAtMost(5000f).coerceAtLeast(10f)
    var fadeInMs by remember(clip.id) { mutableStateOf(framesToMs(clip.fadeInFrames).coerceIn(0f, maxMs)) }
    var fadeOutMs by remember(clip.id) { mutableStateOf(framesToMs(clip.fadeOutFrames).coerceIn(0f, maxMs)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fades do clipe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(clip.name, style = MaterialTheme.typography.bodyMedium)
                Text("Fade in · ${fadeInMs.toInt()} ms")
                androidx.compose.material3.Slider(value = fadeInMs, onValueChange = { fadeInMs = it }, valueRange = 0f..maxMs)
                Text("Fade out · ${fadeOutMs.toInt()} ms")
                androidx.compose.material3.Slider(value = fadeOutMs, onValueChange = { fadeOutMs = it }, valueRange = 0f..maxMs)
                Text("A edição é não destrutiva e usa a mesma curva na reprodução e na exportação.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onApply(msToFrames(fadeInMs), msToFrames(fadeOutMs)) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

'''
# insert before roleName if found, else append
marker='private fun roleName('
if marker in write_text:
    write(p, write_text.replace(marker, fade_dialog + marker, 1))
else:
    write(p, write_text + '\n' + fade_dialog)

# ---- Version ----
replace('app/build.gradle.kts', 'versionCode = 16\n        versionName = "0.3.0-alpha1"', 'versionCode = 17\n        versionName = "0.4.0-alpha1"')

# ---- Docs: M6 closure + M7 exact scope ----
write('docs/IMPLEMENTATION_ROADMAP.md', '''# Implementation Roadmap

Updated: 2026-09-09

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 Android 16/API36 + Pocket Amp USB physical gate established.

## M3 — Codec/import foundation — ABSORBED
Codec/import work was consolidated into M5.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after user physical approval of `0.2.0-alpha14`.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit user physical approval of `0.3.0-alpha1`. Loopback calibration, route-scoped compensation, clocks, live Mute/Solo and project rename are no longer pending.

## M7 — Production audio polish — ACTIVE PHYSICAL GATE
### Implemented for alpha1
- validated offline band-limited sample-rate conversion for mismatched imported source/project rates;
- immutable original retained while converted audio is stored only as managed editing proxy;
- additive editing-rate metadata for backwards-compatible project persistence;
- non-destructive clip fade-in/fade-out controls;
- crossfade over overlapping clips on the same track;
- identical fade envelope in realtime playback and offline master rendering;
- bounded-memory resampling and reusable render scratch buffers for larger-session stability;
- Home project menu: Rename plus the same shared `Salvar e exportar` modal used in Studio;
- Home export covers `.guitarlab`, WAV 32-bit float, FLAC and MP3 320 kbps through the established managed-media/render pipeline.

### M7 closure gates
1. exact-candidate unit tests, Android Lint, debug build and signed release;
2. import mismatched 44.1/48 kHz representatives and verify duration/pitch/playback/export;
3. verify fades and overlapping crossfade audibly and in exported master;
4. stress a larger multi-track session for responsiveness/memory regressions;
5. verify Home rename and Home `Salvar e exportar` round trip;
6. regression of M5/M6 audio, recording, latency compensation and project persistence;
7. zero repeatable P0/P1 plus explicit user M7 PASS/CLOSED.

## M8 — Release hardening
- migration/compatibility matrix;
- accessibility/device-size polish;
- crash/edge-case hardening;
- packaging, release notes and distribution gate.

## Gate discipline
Each milestone advances only after software and required physical gates pass. `CURRENT_STATE.md` and this roadmap are canonical.
''')
write('docs/CURRENT_STATE.md', '''# Current State — GuitarLab Studio

Updated: 2026-09-09

## Active branch and gate
- Repository: `anfalcir/guitarlab`
- Branch: `dev/parallel-m3-m5` (historical branch name retained while PR #1 stays active)
- Draft PR: #1
- M2: PASS/CLOSED
- M3/M4: absorbed
- M5: PASS/CLOSED
- M6: **PASS/CLOSED by explicit user physical approval of 0.3.0-alpha1**
- M7: implementation candidate `0.4.0-alpha1`, versionCode 17; **OPEN pending physical homologation**

## M7 production audio polish
M7 closes the previously documented production-audio-polish block: mismatched sample-rate conversion, non-destructive fades/crossfades, render parity and larger-session memory discipline. It also incorporates the approved workflow polish of Rename + shared `Salvar e exportar` directly from each project row on Home.

### Sample-rate contract
The imported native source remains immutable. When the source rate differs from the project/editing rate, GuitarLab creates a project-managed 32-bit-float WAV proxy using bounded-memory windowed-sinc conversion. Timeline/sourceStart/length editing then operates in the editing-proxy frame domain, while native-source rate/format metadata remains provenance.

### Fade/crossfade contract
Fade in/out are clip metadata and do not rewrite source audio. Realtime playback and offline master rendering use the same deterministic envelope. Crossfade requires actual overlap between two clips on the same track and maps the overlap to left fade-out + right fade-in.

### Performance contract
The resampler is chunk-bounded and playback/master ClipReaders reuse scratch arrays rather than allocating a new decode buffer on every render chunk. M7 physical stress remains required before claiming large-session verification.

### Home project actions
The three-dot project menu now offers Rename, Salvar e exportar, Duplicate and Delete. Home invokes the same `RenameProjectDialog` and `SaveAndExportDialog` components as Studio. Project persistence and master export use the same managed project/media contracts.

Canonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.
Active checklist: `docs/M7_ALPHA1_HOMOLOGATION_CHECKLIST.md`.
''')
write('docs/M7_ALPHA1_HOMOLOGATION_CHECKLIST.md', '''# M7 alpha1 — Physical Homologation Checklist

Target: Samsung SM-X230 Android 16/API36 + normal GuitarLab/Pocket Amp workflow.
Candidate: `0.4.0-alpha1` / versionCode 17.

## A. Home project actions
- Three-dot menu shows Renomear, Salvar e exportar, Duplicar, Excluir.
- Rename persists after reopen/app restart and is reflected in Studio/export filenames.
- Salvar e exportar opens the same visual/semantic modal as Studio.
- From Home, save `.guitarlab`, export WAV32f, FLAC, MP3; verify nonempty/playable outputs and reopen the project package.

## B. Sample-rate conversion
- Import 44.1 kHz into a 48 kHz project and 48 kHz into a 44.1 kHz project where practical.
- Import must complete with processing feedback, then playback at correct pitch and duration.
- Verify imported provenance still reports native source rate and editing path reports converted rate.
- Export master and confirm duration/pitch remain correct.
- Regression: same-rate import remains passthrough/no unnecessary conversion.

## C. Fades/crossfades
- Open clip menu > Fades, apply audible fade-in and fade-out; reopen and confirm persistence.
- Export master and confirm fades match realtime playback.
- Place two clips on same track with overlap and apply Crossfade with next.
- Verify smooth transition and Undo/Redo behavior.
- Attempt crossfade without overlap: app should reject safely, not corrupt clips.

## D. Larger-session/performance
- Use a representative larger session with multiple tracks/clips.
- Play, seek, loop, open mixer, manipulate Mute/Solo and edit fades.
- No repeatable ANR/crash or progressive memory/performance collapse.

## E. Regression
- recording + M6 calibrated take placement;
- drag/trim/split/stereo separation;
- `.guitarlab` save/open;
- WAV/FLAC/MP3 export;
- project rename and live Mute/Solo.

M7 closes only with zero repeatable P0/P1 and explicit user approval.
''')
