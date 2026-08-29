package com.shjamolov.mediastreamplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shjamolov.mediastreamplayer.data.local.dao.FavoriteChannelDao
import com.shjamolov.mediastreamplayer.data.local.dao.FavoriteMediaDao
import com.shjamolov.mediastreamplayer.data.local.dao.PlaybackHistoryDao
import com.shjamolov.mediastreamplayer.data.local.dao.CatalogCacheDao
import com.shjamolov.mediastreamplayer.data.local.entity.CachedCatalogItemEntity
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteChannelEntity
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteMediaEntity
import com.shjamolov.mediastreamplayer.data.local.entity.PlaybackHistoryEntity

@Database(
    entities = [
        FavoriteMediaEntity::class,
        FavoriteChannelEntity::class,
        PlaybackHistoryEntity::class,
        CachedCatalogItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteMediaDao(): FavoriteMediaDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun catalogCacheDao(): CatalogCacheDao
}
