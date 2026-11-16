package jp.ikanoshiokara.pixelpilot.android

import android.app.Application
import jp.ikanoshiokara.pixelpilot.android.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PixelPilotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PixelPilotApplication)
            modules(appModules)
        }
    }
}
