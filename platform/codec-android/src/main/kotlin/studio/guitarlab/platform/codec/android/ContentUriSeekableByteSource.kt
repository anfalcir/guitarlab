package studio.guitarlab.platform.codec.android

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import studio.guitarlab.core.codec.AudioCodecException
import studio.guitarlab.core.codec.SeekableByteSource
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Random-access bridge for SAF/content:// audio files. The descriptor remains open
 * for the lifetime of this source and is never retained by core codec code.
 */
class ContentUriSeekableByteSource(
    resolver: ContentResolver,
    uri: Uri,
) : SeekableByteSource, AutoCloseable {
    private val descriptor: AssetFileDescriptor = resolver.openAssetFileDescriptor(uri, "r")
        ?: throw AudioCodecException("Android could not open the selected audio document")
    private val input = FileInputStream(descriptor.fileDescriptor)
    private val channel: FileChannel = input.channel
    private val baseOffset = descriptor.startOffset.coerceAtLeast(0L)

    override val sizeBytes: Long = when {
        descriptor.declaredLength >= 0L -> descriptor.declaredLength
        else -> (channel.size() - baseOffset).coerceAtLeast(0L)
    }

    @Synchronized
    override fun readAt(position: Long, destination: ByteArray, offset: Int, length: Int): Int {
        require(position >= 0) { "position must be non-negative" }
        require(offset >= 0 && length >= 0 && offset + length <= destination.size) {
            "destination range is out of bounds"
        }
        if (length == 0 || position >= sizeBytes) return 0

        val count = minOf(length.toLong(), sizeBytes - position).toInt()
        channel.position(baseOffset + position)
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
        runCatching { input.close() }
        runCatching { descriptor.close() }
    }
}
