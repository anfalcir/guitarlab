package studio.guitarlab.core.codec

class ByteArraySeekableSource(
    private val bytes: ByteArray,
) : SeekableByteSource {
    override val sizeBytes: Long
        get() = bytes.size.toLong()

    override fun readAt(position: Long, destination: ByteArray, offset: Int, length: Int): Int {
        require(position >= 0) { "position must be non-negative" }
        require(offset >= 0 && length >= 0 && offset + length <= destination.size) {
            "destination range is out of bounds"
        }
        if (length == 0 || position >= bytes.size) return 0
        val start = position.toInt()
        val count = minOf(length, bytes.size - start)
        bytes.copyInto(destination, destinationOffset = offset, startIndex = start, endIndex = start + count)
        return count
    }
}
