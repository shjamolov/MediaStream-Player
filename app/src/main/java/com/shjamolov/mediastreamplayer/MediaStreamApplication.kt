package com.shjamolov.mediastreamplayer

import android.app.Application
import com.shjamolov.mediastreamplayer.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MediaStreamApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MediaStreamApplication)
            modules(appModules)
        }
    }
}
