package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgRemoteDataSource
import com.shjamolov.mediastreamplayer.data.remote.iptv.IptvOrgSnapshot
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.CategoryDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.ChannelDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.CountryDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.FeedDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LanguageDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.LogoDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.StreamDto
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.repository.AdultContentAccess
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IptvOrgTvCatalogRepositoryTest {
    @Test
    fun getCatalog_blocksNsfwByDefaultAndOmitsOrphanStreams() = runTest {
        val repository = repositoryWith(snapshot())

        val result = repository.getCatalog() as AppResult.Success

        assertEquals(listOf("News.uz"), result.value.channels.map { it.channel.id.value })
        assertTrue(result.value.channels.none { it.channel.isNsfw })
    }

    @Test
    fun getCatalog_unlockedGroupsDeduplicatesAndOrdersStreamsByQuality() = runTest {
        val repository = repositoryWith(snapshot())

        val result = repository.getCatalog(AdultContentAccess.UNLOCKED) as AppResult.Success
        val news = result.value.channels.single { it.channel.id.value == "News.uz" }

        assertEquals(2, news.streams.size)
        assertEquals(listOf("1080p", "720p"), news.streams.map { it.quality })
        assertEquals(setOf("uzb", "rus"), news.channel.languageCodes)
        assertEquals("https://img.example/generic.png", news.channel.logoUrl)
        assertTrue(result.value.channels.any { it.channel.isNsfw })
    }

    @Test
    fun getCatalog_mapsIoFailureToNetworkError() = runTest {
        val repository = IptvOrgTvCatalogRepository(
            object : IptvOrgRemoteDataSource {
                override suspend fun loadCatalog(): IptvOrgSnapshot = throw IOException("offline")
            },
        )

        val result = repository.getCatalog()

        assertTrue(result is AppResult.Failure)
    }

    private fun repositoryWith(snapshot: IptvOrgSnapshot) =
        IptvOrgTvCatalogRepository(
            object : IptvOrgRemoteDataSource {
                override suspend fun loadCatalog(): IptvOrgSnapshot = snapshot
            },
        )

    private fun snapshot() = IptvOrgSnapshot(
        channels = listOf(
            ChannelDto("News.uz", "News", country = "UZ", categories = listOf("news")),
            ChannelDto("Adult.us", "Adult", country = "US", isNsfw = true),
        ),
        feeds = listOf(
            FeedDto("News.uz", "Main", listOf("uzb", "rus")),
        ),
        streams = listOf(
            StreamDto("News.uz", "Main", "News HD", "https://example.com/hd.m3u8", quality = "1080p"),
            StreamDto("News.uz", "Main", "News", "https://example.com/sd.m3u8", quality = "720p"),
            StreamDto("News.uz", "Main", "Duplicate", "https://example.com/sd.m3u8", quality = "480p"),
            StreamDto("Adult.us", null, "Adult", "https://example.com/adult.m3u8"),
            StreamDto(null, null, "Orphan", "https://example.com/orphan.m3u8"),
        ),
        logos = listOf(
            LogoDto("News.uz", "Main", true, 2000, 1000, "https://img.example/feed.png"),
            LogoDto("News.uz", null, false, 100, 50, "https://img.example/generic.png"),
        ),
        categories = listOf(CategoryDto("news", "News", "News programming")),
        countries = listOf(CountryDto("Uzbekistan", "UZ", listOf("uzb"), "🇺🇿")),
        languages = listOf(LanguageDto("Uzbek", "uzb")),
    )
}
