package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgRemoteDataSource
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgSnapshot
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LogoDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.StreamDto
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import com.shjamolov.mediastreamplayer.domain.model.TvCatalog
import com.shjamolov.mediastreamplayer.domain.model.TvCategory
import com.shjamolov.mediastreamplayer.domain.model.TvChannel
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.domain.model.TvCountry
import com.shjamolov.mediastreamplayer.domain.model.TvLanguage
import com.shjamolov.mediastreamplayer.domain.model.TvStream
import com.shjamolov.mediastreamplayer.domain.repository.AdultContentAccess
import com.shjamolov.mediastreamplayer.domain.repository.TvCatalogRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class IptvOrgTvCatalogRepository(
    private val remoteDataSource: IptvOrgRemoteDataSource,
) : TvCatalogRepository {
    override suspend fun getCatalog(adultContentAccess: AdultContentAccess): AppResult<TvCatalog> =
        try {
            AppResult.Success(
                remoteDataSource.loadCatalog().toDomain(adultContentAccess),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            AppResult.Failure(AppError.Network(error))
        } catch (error: HttpException) {
            AppResult.Failure(AppError.Network(error))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unexpected(error))
        }
}

internal fun IptvOrgSnapshot.toDomain(adultContentAccess: AdultContentAccess): TvCatalog {
    val allowedChannelIds = channels
        .asSequence()
        .filter { it.country.uppercase() in SUPPORTED_TV_COUNTRIES }
        .map { it.id }
        .toSet()
    val streamsByChannel = streams
        .asSequence()
        .filter { it.channel in allowedChannelIds && it.url.isNotBlank() }
        .groupBy { checkNotNull(it.channel) }
    val languagesByChannel = feeds
        .groupBy { it.channel }
        .mapValues { (_, channelFeeds) ->
            channelFeeds.flatMap { it.languages }.toSet()
        }
    val logosByChannel = logos.groupBy { it.channel }

    val playableChannels = channels
        .asSequence()
        .filter { it.id in allowedChannelIds }
        .filter { adultContentAccess == AdultContentAccess.UNLOCKED || !it.isNsfw }
        .mapNotNull { channel ->
            val channelStreams = streamsByChannel[channel.id]
                ?.distinctBy(StreamDto::url)
                ?.sortedWith(compareByDescending(::qualityRank).thenBy(StreamDto::url))
                .orEmpty()
            if (channelStreams.isEmpty()) return@mapNotNull null

            val channelId = ChannelId(channel.id)
            TvChannelStreams(
                channel = TvChannel(
                    id = channelId,
                    name = channel.name,
                    alternativeNames = channel.alternativeNames,
                    countryCode = channel.country,
                    languageCodes = languagesByChannel[channel.id].orEmpty(),
                    categoryIds = channel.categories.toSet(),
                    logoUrl = selectLogo(logosByChannel[channel.id].orEmpty()),
                    websiteUrl = channel.website,
                    isNsfw = channel.isNsfw,
                ),
                streams = channelStreams.map { stream ->
                    TvStream(
                        channelId = channelId,
                        url = stream.url,
                        feedId = stream.feed,
                        title = stream.title,
                        quality = stream.quality,
                        label = stream.label,
                        userAgent = stream.userAgent,
                        referrer = stream.referrer,
                    )
                },
            )
        }
        .sortedBy { it.channel.name.lowercase() }
        .toList()

    return TvCatalog(
        channels = playableChannels,
        categories = categories.map { TvCategory(it.id, it.name, it.description) },
        countries = countries.filter { it.code.uppercase() in SUPPORTED_TV_COUNTRIES }.map {
            TvCountry(it.code, it.name, it.languages.toSet(), it.flag)
        },
        languages = languages.map { TvLanguage(it.code, it.name) },
    )
}

private fun selectLogo(logos: List<LogoDto>): String? =
    logos.maxWithOrNull(
        compareBy<LogoDto> { it.feed == null }
            .thenBy { it.inUse }
            .thenBy { it.width.toLong() * it.height },
    )?.url

private fun qualityRank(stream: StreamDto): Int =
    QUALITY_NUMBER.find(stream.quality.orEmpty())?.value?.toIntOrNull() ?: 0

private val QUALITY_NUMBER = Regex("\\d+")

internal val SUPPORTED_TV_COUNTRIES = setOf("UZ", "RU", "KZ")
