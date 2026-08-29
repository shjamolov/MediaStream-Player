package com.shjamolov.mediastreamplayer.domain.model

data class TvGuideEntry(
    val channelId: ChannelId,
    val title: String,
    val description: String? = null,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
) {
    init {
        require(title.isNotBlank()) { "Guide title must not be blank" }
        require(endsAtEpochMillis > startsAtEpochMillis) {
            "Guide entry must end after it starts"
        }
    }

    fun isAiringAt(epochMillis: Long): Boolean =
        epochMillis >= startsAtEpochMillis && epochMillis < endsAtEpochMillis
}
