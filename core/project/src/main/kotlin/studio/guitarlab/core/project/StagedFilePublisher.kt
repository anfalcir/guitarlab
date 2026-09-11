package studio.guitarlab.core.project

import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Publishes a fully staged file to an externally owned destination.
 *
 * The destination is opened only after the source is complete. If publication fails or
 * the coroutine is cancelled after the destination was opened, [resetDestination] is
 * invoked best-effort so callers do not silently leave a plausible-looking partial file.
 */
object StagedFilePublisher {
    suspend fun publish(
        source: File,
        openDestination: () -> OutputStream,
        resetDestination: () -> Unit,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
    ) {
        require(source.isFile) { "Staged source file is missing" }
        require(source.length() > 0L) { "Staged source file is empty" }
        require(bufferSize > 0) { "bufferSize must be positive" }
        currentCoroutineContext().ensureActive()

        var destinationOpened = false
        try {
            openDestination().use { target ->
                destinationOpened = true
                source.inputStream().buffered(bufferSize).use { input ->
                    val buffer = ByteArray(bufferSize)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read > 0) target.write(buffer, 0, read)
                    }
                }
                currentCoroutineContext().ensureActive()
                target.flush()
            }
        } catch (error: Throwable) {
            if (destinationOpened) {
                runCatching(resetDestination).onFailure(error::addSuppressed)
            }
            throw error
        }
    }

    private const val DEFAULT_BUFFER_SIZE = 64 * 1024
}
