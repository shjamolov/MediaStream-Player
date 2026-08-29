package com.shjamolov.mediastreamplayer.domain.model

data class CatalogDetails(
    val item: CatalogItem,
    val genres: List<String> = emptyList(),
    val seasons: List<CatalogSeason> = emptyList(),
)

data class CatalogSeason(
    val id: Long,
    val number: Int,
    val name: String,
    val episodeCount: Int,
)
