package com.shjamolov.mediastreamplayer.data.di

import androidx.room.Room
import com.shjamolov.mediastreamplayer.data.local.AppDatabase
import com.shjamolov.mediastreamplayer.data.local.migration.MIGRATION_1_2
import com.shjamolov.mediastreamplayer.data.local.migration.MIGRATION_2_3
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.shjamolov.mediastreamplayer.data.repository.DefaultTorrServerRepository
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository

val dataModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = "media-stream-player.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    single { get<AppDatabase>().favoriteMediaDao() }
    single { get<AppDatabase>().favoriteChannelDao() }
    single { get<AppDatabase>().playbackHistoryDao() }
    single { get<AppDatabase>().catalogCacheDao() }
    single<TorrServerRepository> { DefaultTorrServerRepository(get()) }
}
