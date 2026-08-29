package com.shjamolov.mediastreamplayer.data.remote.iptv

import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.CategoryDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.ChannelDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.CountryDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.FeedDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LanguageDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LogoDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.StreamDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

interface IptvOrgRemoteDataSource {
    suspend fun loadCatalog(): IptvOrgSnapshot
}

data class IptvOrgSnapshot(
    val channels: List<ChannelDto>,
    val feeds: List<FeedDto>,
    val streams: List<StreamDto>,
    val logos: List<LogoDto>,
    val categories: List<CategoryDto>,
    val countries: List<CountryDto>,
    val languages: List<LanguageDto>,
)

class DefaultIptvOrgRemoteDataSource(
    private val api: IptvOrgApi,
    private val dispatchers: DispatcherProvider,
) : IptvOrgRemoteDataSource {
    override suspend fun loadCatalog(): IptvOrgSnapshot = withContext(dispatchers.io) {
        coroutineScope {
            val channels = async { api.getChannels() }
            val feeds = async { api.getFeeds() }
            val streams = async { api.getStreams() }
            val logos = async { api.getLogos() }
            val categories = async { api.getCategories() }
            val countries = async { api.getCountries() }
            val languages = async { api.getLanguages() }

            IptvOrgSnapshot(
                channels = channels.await(),
                feeds = feeds.await(),
                streams = streams.await(),
                logos = logos.await(),
                categories = categories.await(),
                countries = countries.await(),
                languages = languages.await(),
            )
        }
    }
}
