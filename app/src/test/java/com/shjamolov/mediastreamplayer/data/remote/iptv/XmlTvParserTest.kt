package com.shjamolov.mediastreamplayer.data.remote.iptv

import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class XmlTvParserTest {
    @Test
    fun parse_keepsOnlyTargetChannelAndOrdersProgrammes() {
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <programme start="20260830120000 +0500" stop="20260830130000 +0500" channel="target">
                <title lang="en">Second</title>
              </programme>
              <programme start="20260830110000 +0500" stop="20260830120000 +0500" channel="target">
                <title lang="en">First</title>
                <desc lang="en">Description</desc>
              </programme>
              <programme start="20260830110000 +0500" stop="20260830120000 +0500" channel="other">
                <title>Ignored</title>
              </programme>
            </tv>
            """.trimIndent()

        val entries = XmlTvParser().parse(
            input = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)),
            siteChannelId = "target",
            domainChannelId = ChannelId("Example.uz"),
        )

        assertEquals(listOf("First", "Second"), entries.map { it.title })
        assertEquals("Description", entries.first().description)
        assertEquals(60 * 60 * 1000L, entries.first().endsAtEpochMillis - entries.first().startsAtEpochMillis)
    }
}
