package com.shjamolov.mediastreamplayer.domain.model

sealed interface PlaybackSource {
    val url: String
    val headers: Map<String, String>

    data class Hls(
        override val url: String,
        override val headers: Map<String, String> = emptyMap(),
    ) : PlaybackSource {
        init {
            require(url.isNotBlank()) { "Playback URL must not be blank" }
        }
    }

    data class Dash(
        override val url: String,
        override val headers: Map<String, String> = emptyMap(),
    ) : PlaybackSource {
        init {
            require(url.isNotBlank()) { "Playback URL must not be blank" }
        }
    }

    data class Progressive(
        override val url: String,
        override val headers: Map<String, String> = emptyMap(),
    ) : PlaybackSource {
        init {
            require(url.isNotBlank()) { "Playback URL must not be blank" }
        }
    }
}
