package com.shjamolov.mediastreamplayer.presentation.torrent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.core.settings.TorrServerSettingsStore
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrentContent
import com.shjamolov.mediastreamplayer.domain.model.TorrentPlaybackSource
import com.shjamolov.mediastreamplayer.domain.model.TorrentVideoFile
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TorrentPlaybackViewModel(
    private val repository: TorrServerRepository,
    private val settings: TorrServerSettingsStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TorrentSourceUiState())
    val state: StateFlow<TorrentSourceUiState> = mutableState.asStateFlow()
    private val mutablePlayback = MutableStateFlow<TorrentPlaybackSource?>(null)
    val playback: StateFlow<TorrentPlaybackSource?> = mutablePlayback.asStateFlow()

    fun open(title: String, poster: String?) = mutableState.update {
        TorrentSourceUiState(visible = true, title = title, poster = poster)
    }

    fun close() = mutableState.update { TorrentSourceUiState() }
    fun setLink(value: String) = mutableState.update { it.copy(link = value, error = null) }

    fun load() {
        val current = mutableState.value
        val link = current.link.trim()
        if (!isSupportedLink(link)) {
            mutableState.update { it.copy(error = TorrentSourceError.INVALID_LINK) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, content = null) }
        viewModelScope.launch {
            when (val result = repository.addTorrent(settings.load(), link, current.title, current.poster)) {
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
    val link: String = "",
    val loading: Boolean = false,
    val content: TorrentContent? = null,
    val error: TorrentSourceError? = null,
)

enum class TorrentSourceError { INVALID_LINK, LOAD_FAILED }
