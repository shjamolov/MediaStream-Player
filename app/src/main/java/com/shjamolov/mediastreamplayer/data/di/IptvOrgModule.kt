package com.shjamolov.mediastreamplayer.data.di

import com.shjamolov.mediastreamplayer.data.remote.iptv.DefaultIptvOrgRemoteDataSource
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgApi
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgRemoteDataSource
import com.shjamolov.mediastreamplayer.data.remote.iptv.XmlTvParser
import com.shjamolov.mediastreamplayer.data.repository.IptvOrgTvCatalogRepository
import com.shjamolov.mediastreamplayer.data.repository.IptvOrgTvGuideRepository
import com.shjamolov.mediastreamplayer.domain.repository.TvCatalogRepository
import com.shjamolov.mediastreamplayer.domain.repository.TvGuideRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val IPTV_ORG_API_URL = "https://iptv-org.github.io/api/"

val iptvOrgModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(IPTV_ORG_API_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<IptvOrgApi> { get<Retrofit>().create(IptvOrgApi::class.java) }
    single<IptvOrgRemoteDataSource> { DefaultIptvOrgRemoteDataSource(get(), get()) }
    single<TvCatalogRepository> { IptvOrgTvCatalogRepository(get()) }
    single { XmlTvParser() }
    single<TvGuideRepository> { IptvOrgTvGuideRepository(get(), get(), get(), get()) }
}
