package studio.guitarlab.platform.codec.android

import android.content.Context
import android.net.Uri
import studio.guitarlab.core.codec.AudioCodecException
import studio.guitarlab.core.codec.SeekableByteSource
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

interface AndroidAudioDocumentSource : SeekableByteSource, AutoCloseable {
    val accessMode: String
}

object AndroidAudioDocumentSourceFactory {
    fun open(context: Context, uri: Uri): AndroidAudioDocumentSource {
        val direct = runCatching {
            ContentUriSeekableByteSource(context.contentResolver, uri).also { source ->
                if (source.sizeBytes <= 0L) throw AudioCodecException("Selected audio document reports no readable bytes")
                val probe = ByteArray(1)
                if (source.readAt(0, probe) != 1) throw AudioCodecException("Selected document did not allow initial random read")
                if (source.sizeBytes > 1L && source.readAt(source.sizeBytes - 1L, probe) != 1) {
                    throw AudioCodecException("Selected document did not allow end-of-file random read")
                }
            }
        }.getOrNull()
        if (direct != null) return direct

        val tempFile = File.createTempFile("guitarlab-import-", ".audio", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: throw AudioCodecException("Android could not stream the selected audio document")
            if (tempFile.length() <= 0L) throw AudioCodecException("Selected audio document is empty")
            return TempFileAndroidAudioDocumentSource(tempFile)
        } catch (error: Throwable) {
            tempFile.delete()
            if (error is AudioCodecException) throw error
            throw AudioCodecException("Could not prepare selected audio document for random access", error)
        }
    }
}

private class TempFileAndroidAudioDocumentSource(
    private val file: File,
) : AndroidAudioDocumentSource {
    private val randomAccess = RandomAccessFile(file, "r")
    private val channel = randomAccess.channel

    override val accessMode: String = "CACHE_FALLBACK"
    override val sizeBytes: Long = file.length()

    @Synchronized
    override fun readAt(position: Long, destination: ByteArray, offset: Int, length: Int): Int {
        require(position >= 0) { "position must be non-negative" }
        require(offset >= 0 && length >= 0 && offset + length <= destination.size) {
            "destination range is out of bounds"
        }
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
        runCatching { file.delete() }
    }
}
