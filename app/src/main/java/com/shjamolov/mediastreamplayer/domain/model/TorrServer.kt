package com.shjamolov.mediastreamplayer.domain.model

enum class TorrServerMode {
    LOCAL_MANAGED,
    LOCAL_EXTERNAL,
    REMOTE,
}

data class TorrServerEndpoint(
    val mode: TorrServerMode,
    val baseUrl: String,
    val username: String? = null,
    val password: String? = null,
) {
    init {
        require(baseUrl.isNotBlank()) { "TorrServer URL must not be blank" }
        require((username == null) == (password == null)) {
            "TorrServer credentials must be provided together"
        }
    }
}

data class TorrentVideoFile(
    val id: Int,
    val path: String,
    val sizeBytes: Long,
) {
    init {
        require(id >= 0) { "Torrent file id must not be negative" }
        require(path.isNotBlank()) { "Torrent file path must not be blank" }
        require(sizeBytes >= 0) { "Torrent file size must not be negative" }
    }
}

data class TorrentContent(
    val hash: String,
    val title: String,
    val files: List<TorrentVideoFile>,
)

data class TorrentPlaybackSource(
    val title: String,
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
)

data class TorrentSearchResult(
    val title: String,
    val source: String,
    val size: String,
    val seeders: Int,
    val peers: Int,
    val quality: Int?,
    val magnetOrLink: String,
    val sizeBytes: Long? = null,
    val audioCompatibility: Int = 0,
)
