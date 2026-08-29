package com.shjamolov.mediastreamplayer.domain.model

data class TvStream(
    val channelId: ChannelId,
    val url: String,
    val feedId: String? = null,
    val title: String? = null,
    val quality: String? = null,
    val label: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
) {
    init {
        require(url.isNotBlank()) { "Stream URL must not be blank" }
    }

    val requestHeaders: Map<String, String>
        get() = buildMap {
            userAgent?.takeIf(String::isNotBlank)?.let { put("User-Agent", it) }
            referrer?.takeIf(String::isNotBlank)?.let { put("Referer", it) }
        }
}

data class TvChannelStreams(
    val channel: TvChannel,
    val streams: List<TvStream>,
) {
    init {
        require(streams.isNotEmpty()) { "A playable channel must have at least one stream" }
        require(streams.all { it.channelId == channel.id }) {
            "Every stream must belong to the channel"
        }
    }
}
