package com.shjamolov.mediastreamplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["updatedAtEpochMillis"])],
)
data class PlaybackHistoryEntity(
    @PrimaryKey val contentKey: String,
    val contentType: String,
    val tmdbId: Long?,
    val channelId: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String,
    val posterPath: String?,
    val positionMillis: Long,
    val durationMillis: Long?,
    val updatedAtEpochMillis: Long,
)
