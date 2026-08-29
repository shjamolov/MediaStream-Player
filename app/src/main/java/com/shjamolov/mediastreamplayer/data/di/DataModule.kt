package com.shjamolov.mediastreamplayer.data.di

import androidx.room.Room
import com.shjamolov.mediastreamplayer.data.local.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = "media-stream-player.db",
        ).build()
    }
}

