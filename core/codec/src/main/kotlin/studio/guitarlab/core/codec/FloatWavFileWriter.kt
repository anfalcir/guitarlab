package studio.guitarlab.core.codec

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Streaming 32-bit IEEE-float WAV writer used by project-managed recording takes. */
class FloatWavFileWriter(
    private val file: File,
    val sampleRateHz: Int,
    val channelCount: Int,
) : AutoCloseable {
    private val output = RandomAccessFile(file, "rw")
    private var finished = false
    var totalFrames: Long = 0
        private set

    init {
        require(sampleRateHz in 8_000..384_000) { "sampleRateHz out of range" }
        require(channelCount in 1..2) { "Recording writer supports mono or stereo" }
        file.parentFile?.mkdirs()
        output.setLength(0)
        writeHeader(dataBytes = 0)
    }

    fun writeInterleaved(samples: FloatArray, frameCount: Int, sourceFrameOffset: Int = 0) {
        check(!finished) { "WAV writer is already finished" }
        require(frameCount >= 0)
        require(sourceFrameOffset >= 0)
        val sampleOffset = sourceFrameOffset * channelCount
        val sampleCount = frameCount * channelCount
        require(sampleOffset + sampleCount <= samples.size) { "Source buffer is too small" }
        if (sampleCount == 0) return

        val buffer = ByteBuffer.allocate(sampleCount * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        repeat(sampleCount) { index -> buffer.putFloat(samples[sampleOffset + index]) }
        output.write(buffer.array())
        totalFrames += frameCount
    }

    fun finish() {
        if (finished) return
        val dataBytes = Math.multiplyExact(totalFrames, channelCount.toLong() * Float.SIZE_BYTES)
        require(dataBytes <= 0xFFFF_FFFFL - 36L) { "WAV take exceeds RIFF 32-bit size limit" }
        output.seek(0)
        writeHeader(dataBytes)
        output.fd.sync()
        finished = true
    }

    override fun close() {
        try {
            finish()
        } finally {
            output.close()
        }
    }

    private fun writeHeader(dataBytes: Long) {
        output.writeBytes("RIFF")
        writeLeInt((36L + dataBytes).toInt())
        output.writeBytes("WAVE")
        output.writeBytes("fmt ")
        writeLeInt(16)
        writeLeShort(WAVE_FORMAT_IEEE_FLOAT)
        writeLeShort(channelCount)
        writeLeInt(sampleRateHz)
        val blockAlign = channelCount * Float.SIZE_BYTES
        writeLeInt(sampleRateHz * blockAlign)
        writeLeShort(blockAlign)
        writeLeShort(32)
        output.writeBytes("data")
        writeLeInt(dataBytes.toInt())
    }

    private fun writeLeShort(value: Int) {
        output.write(value and 0xFF)
        output.write((value ushr 8) and 0xFF)
    }

    private fun writeLeInt(value: Int) {
        output.write(value and 0xFF)
        output.write((value ushr 8) and 0xFF)
        output.write((value ushr 16) and 0xFF)
        output.write((value ushr 24) and 0xFF)
    }

    private companion object {
        const val WAVE_FORMAT_IEEE_FLOAT = 0x0003
    }
}
