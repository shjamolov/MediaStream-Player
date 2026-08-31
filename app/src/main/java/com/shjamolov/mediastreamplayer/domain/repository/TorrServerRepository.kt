package com.shjamolov.mediastreamplayer.domain.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrentContent
import com.shjamolov.mediastreamplayer.domain.model.TorrentPlaybackSource
import com.shjamolov.mediastreamplayer.domain.model.TorrentSearchResult

interface TorrServerRepository {
    suspend fun testConnection(endpoint: TorrServerEndpoint): AppResult<TorrServerStatus>
    suspend fun addTorrent(endpoint: TorrServerEndpoint, link: String, title: String, poster: String?): AppResult<TorrentContent>
    fun playbackSource(endpoint: TorrServerEndpoint, content: TorrentContent, fileId: Int): TorrentPlaybackSource
    suspend fun search(endpoint: TorrServerEndpoint, query: String): AppResult<List<TorrentSearchResult>>
}

data class TorrServerStatus(val version: String, val isMatrix: Boolean)
