package studio.guitarlab.app.backup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BackupOperationLock {
    private val mutex = Mutex()
    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
    fun isBusy(): Boolean = mutex.isLocked
}
