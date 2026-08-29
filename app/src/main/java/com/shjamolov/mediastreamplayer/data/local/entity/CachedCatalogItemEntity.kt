package com.shjamolov.mediastreamplayer.data.local.entity

import androidx.room.Entity

@Entity(tableName = "catalog_cache", primaryKeys = ["tmdbId", "mediaType"])
data class CachedCatalogItemEntity(
    val tmdbId: Long,
    val mediaType: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val updatedAtEpochMillis: Long,
)
