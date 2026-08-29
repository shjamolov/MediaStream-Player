package com.shjamolov.mediastreamplayer

import android.app.Application
import androidx.room.Room
import com.shjamolov.mediastreamplayer.data.local.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MediaStreamApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MediaStreamApplication)
            modules(
                module {
                    single {
                        Room.databaseBuilder(
                            context = get(),
                            klass = AppDatabase::class.java,
                            name = "media-stream-player.db",
                        ).build()
                    }
                },
            )
        }
    }
}

