package com.shjamolov.mediastreamplayer.data.remote.tmdb.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPageDto(val results: List<TmdbMediaDto> = emptyList())

@Serializable
data class TmdbMediaDto(
    val id: Long,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

@Serializable
data class TmdbDetailsDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val seasons: List<TmdbSeasonDto> = emptyList(),
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val videos: TmdbVideosDto = TmdbVideosDto(),
    val credits: TmdbCreditsDto = TmdbCreditsDto(),
    val recommendations: TmdbPageDto = TmdbPageDto(),
    val similar: TmdbPageDto = TmdbPageDto(),
    @SerialName("external_ids") val externalIds: TmdbExternalIdsDto = TmdbExternalIdsDto(),
    @SerialName("release_dates") val releaseDates: TmdbReleaseDatesDto = TmdbReleaseDatesDto(),
    @SerialName("content_ratings") val contentRatings: TmdbContentRatingsDto = TmdbContentRatingsDto(),
    @SerialName("watch/providers") val watchProviders: TmdbWatchProvidersDto = TmdbWatchProvidersDto(),
)

@Serializable data class TmdbGenreDto(val id: Long, val name: String)

@Serializable
data class TmdbSeasonDto(
    val id: Long,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    @SerialName("episode_count") val episodeCount: Int = 0,
)

@Serializable data class TmdbVideosDto(val results: List<TmdbVideoDto> = emptyList())
@Serializable data class TmdbVideoDto(
    val name: String,
    val key: String,
    val site: String,
    val type: String,
    val official: Boolean = false,
)

@Serializable data class TmdbCreditsDto(val cast: List<TmdbCastDto> = emptyList())
@Serializable data class TmdbCastDto(
    val id: Long,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = Int.MAX_VALUE,
)

@Serializable data class TmdbExternalIdsDto(@SerialName("imdb_id") val imdbId: String? = null)
@Serializable data class TmdbReleaseDatesDto(val results: List<TmdbReleaseCountryDto> = emptyList())
@Serializable data class TmdbReleaseCountryDto(
    @SerialName("iso_3166_1") val country: String,
    @SerialName("release_dates") val dates: List<TmdbReleaseCertificationDto> = emptyList(),
)
@Serializable data class TmdbReleaseCertificationDto(val certification: String = "", val type: Int = 0)
@Serializable data class TmdbContentRatingsDto(val results: List<TmdbContentRatingDto> = emptyList())
@Serializable data class TmdbContentRatingDto(@SerialName("iso_3166_1") val country: String, val rating: String = "")

@Serializable data class TmdbWatchProvidersDto(val results: Map<String, TmdbWatchRegionDto> = emptyMap())
@Serializable data class TmdbWatchRegionDto(
    val flatrate: List<TmdbProviderDto> = emptyList(),
    val free: List<TmdbProviderDto> = emptyList(),
    val ads: List<TmdbProviderDto> = emptyList(),
    val rent: List<TmdbProviderDto> = emptyList(),
    val buy: List<TmdbProviderDto> = emptyList(),
)
@Serializable data class TmdbProviderDto(@SerialName("provider_name") val name: String)

@Serializable data class TmdbSeasonDetailsDto(val episodes: List<TmdbEpisodeDto> = emptyList())
@Serializable data class TmdbEpisodeDto(
    val id: Long,
    @SerialName("episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    val runtime: Int? = null,
)
