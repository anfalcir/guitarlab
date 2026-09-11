package studio.guitarlab.app

import android.app.Application
import studio.guitarlab.app.io.AppCacheTemporaryCleaner

class GuitarLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCacheTemporaryCleaner.clean(cacheDir)
    }
}
