package com.shjamolov.mediastreamplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val channelId: String,
    val name: String,
    val logoUrl: String?,
    val addedAtEpochMillis: Long,
)
