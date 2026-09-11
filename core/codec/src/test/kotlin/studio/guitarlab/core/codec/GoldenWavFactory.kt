package studio.guitarlab.core.codec

import java.io.ByteArrayOutputStream

internal object GoldenWavFactory {
    fun pcm16(sampleRate: Int, channels: Int, frames: List<ShortArray>): ByteArray {
        require(frames.all { it.size == channels })
        val payload = ByteArrayOutputStream()
        frames.forEach { frame ->
            frame.forEach { sample ->
                val value = sample.toInt()
                payload.write(value and 0xFF)
                payload.write((value ushr 8) and 0xFF)
            }
        }
        return wav(formatTag = 1, sampleRate = sampleRate, channels = channels, bits = 16, payload = payload.toByteArray())
    }

    fun pcm24(sampleRate: Int, channels: Int, interleaved: IntArray): ByteArray {
        require(interleaved.size % channels == 0)
        val payload = ByteArrayOutputStream()
        interleaved.forEach { sample ->
            require(sample in -8_388_608..8_388_607)
            payload.write(sample and 0xFF)
            payload.write((sample ushr 8) and 0xFF)
            payload.write((sample ushr 16) and 0xFF)
        }
        return wav(formatTag = 1, sampleRate = sampleRate, channels = channels, bits = 24, payload = payload.toByteArray())
    }

    fun float32(sampleRate: Int, channels: Int, interleaved: FloatArray): ByteArray {
        require(interleaved.size % channels == 0)
        val payload = ByteArrayOutputStream()
        interleaved.forEach { sample ->
            val bits = sample.toRawBits()
            repeat(4) { shift -> payload.write((bits ushr (shift * 8)) and 0xFF) }
        }
        return wav(formatTag = 3, sampleRate = sampleRate, channels = channels, bits = 32, payload = payload.toByteArray())
    }

    private fun wav(formatTag: Int, sampleRate: Int, channels: Int, bits: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val blockAlign = channels * bits / 8
        val byteRate = sampleRate * blockAlign
        val riffSize = 4 + (8 + 16) + (8 + payload.size)
        out.ascii("RIFF")
        out.u32(riffSize)
        out.ascii("WAVE")
        out.ascii("fmt ")
        out.u32(16)
        out.u16(formatTag)
        out.u16(channels)
        out.u32(sampleRate)
        out.u32(byteRate)
        out.u16(blockAlign)
        out.u16(bits)
        out.ascii("data")
        out.u32(payload.size)
        out.write(payload)
        if (payload.size and 1 == 1) out.write(0)
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.ascii(value: String) = write(value.toByteArray(Charsets.US_ASCII))
    private fun ByteArrayOutputStream.u16(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }
    private fun ByteArrayOutputStream.u32(value: Int) {
        repeat(4) { shift -> write((value ushr (shift * 8)) and 0xFF) }
    }
}
