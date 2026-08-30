package com.shjamolov.mediastreamplayer.domain.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint

interface TorrServerRepository {
    suspend fun testConnection(endpoint: TorrServerEndpoint): AppResult<TorrServerStatus>
}

data class TorrServerStatus(val version: String, val isMatrix: Boolean)
