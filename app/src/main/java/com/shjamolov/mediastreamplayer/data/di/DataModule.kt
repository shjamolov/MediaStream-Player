package com.shjamolov.mediastreamplayer.data.di

import androidx.room.Room
import com.shjamolov.mediastreamplayer.data.local.AppDatabase
import com.shjamolov.mediastreamplayer.data.local.migration.MIGRATION_1_2
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = "media-stream-player.db",
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    single { get<AppDatabase>().favoriteMediaDao() }
    single { get<AppDatabase>().favoriteChannelDao() }
    single { get<AppDatabase>().playbackHistoryDao() }
}
