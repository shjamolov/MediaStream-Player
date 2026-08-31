package com.shjamolov.mediastreamplayer.domain.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.domain.model.TmdbId
import com.shjamolov.mediastreamplayer.domain.model.CatalogEpisode
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    suspend fun popular(type: MediaType): AppResult<CatalogPage>
    suspend fun discover(type: MediaType, genreId: Int): AppResult<CatalogPage>
    suspend fun search(query: String): AppResult<CatalogPage>
    suspend fun details(id: TmdbId, type: MediaType): AppResult<CatalogDetails>
    suspend fun seasonEpisodes(seriesId: TmdbId, seasonNumber: Int): AppResult<List<CatalogEpisode>>
    fun observeFavorites(): Flow<List<CatalogItem>>
    fun observeIsFavorite(id: TmdbId, type: MediaType): Flow<Boolean>
    suspend fun setFavorite(item: CatalogItem, favorite: Boolean)
}

data class CatalogPage(val items: List<CatalogItem>, val fromCache: Boolean)
