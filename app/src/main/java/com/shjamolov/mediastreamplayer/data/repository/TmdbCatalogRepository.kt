package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.data.local.dao.CatalogCacheDao
import com.shjamolov.mediastreamplayer.data.local.dao.FavoriteMediaDao
import com.shjamolov.mediastreamplayer.data.local.entity.CachedCatalogItemEntity
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteMediaEntity
import com.shjamolov.mediastreamplayer.data.remote.tmdb.TmdbApi
import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbDetailsDto
import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbMediaDto
import com.shjamolov.mediastreamplayer.core.settings.AppSettingsStore
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.CatalogSeason
import com.shjamolov.mediastreamplayer.domain.model.CatalogTrailer
import com.shjamolov.mediastreamplayer.domain.model.CatalogCastMember
import com.shjamolov.mediastreamplayer.domain.model.CatalogEpisode
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.domain.model.TmdbId
import com.shjamolov.mediastreamplayer.domain.repository.CatalogPage
import com.shjamolov.mediastreamplayer.domain.repository.CatalogRepository
import com.shjamolov.mediastreamplayer.domain.repository.CatalogShelf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class TmdbCatalogRepository(
    private val api: TmdbApi,
    private val cache: CatalogCacheDao,
    private val favorites: FavoriteMediaDao,
    private val token: String,
    private val settings: AppSettingsStore,
) : CatalogRepository {
    override suspend fun popular(type: MediaType): AppResult<CatalogPage> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val remote = (1..POPULAR_PAGE_COUNT).flatMap { page ->
                when (type) {
                    MediaType.MOVIE -> api.popularMovies(settings.language.value.apiCode, page).results.mapNotNull { it.toDomain(MediaType.MOVIE) }
                    MediaType.SERIES -> api.popularSeries(settings.language.value.apiCode, page).results.mapNotNull { it.toDomain(MediaType.SERIES) }
                }
            }.distinctBy { it.id }
            cache.upsertAll(remote.map { it.toCache() })
            AppResult.Success(CatalogPage(remote, fromCache = false))
        } catch (error: Exception) {
            val cached = cache.getByType(type.name).map { it.toDomain() }
            if (cached.isNotEmpty()) AppResult.Success(CatalogPage(cached, fromCache = true))
            else AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
        }
    }

    override suspend fun home(type: MediaType): AppResult<List<CatalogShelf>> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val language = settings.language.value.apiCode
            val shelves = when (type) {
                MediaType.MOVIE -> listOf(
                    CatalogShelf("trending", "Тренды недели", api.trending("movie", language).toItems(type)),
                    CatalogShelf("now_playing", "Сейчас в кино", api.nowPlayingMovies(language).toItems(type)),
                    CatalogShelf("upcoming", "Скоро", api.upcomingMovies(language).toItems(type)),
                    CatalogShelf("top_rated", "Лучшее по рейтингу", api.topRatedMovies(language).toItems(type)),
                )
                MediaType.SERIES -> listOf(
                    CatalogShelf("trending", "Тренды недели", api.trending("tv", language).toItems(type)),
                    CatalogShelf("on_the_air", "Сейчас в эфире", api.onTheAirSeries(language).toItems(type)),
                    CatalogShelf("airing_today", "Новые серии сегодня", api.airingTodaySeries(language).toItems(type)),
                    CatalogShelf("top_rated", "Лучшие сериалы", api.topRatedSeries(language).toItems(type)),
                )
            }.filter { it.items.isNotEmpty() }
            val allItems = shelves.flatMap(CatalogShelf::items).distinctBy { it.id }
            cache.upsertAll(allItems.map { it.toCache() })
            AppResult.Success(shelves)
        } catch (error: Exception) {
            val cached = cache.getByType(type.name).map { it.toDomain() }
            if (cached.isNotEmpty()) {
                AppResult.Success(listOf(CatalogShelf("offline", "Сохранённый каталог", cached)))
            } else {
                AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
            }
        }
    }

    override suspend fun discover(type: MediaType, genreId: Int): AppResult<CatalogPage> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val items = (1..DISCOVER_PAGE_COUNT).flatMap { page ->
                when (type) {
                    MediaType.MOVIE -> api.discoverMovies(genreId, settings.language.value.apiCode, page = page).results.mapNotNull { it.toDomain(type) }
                    MediaType.SERIES -> api.discoverSeries(genreId, settings.language.value.apiCode, page = page).results.mapNotNull { it.toDomain(type) }
                }
            }.distinctBy { it.id }
            AppResult.Success(CatalogPage(items, fromCache = false))
        } catch (error: Exception) {
            AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
        }
    }

    override suspend fun search(query: String): AppResult<CatalogPage> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val items = api.search(query.trim(), settings.language.value.apiCode).results.mapNotNull { dto ->
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
                MediaType.MOVIE -> api.movieDetails(id.value, settings.language.value.apiCode)
                MediaType.SERIES -> api.seriesDetails(id.value, settings.language.value.apiCode)
            }
            val fallbackVideos = if (dto.videos.results.isEmpty()) {
                runCatching {
                    when (type) {
                        MediaType.MOVIE -> api.movieVideos(id.value).results
                        MediaType.SERIES -> api.seriesVideos(id.value).results
                    }
                }.getOrDefault(emptyList())
            } else emptyList()
            AppResult.Success(dto.toDomain(type, fallbackVideos))
        } catch (error: Exception) {
            AppResult.Failure(if (error is IOException) AppError.Network(error) else AppError.Unexpected(error))
        }
    }

    override suspend fun seasonEpisodes(seriesId: TmdbId, seasonNumber: Int): AppResult<List<CatalogEpisode>> {
        if (token.isBlank()) return AppResult.Failure(AppError.Configuration("TMDB_API_TOKEN is missing"))
        return try {
            val episodes = api.seasonDetails(seriesId.value, seasonNumber, settings.language.value.apiCode).episodes.map {
                CatalogEpisode(it.id, it.episodeNumber, it.name, it.overview, it.stillPath, it.runtime)
            }
            AppResult.Success(episodes)
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

private fun com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbPageDto.toItems(type: MediaType) =
    results.mapNotNull { it.toDomain(type) }.distinctBy { it.id }

private fun TmdbDetailsDto.toDomain(
    type: MediaType,
    fallbackVideos: List<com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbVideoDto> = emptyList(),
): CatalogDetails {
    val item = CatalogItem(TmdbId(id), type, title ?: name ?: "—", originalTitle ?: originalName,
        overview, posterPath, backdropPath, releaseDate ?: firstAirDate, voteAverage?.coerceIn(0.0, 10.0))
    val mappedRecommendations = recommendations.results.mapNotNull { it.toDomain(type) }
    val mappedSimilar = similar.results.mapNotNull { it.toDomain(type) }
    val trailerDto = (videos.results + fallbackVideos).sortedWith(compareByDescending<com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbVideoDto> { it.official }
        .thenBy { it.type != "Trailer" }).firstOrNull { it.site.equals("YouTube", true) }
    val providerRegion = listOf("UZ", "RU", "US").firstNotNullOfOrNull(watchProviders.results::get)
        ?: watchProviders.results.values.firstOrNull()
    val providers = providerRegion?.let { region ->
        (region.flatrate + region.free + region.ads + region.rent + region.buy).map { it.name }.distinct()
    }.orEmpty()
    val rating = if (type == MediaType.MOVIE) {
        listOf("RU", "US").firstNotNullOfOrNull { country ->
            releaseDates.results.firstOrNull { it.country == country }?.dates?.firstOrNull { it.certification.isNotBlank() }?.certification
        }
    } else {
        listOf("RU", "US").firstNotNullOfOrNull { country ->
            contentRatings.results.firstOrNull { it.country == country }?.rating?.takeIf(String::isNotBlank)
        }
    }
    return CatalogDetails(
        item = item,
        genres = genres.map { it.name },
        seasons = seasons.map { CatalogSeason(it.id, it.seasonNumber, it.name, it.episodeCount) },
        runtimeMinutes = runtime ?: episodeRunTime.firstOrNull(),
        certification = rating,
        trailer = trailerDto?.let { CatalogTrailer(it.name, it.site, it.key) },
        cast = credits.cast.sortedBy { it.order }.take(15).map { CatalogCastMember(it.id, it.name, it.character, it.profilePath) },
        recommendations = mappedRecommendations,
        similar = mappedSimilar,
        imdbId = externalIds.imdbId,
        watchProviders = providers,
    )
}

private fun CatalogItem.toCache() = CachedCatalogItemEntity(id.value, type.name, title, originalTitle,
    overview, posterPath, backdropPath, releaseDate, voteAverage, System.currentTimeMillis())

private fun CachedCatalogItemEntity.toDomain() = CatalogItem(TmdbId(tmdbId), MediaType.valueOf(mediaType),
    title, originalTitle, overview, posterPath, backdropPath, releaseDate, voteAverage)

private fun CatalogItem.toFavorite() = FavoriteMediaEntity(id.value, type.name, title, posterPath,
    System.currentTimeMillis())

private fun FavoriteMediaEntity.toDomain() = CatalogItem(TmdbId(tmdbId), MediaType.valueOf(mediaType),
    title, posterPath = posterPath)

private const val POPULAR_PAGE_COUNT = 3
private const val DISCOVER_PAGE_COUNT = 2
