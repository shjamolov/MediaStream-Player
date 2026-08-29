package com.shjamolov.mediastreamplayer.data.remote.iptv.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelDto(
    val id: String,
    val name: String,
    @SerialName("alt_names") val alternativeNames: List<String> = emptyList(),
    val country: String,
    val categories: List<String> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    val website: String? = null,
)

@Serializable
data class FeedDto(
    val channel: String,
    val id: String,
    val languages: List<String> = emptyList(),
)

@Serializable
data class StreamDto(
    val channel: String? = null,
    val feed: String? = null,
    val title: String,
    val url: String,
    val referrer: String? = null,
    @SerialName("user_agent") val userAgent: String? = null,
    val quality: String? = null,
    val label: String? = null,
)

@Serializable
data class LogoDto(
    val channel: String,
    val feed: String? = null,
    @SerialName("in_use") val inUse: Boolean,
    val width: Int,
    val height: Int,
    val url: String,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String,
)

@Serializable
data class CountryDto(
    val name: String,
    val code: String,
    val languages: List<String> = emptyList(),
    val flag: String,
)

@Serializable
data class LanguageDto(
    val name: String,
    val code: String,
)
