package com.shjamolov.mediastreamplayer.domain.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TvCatalog

enum class AdultContentAccess {
    BLOCKED,
    UNLOCKED,
}

interface TvCatalogRepository {
    suspend fun getCatalog(
        adultContentAccess: AdultContentAccess = AdultContentAccess.BLOCKED,
    ): AppResult<TvCatalog>
}
