package com.shjamolov.mediastreamplayer.presentation.player

import androidx.media3.common.MimeTypes
import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import com.shjamolov.mediastreamplayer.domain.model.TvStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamFallbackQueueTest {
    private val channelId = ChannelId("example.us")
    private val first = TvStream(channelId, "https://example.com/best.m3u8")
    private val second = TvStream(channelId, "https://example.com/fallback.m3u8")

    @Test
    fun advance_usesEachStreamOnceAndStopsAtEnd() {
        val queue = StreamFallbackQueue(listOf(first, second))

        assertEquals(first, queue.current)
        assertEquals(second, queue.advance())
        assertNull(queue.advance())
        assertEquals(1, queue.currentIndex)
    }

    @Test
    fun reset_returnsToBestStreamAfterExhaustion() {
        val queue = StreamFallbackQueue(listOf(first, second))
        queue.advance()

        assertEquals(first, queue.reset())
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun inferMimeType_detectsManifestBeforeQueryString() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            inferMimeType("https://example.com/live.M3U8?token=abc"),
        )
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            inferMimeType("https://example.com/live.mpd"),
        )
        assertNull(inferMimeType("https://example.com/video"))
    }
}
