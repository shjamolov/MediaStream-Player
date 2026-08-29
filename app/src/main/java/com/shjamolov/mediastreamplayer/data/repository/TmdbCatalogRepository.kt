package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.data.local.dao.CatalogCacheDao
import com.shjamolov.mediastreamplayer.data.local.dao.FavoriteMediaDao
import com.shjamolov.mediastreamplayer.data.local.entity.CachedCatalogItemEntity
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteMediaEntity
import com.shjamolov.mediastreamplayer.data.remote.tmdb.TmdbApi
import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbDetailsDto
import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbMediaDto
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.CatalogSeason
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.domain.model.TmdbId
import com.shjamolov.mediastreamplayer.domain.repository.CatalogPage
import com.shjamolov.mediastreamplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class TmdbCatalogRepository(
    private val api: TmdbApi,
    private val cache: CatalogCacheDao,
    private val favorites: FavoriteMediaDao,
    private val token: String,
) : CatalogRepository {
    override suspend fun popular(type: MediaType): AppResult<CatalogPage> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val remote = when (type) {
                MediaType.MOVIE -> api.popularMovies().results.mapNotNull { it.toDomain(MediaType.MOVIE) }
                MediaType.SERIES -> api.popularSeries().results.mapNotNull { it.toDomain(MediaType.SERIES) }
            }
            cache.upsertAll(remote.map { it.toCache() })
            AppResult.Success(CatalogPage(remote, fromCache = false))
        } catch (error: Exception) {
            val cached = cache.getByType(type.name).map { it.toDomain() }
            if (cached.isNotEmpty()) AppResult.Success(CatalogPage(cached, fromCache = true))
            else AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
        }
    }

    override suspend fun search(query: String): AppResult<CatalogPage> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val items = api.search(query.trim()).results.mapNotNull { dto ->
                when (dto.mediaType) {
                    "movie" -> dto.toDomain(MediaType.MOVIE)
                    "tv" -> dto.toDomain(MediaType.SERIES)
                    else -> null
                }
            }
            AppResult.Success(CatalogPage(items, fromCache = false))
        } catch (error: Exception) {
            AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
        }
    }

    override suspend fun details(id: TmdbId, type: MediaType): AppResult<CatalogDetails> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val dto = when (type) {
                MediaType.MOVIE -> api.movieDetails(id.value)
                MediaType.SERIES -> api.seriesDetails(id.value)
            }
            AppResult.Success(dto.toDomain(type))
        } catch (error: Exception) {
            AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
        }
    }

    override fun observeFavorites(): Flow<List<CatalogItem>> = favorites.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override fun observeIsFavorite(id: TmdbId, type: MediaType): Flow<Boolean> =
        favorites.observeIsFavorite(id.value, type.name)

    override suspend fun setFavorite(item: CatalogItem, favorite: Boolean) {
        if (favorite) favorites.upsert(item.toFavorite())
        else favorites.delete(item.id.value, item.type.name)
    }
}

private fun TmdbMediaDto.toDomain(type: MediaType): CatalogItem? {
    val displayTitle = title ?: name ?: return null
    return CatalogItem(TmdbId(id), type, displayTitle, originalTitle ?: originalName, overview,
        posterPath, backdropPath, releaseDate ?: firstAirDate, voteAverage?.coerceIn(0.0, 10.0))
}

private fun TmdbDetailsDto.toDomain(type: MediaType): CatalogDetails {
    val item = CatalogItem(TmdbId(id), type, title ?: name ?: "—", originalTitle ?: originalName,
        overview, posterPath, backdropPath, releaseDate ?: firstAirDate, voteAverage?.coerceIn(0.0, 10.0))
    return CatalogDetails(item, genres.map { it.name }, seasons.map {
        CatalogSeason(it.id, it.seasonNumber, it.name, it.episodeCount)
    })
}

private fun CatalogItem.toCache() = CachedCatalogItemEntity(id.value, type.name, title, originalTitle,
    overview, posterPath, backdropPath, releaseDate, voteAverage, System.currentTimeMillis())

private fun CachedCatalogItemEntity.toDomain() = CatalogItem(TmdbId(tmdbId), MediaType.valueOf(mediaType),
    title, originalTitle, overview, posterPath, backdropPath, releaseDate, voteAverage)

private fun CatalogItem.toFavorite() = FavoriteMediaEntity(id.value, type.name, title, posterPath,
    System.currentTimeMillis())

private fun FavoriteMediaEntity.toDomain() = CatalogItem(TmdbId(tmdbId), MediaType.valueOf(mediaType),
    title, posterPath = posterPath)
