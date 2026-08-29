package com.shjamolov.mediastreamplayer.domain.model

@JvmInline
value class ChannelId(val value: String) {
    init {
        require(value.isNotBlank()) { "Channel id must not be blank" }
    }
}

data class TvChannel(
    val id: ChannelId,
    val name: String,
    val alternativeNames: List<String> = emptyList(),
    val countryCode: String? = null,
    val languageCodes: Set<String> = emptySet(),
    val categoryIds: Set<String> = emptySet(),
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val isNsfw: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Channel name must not be blank" }
    }
}
