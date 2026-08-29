package com.shjamolov.mediastreamplayer.domain.model

data class TvCatalog(
    val channels: List<TvChannelStreams>,
    val categories: List<TvCategory>,
    val countries: List<TvCountry>,
    val languages: List<TvLanguage>,
)

data class TvCategory(
    val id: String,
    val name: String,
    val description: String,
)

data class TvCountry(
    val code: String,
    val name: String,
    val languageCodes: Set<String>,
    val flag: String,
)

data class TvLanguage(
    val code: String,
    val name: String,
)
