package studio.guitarlab.core.codec

import java.io.File

data class StereoWavSplitResult(
    val totalFrames: Long,
    val sampleRateHz: Int,
)

object StereoWavChannelSplitter {
    fun split(input: File, leftOutput: File, rightOutput: File): StereoWavSplitResult {
        FileSeekableByteSource(input).use { source ->
            val decoder = WavPcmDecoder(source)
            val metadata = decoder.metadata
            require(metadata.channelCount == 2) { "A separação L/R exige uma fonte estéreo de 2 canais." }
            require(metadata.totalFrames > 0L) { "A fonte estéreo não contém áudio." }
            val interleaved = FloatArray(BUFFER_FRAMES * 2)
            val left = FloatArray(BUFFER_FRAMES)
            val right = FloatArray(BUFFER_FRAMES)
            FloatWavFileWriter(leftOutput, metadata.sampleRateHz, 1).use { leftWriter ->
                FloatWavFileWriter(rightOutput, metadata.sampleRateHz, 1).use { rightWriter ->
                    decoder.seekToFrame(0L)
                    while (true) {
                        val frames = decoder.readInterleaved(interleaved, 0, BUFFER_FRAMES)
                        if (frames <= 0) break
                        repeat(frames) { frame ->
                            left[frame] = interleaved[frame * 2]
                            right[frame] = interleaved[frame * 2 + 1]
                        }
                        leftWriter.writeInterleaved(left, frames)
                        rightWriter.writeInterleaved(right, frames)
                    }
                }
            }
            return StereoWavSplitResult(metadata.totalFrames, metadata.sampleRateHz)
        }
    }

    private const val BUFFER_FRAMES = 4096
}
