package com.shjamolov.mediastreamplayer.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "favorite_media",
    primaryKeys = ["tmdbId", "mediaType"],
)
data class FavoriteMediaEntity(
    val tmdbId: Long,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val addedAtEpochMillis: Long,
)
