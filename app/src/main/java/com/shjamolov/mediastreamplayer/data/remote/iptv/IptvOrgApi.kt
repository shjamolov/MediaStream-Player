package com.shjamolov.mediastreamplayer.data.remote.iptv

import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.CategoryDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.ChannelDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.CountryDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.FeedDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.GuideDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LanguageDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LogoDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.StreamDto
import retrofit2.http.GET

interface IptvOrgApi {
    @GET("channels.json")
    suspend fun getChannels(): List<ChannelDto>

    @GET("feeds.json")
    suspend fun getFeeds(): List<FeedDto>

    @GET("streams.json")
    suspend fun getStreams(): List<StreamDto>

    @GET("logos.json")
    suspend fun getLogos(): List<LogoDto>

    @GET("guides.json")
    suspend fun getGuides(): List<GuideDto>

    @GET("categories.json")
    suspend fun getCategories(): List<CategoryDto>

    @GET("countries.json")
    suspend fun getCountries(): List<CountryDto>

    @GET("languages.json")
    suspend fun getLanguages(): List<LanguageDto>
}
