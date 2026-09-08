package studio.guitarlab.core.codec

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class FileSeekableByteSource(file: File) : SeekableByteSource, Closeable {
    private val randomAccess = RandomAccessFile(file, "r")
    private val channel = randomAccess.channel

    override val sizeBytes: Long = file.length()

    @Synchronized
    override fun readAt(position: Long, destination: ByteArray, offset: Int, length: Int): Int {
        require(position >= 0L) { "position must be non-negative" }
        require(offset >= 0 && length >= 0 && offset + length <= destination.size) { "destination range is out of bounds" }
        if (length == 0 || position >= sizeBytes) return 0
        val count = minOf(length.toLong(), sizeBytes - position).toInt()
        channel.position(position)
        val buffer = ByteBuffer.wrap(destination, offset, count)
        var total = 0
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer)
            if (read <= 0) break
            total += read
        }
        return total
    }

    override fun close() {
        runCatching { channel.close() }
        runCatching { randomAccess.close() }
    }
}
