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
