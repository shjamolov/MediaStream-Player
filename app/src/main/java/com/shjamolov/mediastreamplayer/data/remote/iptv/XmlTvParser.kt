package com.shjamolov.mediastreamplayer.data.remote.iptv

import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import com.shjamolov.mediastreamplayer.domain.model.TvGuideEntry
import java.io.InputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class XmlTvParser {
    fun parse(
        input: InputStream,
        siteChannelId: String,
        domainChannelId: ChannelId,
    ): List<TvGuideEntry> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }
        val entries = mutableListOf<TvGuideEntry>()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (
                parser.eventType == XmlPullParser.START_TAG &&
                parser.name == PROGRAMME_TAG &&
                parser.getAttributeValue(null, CHANNEL_ATTRIBUTE) == siteChannelId
            ) {
                parseProgramme(parser, domainChannelId)?.let(entries::add)
            }
            parser.next()
        }
        return entries.sortedBy(TvGuideEntry::startsAtEpochMillis)
    }

    private fun parseProgramme(
        parser: XmlPullParser,
        channelId: ChannelId,
    ): TvGuideEntry? {
        val startsAt = parseTimestamp(parser.getAttributeValue(null, START_ATTRIBUTE)) ?: return null
        val endsAt = parseTimestamp(parser.getAttributeValue(null, STOP_ATTRIBUTE)) ?: return null
        var title: String? = null
        var description: String? = null

        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == PROGRAMME_TAG)) {
            parser.next()
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    TITLE_TAG -> if (title == null) title = parser.nextText().trim()
                    DESCRIPTION_TAG -> if (description == null) description = parser.nextText().trim()
                }
            }
        }

        return title
            ?.takeIf(String::isNotBlank)
            ?.let {
                TvGuideEntry(
                    channelId = channelId,
                    title = it,
                    description = description?.takeIf(String::isNotBlank),
                    startsAtEpochMillis = startsAt,
                    endsAtEpochMillis = endsAt,
                )
            }
    }

    private fun parseTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        for (pattern in TIMESTAMP_PATTERNS) {
            val position = ParsePosition(0)
            val result = SimpleDateFormat(pattern, Locale.ROOT).apply {
                isLenient = false
            }.parse(value, position)
            if (result != null && position.index == value.length) return result.time
        }
        return null
    }

    private companion object {
        const val PROGRAMME_TAG = "programme"
        const val TITLE_TAG = "title"
        const val DESCRIPTION_TAG = "desc"
        const val CHANNEL_ATTRIBUTE = "channel"
        const val START_ATTRIBUTE = "start"
        const val STOP_ATTRIBUTE = "stop"
        val TIMESTAMP_PATTERNS = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmm Z")
    }
}
