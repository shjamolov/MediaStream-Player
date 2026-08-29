package com.shjamolov.mediastreamplayer.presentation.player

import com.shjamolov.mediastreamplayer.domain.model.TvStream

class StreamFallbackQueue(
    private val streams: List<TvStream>,
) {
    init {
        require(streams.isNotEmpty()) { "At least one stream is required" }
    }

    var currentIndex: Int = 0
        private set

    val current: TvStream
        get() = streams[currentIndex]

    val size: Int
        get() = streams.size

    fun advance(): TvStream? {
        if (currentIndex == streams.lastIndex) return null
        currentIndex += 1
        return current
    }

    fun reset(): TvStream {
        currentIndex = 0
        return current
    }
}
