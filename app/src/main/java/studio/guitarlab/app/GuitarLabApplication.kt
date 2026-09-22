package studio.guitarlab.app

import android.app.Application
import studio.guitarlab.app.io.AppCacheTemporaryCleaner
import studio.guitarlab.app.backup.BackupScheduler
import studio.guitarlab.platform.separation.RemoteSeparationClient

class GuitarLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCacheTemporaryCleaner.clean(cacheDir)
        BackupScheduler.sync(this)
        RemoteSeparationClient(this).resumePending()
    }
}
