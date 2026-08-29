package com.shjamolov.mediastreamplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shjamolov.mediastreamplayer.data.local.dao.FavoriteChannelDao
import com.shjamolov.mediastreamplayer.data.local.dao.FavoriteMediaDao
import com.shjamolov.mediastreamplayer.data.local.dao.PlaybackHistoryDao
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteChannelEntity
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteMediaEntity
import com.shjamolov.mediastreamplayer.data.local.entity.PlaybackHistoryEntity

@Database(
    entities = [
        FavoriteMediaEntity::class,
        FavoriteChannelEntity::class,
        PlaybackHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteMediaDao(): FavoriteMediaDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
}
