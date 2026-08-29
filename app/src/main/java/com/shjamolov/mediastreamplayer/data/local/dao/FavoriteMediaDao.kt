package com.shjamolov.mediastreamplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMediaDao {
    @Query("SELECT * FROM favorite_media ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteMediaEntity>>

    @Upsert
    suspend fun upsert(item: FavoriteMediaEntity)

    @Query("DELETE FROM favorite_media WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun delete(tmdbId: Long, mediaType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_media WHERE tmdbId = :tmdbId AND mediaType = :mediaType)")
    fun observeIsFavorite(tmdbId: Long, mediaType: String): Flow<Boolean>
}
