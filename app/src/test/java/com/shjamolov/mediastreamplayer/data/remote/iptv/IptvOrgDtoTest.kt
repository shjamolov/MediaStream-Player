package com.shjamolov.mediastreamplayer.data.remote.iptv

import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.StreamDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class IptvOrgDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun streamDto_decodesOfficialSnakeCaseFieldsAndUnknownFields() {
        val payload =
            """
            {
              "channel": "Example.us",
              "feed": null,
              "title": "Example HD",
              "url": "https://example.com/live.m3u8",
              "referrer": "https://example.com/",
              "user_agent": "Example Agent",
              "quality": "1080p",
              "label": "Geo-blocked",
              "future_field": true
            }
            """.trimIndent()

        val stream = json.decodeFromString<StreamDto>(payload)

        assertEquals("Example Agent", stream.userAgent)
        assertEquals("https://example.com/", stream.referrer)
        assertEquals("1080p", stream.quality)
    }
}
