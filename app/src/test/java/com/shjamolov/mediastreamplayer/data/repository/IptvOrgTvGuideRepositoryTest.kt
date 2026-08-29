package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.GuideDto
import com.shjamolov.mediastreamplayer.data.remote.iptv.dto.GuideSourceDto
import org.junit.Assert.assertEquals
import org.junit.Test

class IptvOrgTvGuideRepositoryTest {
    @Test
    fun selectGuide_prefersExactFeedThenChannelLevelGuide() {
        val channelGuide = guide(feed = null, siteId = "channel")
        val sdGuide = guide(feed = "SD", siteId = "sd")
        val hdGuide = guide(feed = "HD", siteId = "hd")
        val guides = listOf(channelGuide, sdGuide, hdGuide)

        assertEquals("hd", guides.selectGuide("Example.uz", "HD")?.siteId)
        assertEquals("channel", guides.selectGuide("Example.uz", "Unknown")?.siteId)
    }

    private fun guide(feed: String?, siteId: String) = GuideDto(
        channel = "Example.uz",
        feed = feed,
        site = "example.com",
        siteId = siteId,
        siteName = "Example",
        lang = "en",
        sources = listOf(GuideSourceDto("example.com", "https://example.com/guide.xml", "XML")),
    )
}
