package com.shjamolov.mediastreamplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shjamolov.mediastreamplayer.data.local.entity.CachedCatalogItemEntity

@Dao
interface CatalogCacheDao {
    @Query("SELECT * FROM catalog_cache WHERE mediaType = :mediaType ORDER BY voteAverage DESC")
    suspend fun getByType(mediaType: String): List<CachedCatalogItemEntity>

    @Upsert
    suspend fun upsertAll(items: List<CachedCatalogItemEntity>)
}
