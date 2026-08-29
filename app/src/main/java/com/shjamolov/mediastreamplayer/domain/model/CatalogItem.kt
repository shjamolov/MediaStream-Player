package com.shjamolov.mediastreamplayer.domain.model

@JvmInline
value class TmdbId(val value: Long) {
    init {
        require(value > 0) { "TMDB id must be positive" }
    }
}

enum class MediaType {
    MOVIE,
    SERIES,
}

data class CatalogItem(
    val id: TmdbId,
    val type: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double? = null,
) {
    init {
        require(title.isNotBlank()) { "Catalog title must not be blank" }
        require(voteAverage == null || voteAverage in 0.0..10.0) {
            "Vote average must be between 0 and 10"
        }
    }
}
