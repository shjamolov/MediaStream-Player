package com.shjamolov.mediastreamplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shjamolov.mediastreamplayer.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY updatedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE contentKey = :contentKey")
    suspend fun get(contentKey: String): PlaybackHistoryEntity?

    @Upsert
    suspend fun upsert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE contentKey = :contentKey")
    suspend fun delete(contentKey: String)

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}
