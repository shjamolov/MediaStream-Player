package com.shjamolov.mediastreamplayer.data.di

import com.shjamolov.mediastreamplayer.data.remote.iptv.DefaultIptvOrgRemoteDataSource
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgApi
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgRemoteDataSource
import com.shjamolov.mediastreamplayer.data.repository.IptvOrgTvCatalogRepository
import com.shjamolov.mediastreamplayer.domain.repository.TvCatalogRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val IPTV_ORG_API_URL = "https://iptv-org.github.io/api/"

val iptvOrgModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single {
        Retrofit.Builder()
            .baseUrl(IPTV_ORG_API_URL)
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<IptvOrgApi> { get<Retrofit>().create(IptvOrgApi::class.java) }
    single<IptvOrgRemoteDataSource> { DefaultIptvOrgRemoteDataSource(get(), get()) }
    single<TvCatalogRepository> { IptvOrgTvCatalogRepository(get()) }
}
