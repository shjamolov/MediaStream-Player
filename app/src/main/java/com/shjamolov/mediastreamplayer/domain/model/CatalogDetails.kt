package com.shjamolov.mediastreamplayer.domain.model

data class CatalogDetails(
    val item: CatalogItem,
    val genres: List<String> = emptyList(),
    val seasons: List<CatalogSeason> = emptyList(),
    val runtimeMinutes: Int? = null,
    val certification: String? = null,
    val trailer: CatalogTrailer? = null,
    val cast: List<CatalogCastMember> = emptyList(),
    val recommendations: List<CatalogItem> = emptyList(),
    val similar: List<CatalogItem> = emptyList(),
    val imdbId: String? = null,
    val watchProviders: List<String> = emptyList(),
)

data class CatalogSeason(
    val id: Long,
    val number: Int,
    val name: String,
    val episodeCount: Int,
)

data class CatalogTrailer(val name: String, val site: String, val key: String)
data class CatalogCastMember(val id: Long, val name: String, val character: String?, val profilePath: String?)
data class CatalogEpisode(
    val id: Long,
    val number: Int,
    val name: String,
    val overview: String?,
    val stillPath: String?,
    val runtimeMinutes: Int?,
)
