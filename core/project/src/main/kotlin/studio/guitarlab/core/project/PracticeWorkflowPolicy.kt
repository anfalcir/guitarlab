package studio.guitarlab.core.project

import java.util.UUID
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import studio.guitarlab.core.model.*

enum class GuitarAuditionMode { MIXER, REFERENCE, MY_GUITAR, BOTH }
enum class GuitarAuditionTrackState { UNAFFECTED, INCLUDED, EXCLUDED }

object GuitarAuditionPolicy {
    private val referenceRoles = setOf(BuiltInRoles.REFERENCE_GUITAR, BuiltInRoles.REFERENCE_GUITAR_L, BuiltInRoles.REFERENCE_GUITAR_R)
    private val myRoles = setOf(BuiltInRoles.RECORDED_GUITAR, BuiltInRoles.RECORDED_GUITAR_L, BuiltInRoles.RECORDED_GUITAR_R)
    fun roleAudible(roleId: String?, mode: GuitarAuditionMode): Boolean = when (mode) {
        GuitarAuditionMode.MIXER, GuitarAuditionMode.BOTH -> true
        GuitarAuditionMode.REFERENCE -> roleId !in myRoles
        GuitarAuditionMode.MY_GUITAR -> roleId !in referenceRoles
    }
    fun trackState(roleId: String?, mode: GuitarAuditionMode): GuitarAuditionTrackState {
        val controlled = roleId in referenceRoles || roleId in myRoles
        if (!controlled || mode == GuitarAuditionMode.MIXER) return GuitarAuditionTrackState.UNAFFECTED
        return if (roleAudible(roleId, mode)) GuitarAuditionTrackState.INCLUDED else GuitarAuditionTrackState.EXCLUDED
    }
}

object ActiveTakePolicy {
    fun audibleClips(project: GuitarProject): List<AudioClip> {
        val active = project.takes.asSequence().filter { it.active }.map { it.id }.toSet()
        return project.clips.filter { it.takeId == null || it.takeId in active }
    }
    fun activate(project: GuitarProject, takeId: String, nowEpochMs: Long): GuitarProject {
        val selected = project.takes.firstOrNull { it.id == takeId } ?: error("Take não encontrado: $takeId")
        return project.copy(takes = project.takes.map { if (it.trackId == selected.trackId) it.copy(active = it.id == takeId) else it }, updatedAtEpochMs = nowEpochMs)
    }
}

data class LevelAnalysis(val peakDbfs: Float, val rmsDbfs: Float, val recommendedGainDb: Float, val clipped: Boolean, val silent: Boolean)

class TrackLevelAccumulator {
    private var peak = 0f
    private var squares = 0.0
    private var samples = 0L
    fun append(interleaved: FloatArray, sampleCount: Int = interleaved.size) {
        require(sampleCount in 0..interleaved.size)
        repeat(sampleCount) { i -> val value = interleaved[i]; peak = max(peak, abs(value)); squares += value.toDouble() * value }
        samples += sampleCount
    }
    fun finish(): LevelAnalysis = TrackLevelAdvisor.analyze(peak, if (samples == 0L) 0f else sqrt(squares / samples).toFloat())
}

object TrackLevelAdvisor {
    private const val TARGET_RMS_DBFS = -18f
    private const val MAX_PEAK_DBFS = -3f
    fun analyze(samples: FloatArray): LevelAnalysis = TrackLevelAccumulator().also { it.append(samples) }.finish()
    internal fun analyze(peak: Float, rms: Float): LevelAnalysis {
        val peakDb = amplitudeDb(peak); val rmsDb = amplitudeDb(rms); val silent = rms < 0.00001f
        val gain = if (silent) 0f else min(TARGET_RMS_DBFS - rmsDb, MAX_PEAK_DBFS - peakDb).coerceIn(-12f, 12f)
        return LevelAnalysis(peakDb, rmsDb, gain, peak > 1f, silent)
    }
    private fun amplitudeDb(value: Float) = if (value <= 0f) -120f else (20.0 * log10(value.toDouble())).toFloat().coerceAtLeast(-120f)
}

data class SectionBoundarySuggestion(val frame: Long, val confidence: Float)

data class SectionSuggestionPreview(
    val name: String,
    val startFrame: Long,
    val endFrame: Long,
    val confidence: Float?,
)

object SectionBoundaryAnalyzer {
    fun suggest(peaks: List<Float>, totalFrames: Long, maximum: Int = 12): List<SectionBoundarySuggestion> {
        if (peaks.size < 12 || totalFrames <= 1L || maximum <= 0) return emptyList()
        val radius = max(2, peaks.size / 80); val spacing = max(4, peaks.size / (maximum * 2 + 1))
        val candidates = ArrayList<Pair<Int, Float>>()
        for (index in radius until peaks.size - radius) {
            val before = peaks.subList(index - radius, index).average().toFloat(); val after = peaks.subList(index, index + radius).average().toFloat()
            val contrast = abs(after - before) / max(0.05f, max(before, after))
            if (contrast >= 0.35f) candidates += index to contrast.coerceIn(0f, 1f)
        }
        val selected = mutableListOf<Pair<Int, Float>>()
        candidates.sortedByDescending { it.second }.forEach { c -> if (selected.size < maximum && selected.none { abs(it.first - c.first) < spacing }) selected += c }
        return selected.sortedBy { it.first }.map { (i, confidence) -> SectionBoundarySuggestion((i.toDouble() / peaks.lastIndex * totalFrames).toLong().coerceIn(1L, totalFrames - 1L), confidence) }
    }
}

data class PunchCapturePlan(val captureStartFrame: Long, val automaticStopAfterFrames: Long, val keptTimelineStartFrame: Long, val keptSourceStartFrame: Long, val keptLengthFrames: Long)

object PunchRecordingPolicy {
    fun plan(region: PunchRegion, latencyFrames: Long = 0L): PunchCapturePlan {
        require(region.startFrame >= 0L && region.endFrame > region.startFrame && region.preRollFrames >= 0L && region.postRollFrames >= 0L && latencyFrames >= 0L)
        val captureStart = (region.startFrame - region.preRollFrames).coerceAtLeast(0L); val effectivePre = region.startFrame - captureStart
        return PunchCapturePlan(captureStart, effectivePre + region.endFrame - region.startFrame + region.postRollFrames + latencyFrames, region.startFrame, effectivePre + latencyFrames, region.endFrame - region.startFrame)
    }
}

enum class PracticeRecordingMode { CURRENT_PLAYHEAD, FROM_PROJECT_START, LOOP_PUNCH }

data class PracticeRecordingStartPlan(
    val mode: PracticeRecordingMode,
    val sessionStartFrame: Long,
    val loopEnabled: Boolean,
    val punchRegion: PunchRegion? = null,
)

/**
 * Resolves the user's recording intent at REC time. Punch is deliberately transient: the
 * persisted GuitarProject.punchRegion field remains readable for backward compatibility but is
 * not consulted by this policy. That prevents an old project setting from silently changing a
 * future recording.
 */
object PracticeRecordingStartPolicy {
    fun plan(
        mode: PracticeRecordingMode,
        currentPlayheadFrame: Long,
        loopEnabled: Boolean,
        loopStartFrame: Long,
        loopEndFrame: Long,
        sampleRateHz: Int,
    ): PracticeRecordingStartPlan = when (mode) {
        PracticeRecordingMode.CURRENT_PLAYHEAD -> PracticeRecordingStartPlan(
            mode = mode,
            sessionStartFrame = currentPlayheadFrame.coerceAtLeast(0L),
            loopEnabled = false,
        )
        PracticeRecordingMode.FROM_PROJECT_START -> PracticeRecordingStartPlan(
            mode = mode,
            sessionStartFrame = 0L,
            loopEnabled = false,
        )
        PracticeRecordingMode.LOOP_PUNCH -> {
            require(loopEnabled) { "Ative o loop para gravar somente o trecho selecionado." }
            require(loopStartFrame >= 0L && loopEndFrame > loopStartFrame) { "O intervalo do loop é inválido." }
            require(sampleRateHz > 0) { "A taxa de amostragem precisa ser conhecida para o punch." }
            val region = PunchRegion(
                startFrame = loopStartFrame,
                endFrame = loopEndFrame,
                preRollFrames = sampleRateHz.toLong() * 3L,
                postRollFrames = sampleRateHz.toLong(),
            )
            PracticeRecordingStartPlan(
                mode = mode,
                sessionStartFrame = PunchRecordingPolicy.plan(region).captureStartFrame,
                loopEnabled = true,
                punchRegion = region,
            )
        }
    }
}

object PracticeWorkflowEditor {
    fun addMarker(project: GuitarProject, name: String, frame: Long, now: Long, id: String = UUID.randomUUID().toString()): GuitarProject {
        require(name.trim().isNotEmpty() && frame >= 0L); return project.copy(markers = (project.markers + TimelineMarker(id, name.trim(), frame)).sortedBy { it.frame }, updatedAtEpochMs = now)
    }
    fun removeMarker(project: GuitarProject, id: String, now: Long) = project.copy(markers = project.markers.filterNot { it.id == id }, updatedAtEpochMs = now)
    fun addSection(project: GuitarProject, name: String, start: Long, end: Long, now: Long, id: String = UUID.randomUUID().toString()): GuitarProject {
        require(name.trim().isNotEmpty() && start >= 0L && end > start); return project.copy(sections = (project.sections + TimelineSection(id, name.trim(), start, end)).sortedBy { it.startFrame }, updatedAtEpochMs = now)
    }
    fun previewSuggestedSections(suggestions: List<SectionBoundarySuggestion>, totalFrames: Long): List<SectionSuggestionPreview> {
        require(totalFrames > 0L)
        val normalizedSuggestions = suggestions
            .filter { it.frame in 1 until totalFrames }
            .distinctBy { it.frame }
            .sortedBy { it.frame }
        val boundaries = (listOf(0L) + normalizedSuggestions.map { it.frame } + totalFrames).distinct().sorted()
        return boundaries.zipWithNext().mapIndexedNotNull { index, (start, end) ->
            if (end <= start) null else SectionSuggestionPreview(
                name = "Seção ${index + 1}",
                startFrame = start,
                endFrame = end,
                confidence = normalizedSuggestions.firstOrNull { it.frame == start }?.confidence,
            )
        }
    }
    fun acceptSuggestedSections(project: GuitarProject, suggestions: List<SectionBoundarySuggestion>, totalFrames: Long, now: Long): GuitarProject {
        val sections = previewSuggestedSections(suggestions, totalFrames).map { preview ->
            TimelineSection(
                id = UUID.randomUUID().toString(),
                name = preview.name,
                startFrame = preview.startFrame,
                endFrame = preview.endFrame,
                origin = SectionOrigin.AUTOMATIC,
                confidence = preview.confidence,
            )
        }
        return project.copy(sections = sections, updatedAtEpochMs = now)
    }
    fun removeSection(project: GuitarProject, id: String, now: Long) = project.copy(sections = project.sections.filterNot { it.id == id }, updatedAtEpochMs = now)
    fun clearSections(project: GuitarProject, now: Long) = project.copy(sections = emptyList(), updatedAtEpochMs = now)

    /** Legacy project field kept only for file-format compatibility. New UI/runtime punch is transient. */
    @Deprecated("Punch is selected transiently at REC time; do not arm it in the project")
    fun setPunch(project: GuitarProject, region: PunchRegion?, now: Long) = project.copy(punchRegion = region, updatedAtEpochMs = now)
}
