package com.shjamolov.mediastreamplayer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {
    @Test
    fun tvStream_buildsOnlyConfiguredRequestHeaders() {
        val stream = TvStream(
            channelId = ChannelId("example.us"),
            url = "https://example.com/live.m3u8",
            userAgent = "MediaStream Player",
            referrer = "https://example.com",
        )

        assertEquals(
            mapOf(
                "User-Agent" to "MediaStream Player",
                "Referer" to "https://example.com",
            ),
            stream.requestHeaders,
        )
    }

    @Test
    fun channelStreams_rejectsStreamOwnedByAnotherChannel() {
        val channel = TvChannel(ChannelId("first.us"), "First")
        val foreignStream = TvStream(ChannelId("second.us"), "https://example.com/live.m3u8")

        assertThrows(IllegalArgumentException::class.java) {
            TvChannelStreams(channel, listOf(foreignStream))
        }
    }

    @Test
    fun guideEntry_usesHalfOpenTimeRange() {
        val entry = TvGuideEntry(
            channelId = ChannelId("example.us"),
            title = "Evening news",
            startsAtEpochMillis = 1_000,
            endsAtEpochMillis = 2_000,
        )

        assertTrue(entry.isAiringAt(1_000))
        assertTrue(entry.isAiringAt(1_999))
        assertFalse(entry.isAiringAt(2_000))
    }

    @Test
    fun torrServerEndpoint_requiresCompleteCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            TorrServerEndpoint(
                mode = TorrServerMode.REMOTE,
                baseUrl = "http://192.168.1.10:8090",
                username = "user",
            )
        }
    }

    @Test
    fun catalogItem_rejectsRatingOutsideTmdbRange() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogItem(
                id = TmdbId(1),
                type = MediaType.MOVIE,
                title = "Example",
                voteAverage = 10.1,
            )
        }
    }
}
