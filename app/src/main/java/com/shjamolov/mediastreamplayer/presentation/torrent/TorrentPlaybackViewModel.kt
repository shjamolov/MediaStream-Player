package com.shjamolov.mediastreamplayer.presentation.torrent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.core.settings.TorrServerSettingsStore
import com.shjamolov.mediastreamplayer.core.torrserver.LocalTorrServerManager
import com.shjamolov.mediastreamplayer.core.torrserver.LocalTorrServerState
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrentContent
import com.shjamolov.mediastreamplayer.domain.model.TorrentPlaybackSource
import com.shjamolov.mediastreamplayer.domain.model.TorrentVideoFile
import com.shjamolov.mediastreamplayer.domain.model.TorrentSearchResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TorrentPlaybackViewModel(
    private val repository: TorrServerRepository,
    private val settings: TorrServerSettingsStore,
    private val localManager: LocalTorrServerManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TorrentSourceUiState())
    val state: StateFlow<TorrentSourceUiState> = mutableState.asStateFlow()
    private val mutablePlayback = MutableStateFlow<TorrentPlaybackSource?>(null)
    val playback: StateFlow<TorrentPlaybackSource?> = mutablePlayback.asStateFlow()

    fun open(title: String, poster: String?, year: String?, imdbId: String?) {
        val query = listOfNotNull(title, year?.take(4)).joinToString(" ")
        mutableState.value = TorrentSourceUiState(
            visible = true,
            title = title,
            poster = poster,
            query = query,
            imdbId = imdbId,
            searching = true,
        )
        search(query)
    }

    fun close() = mutableState.update { TorrentSourceUiState() }
    fun setLink(value: String) = mutableState.update { it.copy(link = value, error = null) }
    fun setQuery(value: String) = mutableState.update { it.copy(query = value, searchError = false) }
    fun search() = search(mutableState.value.query)

    private fun search(query: String) {
        if (query.isBlank()) return
        mutableState.update { it.copy(searching = true, searchError = false, searchResults = emptyList()) }
        viewModelScope.launch {
            val endpoint = settings.load()
            if (endpoint.mode == TorrServerMode.LOCAL_MANAGED && localManager.ensureRunning() !is LocalTorrServerState.Running) {
                mutableState.update { it.copy(searching = false, searchError = true) }
                return@launch
            }
            when (val result = repository.search(endpoint, query.trim())) {
                is AppResult.Success -> {
                    val imdbId = mutableState.value.imdbId
                    val results = if (result.value.isEmpty() && !imdbId.isNullOrBlank()) {
                        when (val imdbResult = repository.search(endpoint, imdbId)) {
                            is AppResult.Success -> imdbResult.value
                            is AppResult.Failure -> emptyList()
                        }
                    } else result.value
                    mutableState.update { it.copy(searching = false, searchResults = results) }
                }
                is AppResult.Failure -> mutableState.update { it.copy(searching = false, searchError = true) }
            }
        }
    }

    fun selectResult(result: TorrentSearchResult) {
        mutableState.update { it.copy(link = result.magnetOrLink) }
        load()
    }

    fun load() {
        val current = mutableState.value
        val link = current.link.trim()
        if (!isSupportedLink(link)) {
            mutableState.update { it.copy(error = TorrentSourceError.INVALID_LINK) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, content = null) }
        viewModelScope.launch {
            val endpoint = settings.load()
            if (endpoint.mode == TorrServerMode.LOCAL_MANAGED && localManager.ensureRunning() !is LocalTorrServerState.Running) {
                mutableState.update { it.copy(loading = false, error = TorrentSourceError.LOAD_FAILED) }
                return@launch
            }
            when (val result = repository.addTorrent(endpoint, link, current.title, current.poster)) {
                is AppResult.Success -> mutableState.update { it.copy(loading = false, content = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = TorrentSourceError.LOAD_FAILED) }
            }
        }
    }

    fun play(file: TorrentVideoFile) {
        val content = mutableState.value.content ?: return
        mutablePlayback.value = repository.playbackSource(settings.load(), content, file.id)
    }

    fun closePlayer() { mutablePlayback.value = null }

    private fun isSupportedLink(value: String): Boolean =
        value.startsWith("magnet:?", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}

data class TorrentSourceUiState(
    val visible: Boolean = false,
    val title: String = "",
    val poster: String? = null,
    val query: String = "",
    val imdbId: String? = null,
    val searching: Boolean = false,
    val searchResults: List<TorrentSearchResult> = emptyList(),
    val searchError: Boolean = false,
    val link: String = "",
    val loading: Boolean = false,
    val content: TorrentContent? = null,
    val error: TorrentSourceError? = null,
)

enum class TorrentSourceError { INVALID_LINK, LOAD_FAILED }
