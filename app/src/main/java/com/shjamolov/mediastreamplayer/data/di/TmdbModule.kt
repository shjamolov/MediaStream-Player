package com.shjamolov.mediastreamplayer.data.di

import com.shjamolov.mediastreamplayer.BuildConfig
import com.shjamolov.mediastreamplayer.data.remote.tmdb.TmdbApi
import com.shjamolov.mediastreamplayer.data.repository.TmdbCatalogRepository
import com.shjamolov.mediastreamplayer.domain.repository.CatalogRepository
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val tmdbRetrofit = named("tmdbRetrofit")

val tmdbModule = module {
    single(tmdbRetrofit) {
        val authorization = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.TMDB_API_TOKEN}")
                .header("accept", "application/json")
                .build()
            chain.proceed(request)
        }
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .client(get<OkHttpClient>().newBuilder().addInterceptor(authorization).build())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<TmdbApi> { get<Retrofit>(tmdbRetrofit).create(TmdbApi::class.java) }
    single<CatalogRepository> {
        TmdbCatalogRepository(get(), get(), get(), BuildConfig.TMDB_API_TOKEN, get())
    }
}
