package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgApi
import com.shjamolov.mediastreamplayer.data.remote.iptv.XmlTvParser
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.GuideDto
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import com.shjamolov.mediastreamplayer.domain.model.TvGuideEntry
import com.shjamolov.mediastreamplayer.domain.repository.TvGuideRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException

class IptvOrgTvGuideRepository(
    private val api: IptvOrgApi,
    private val httpClient: OkHttpClient,
    private val parser: XmlTvParser,
    private val dispatchers: DispatcherProvider,
) : TvGuideRepository {
    private val guideCacheMutex = Mutex()
    private var cachedGuides: List<GuideDto>? = null

    override suspend fun getSchedule(
        channelId: ChannelId,
        feedId: String?,
    ): AppResult<List<TvGuideEntry>> = withContext(dispatchers.io) {
        try {
            val guide = loadGuides().selectGuide(channelId.value, feedId)
                ?: return@withContext AppResult.Success(emptyList())
            val xmlSources = guide.sources.filter { it.format.equals("XML", ignoreCase = true) }
            if (xmlSources.isEmpty()) return@withContext AppResult.Success(emptyList())

            var lastFailure: IOException? = null
            for (source in xmlSources) {
                try {
                    val request = Request.Builder().url(source.url).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("EPG request failed with HTTP ${response.code}")
                        }
                        val body = response.body ?: throw IOException("EPG response body is empty")
                        return@withContext AppResult.Success(
                            parser.parse(body.byteStream(), guide.siteId, channelId),
                        )
                    }
                } catch (error: IOException) {
                    lastFailure = error
                }
            }
            AppResult.Failure(AppError.Network(lastFailure))
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

    private suspend fun loadGuides(): List<GuideDto> =
        cachedGuides ?: guideCacheMutex.withLock {
            cachedGuides ?: api.getGuides().also { cachedGuides = it }
        }
}

internal fun List<GuideDto>.selectGuide(channelId: String, feedId: String?): GuideDto? =
    asSequence()
        .filter { it.channel == channelId && it.sources.isNotEmpty() }
        .sortedWith(
            compareByDescending<GuideDto> { feedId != null && it.feed == feedId }
                .thenByDescending { it.feed == null },
        )
        .firstOrNull()
