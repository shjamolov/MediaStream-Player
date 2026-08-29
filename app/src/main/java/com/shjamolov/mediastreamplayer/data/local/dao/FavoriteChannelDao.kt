package com.shjamolov.mediastreamplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteChannelDao {
    @Query("SELECT * FROM favorite_channels ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteChannelEntity>>

    @Upsert
    suspend fun upsert(channel: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE channelId = :channelId)")
    fun observeIsFavorite(channelId: String): Flow<Boolean>
}
