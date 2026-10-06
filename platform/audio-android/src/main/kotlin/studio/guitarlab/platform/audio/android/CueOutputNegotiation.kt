package studio.guitarlab.platform.audio.android

import kotlin.math.abs

enum class CueOutputStrategy {
    MULTI_DEVICE,
    COMMUNICATION_SPLIT,
}

data class CueOutputProfile(
    val sessionSampleRateHz: Int,
    val mainSampleRateHz: Int,
    val cueSampleRateHz: Int,
    val strategy: CueOutputStrategy = CueOutputStrategy.MULTI_DEVICE,
    val communicationModeRequired: Boolean = false,
) {
    val requiresCueResampling: Boolean get() = cueSampleRateHz != sessionSampleRateHz
}

/**
 * Keeps project/timeline rate authoritative and negotiates only the physical CUE sink.
 * A non-empty advertised rate list is treated as a strong hint: do not start with a rate
 * the endpoint does not advertise. Unknown/empty capabilities fall back to the session rate.
 */
internal object CueOutputNegotiationPolicy {
    fun candidateCueSampleRates(sessionSampleRateHz: Int, cueAdvertisedSampleRates: List<Int>): List<Int> {
        require(sessionSampleRateHz > 0)
        val advertised = cueAdvertisedSampleRates.filter { it > 0 }.distinct()
        if (advertised.isEmpty()) return listOf(sessionSampleRateHz)

        val candidates = ArrayList<Int>(advertised.size)
        if (sessionSampleRateHz in advertised) candidates += sessionSampleRateHz
        if (48_000 in advertised && 48_000 !in candidates) candidates += 48_000
        advertised
            .sortedWith(compareBy<Int>({ abs(it - sessionSampleRateHz) }, { it }))
            .forEach { if (it !in candidates) candidates += it }
        return candidates.take(MAX_CANDIDATES)
    }

    private const val MAX_CANDIDATES = 4
}

/**
 * Stateful stereo linear sample-rate adapter used only on the secondary CUE sink.
 * MAIN and the project timeline remain untouched. Output duration is derived from cumulative
 * source/output frame counters so chunk boundaries cannot accumulate rate error.
 */
internal class StereoLinearResampler(
    private val sourceSampleRateHz: Int,
    private val targetSampleRateHz: Int,
) {
    private var consumedSourceFrames = 0L
    private var nextOutputFrame = 0L
    private var previousLeft = 0f
    private var previousRight = 0f
    private var hasPrevious = false

    init {
        require(sourceSampleRateHz > 0)
        require(targetSampleRateHz > 0)
        require(sourceSampleRateHz != targetSampleRateHz)
    }

    fun reset() {
        consumedSourceFrames = 0L
        nextOutputFrame = 0L
        previousLeft = 0f
        previousRight = 0f
        hasPrevious = false
    }

    fun process(input: FloatArray, frameCount: Int): FloatArray {
        require(frameCount >= 0)
        require(input.size >= frameCount * CHANNELS)
        if (frameCount == 0) return FloatArray(0)

        val chunkStart = consumedSourceFrames
        val chunkEndExclusive = chunkStart + frameCount
        val lastInterpolablePositionExclusive = chunkEndExclusive - 1.0
        val estimatedFrames = ((frameCount.toLong() * targetSampleRateHz + sourceSampleRateHz - 1L) /
            sourceSampleRateHz + 4L).toInt()
        var output = FloatArray(estimatedFrames * CHANNELS)
        var outputFrames = 0

        while (true) {
            val sourcePosition = nextOutputFrame.toDouble() * sourceSampleRateHz.toDouble() /
                targetSampleRateHz.toDouble()
            if (sourcePosition >= lastInterpolablePositionExclusive) break

            val baseFrame = kotlin.math.floor(sourcePosition).toLong()
            val fraction = (sourcePosition - baseFrame).toFloat()
            val leftA: Float
            val rightA: Float
            val leftB: Float
            val rightB: Float

            if (baseFrame == chunkStart - 1L && hasPrevious) {
                leftA = previousLeft
                rightA = previousRight
                leftB = input[0]
                rightB = input[1]
            } else {
                val local = (baseFrame - chunkStart).toInt()
                if (local < 0 || local + 1 >= frameCount) break
                val a = local * CHANNELS
                val b = a + CHANNELS
                leftA = input[a]
                rightA = input[a + 1]
                leftB = input[b]
                rightB = input[b + 1]
            }

            if ((outputFrames + 1) * CHANNELS > output.size) {
                output = output.copyOf(output.size.coerceAtLeast(CHANNELS) * 2)
            }
            val out = outputFrames * CHANNELS
            output[out] = leftA + (leftB - leftA) * fraction
            output[out + 1] = rightA + (rightB - rightA) * fraction
            outputFrames += 1
            nextOutputFrame += 1
        }

        val last = (frameCount - 1) * CHANNELS
        previousLeft = input[last]
        previousRight = input[last + 1]
        hasPrevious = true
        consumedSourceFrames = chunkEndExclusive
        return output.copyOf(outputFrames * CHANNELS)
    }

    private companion object {
        const val CHANNELS = 2
    }
}
