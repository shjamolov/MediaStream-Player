package com.shjamolov.mediastreamplayer.data.remote.tmdb

import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbDetailsDto
import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("3/movie/popular")
    suspend fun popularMovies(@Query("language") language: String = "ru-RU"): TmdbPageDto

    @GET("3/tv/popular")
    suspend fun popularSeries(@Query("language") language: String = "ru-RU"): TmdbPageDto

    @GET("3/search/multi")
    suspend fun search(
        @Query("query") query: String,
        @Query("language") language: String = "ru-RU",
        @Query("include_adult") includeAdult: Boolean = false,
    ): TmdbPageDto

    @GET("3/movie/{id}")
    suspend fun movieDetails(
        @Path("id") id: Long,
        @Query("language") language: String = "ru-RU",
    ): TmdbDetailsDto

    @GET("3/tv/{id}")
    suspend fun seriesDetails(
        @Path("id") id: Long,
        @Query("language") language: String = "ru-RU",
    ): TmdbDetailsDto
}
