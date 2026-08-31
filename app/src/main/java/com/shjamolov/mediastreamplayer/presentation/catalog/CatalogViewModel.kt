package com.shjamolov.mediastreamplayer.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.domain.model.CatalogEpisode
import com.shjamolov.mediastreamplayer.domain.repository.CatalogRepository
import com.shjamolov.mediastreamplayer.domain.repository.CatalogShelf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var favoriteJob: Job? = null

    init { load(MediaType.MOVIE) }

    fun load(type: MediaType) {
        _state.update { it.copy(type = type, loading = true, error = null, selected = null, selectedGenre = null) }
        viewModelScope.launch {
            when (val result = repository.home(type)) {
                is AppResult.Success -> _state.update {
                    val items = result.value.flatMap(CatalogShelf::items).distinctBy { item -> item.id }
                    it.copy(items = items, shelves = result.value, loading = false, fromCache = result.value.any { shelf -> shelf.id == "offline" })
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error.toUiError()) }
            }
        }
    }

    fun selectGenre(genre: CatalogGenre?) {
        val type = _state.value.type
        _state.update { it.copy(selectedGenre = genre, loading = true, error = null, selected = null) }
        viewModelScope.launch {
            val result = if (genre == null) repository.popular(type) else repository.discover(type, genre.id)
            when (result) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        items = result.value.items,
                        shelves = listOf(CatalogShelf("genre", genre?.label ?: "Популярное", result.value.items)),
                        loading = false,
                        fromCache = result.value.fromCache,
                    )
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error.toUiError()) }
            }
        }
    }

    fun search(query: String) {
        _state.update { it.copy(query = query, selected = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) return
        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.search(query)) {
                is AppResult.Success -> _state.update {
                    it.copy(items = result.value.items, shelves = emptyList(), loading = false, fromCache = false)
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error.toUiError()) }
            }
        }
    }

    fun open(item: CatalogItem) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            repository.observeIsFavorite(item.id, item.type).collect { favorite ->
                _state.update { it.copy(favorite = favorite) }
            }
        }
        _state.update {
            it.copy(
                loadingDetails = true,
                detailsLoaded = false,
                selected = CatalogDetails(item),
                favorite = false,
                error = null,
                selectedSeason = null,
                episodes = emptyList(),
                loadingEpisodes = false,
            )
        }
        viewModelScope.launch {
            when (val result = repository.details(item.id, item.type)) {
                is AppResult.Success -> _state.update { it.copy(selected = result.value, loadingDetails = false, detailsLoaded = true) }
                is AppResult.Failure -> _state.update { it.copy(loadingDetails = false, error = result.error.toUiError()) }
            }
        }
    }

    fun closeDetails() {
        favoriteJob?.cancel()
        _state.update { it.copy(selected = null, favorite = false, error = null) }
    }

    fun loadSeason(seasonNumber: Int) {
        val details = _state.value.selected ?: return
        _state.update { it.copy(loadingEpisodes = true, episodes = emptyList(), selectedSeason = seasonNumber) }
        viewModelScope.launch {
            when (val result = repository.seasonEpisodes(details.item.id, seasonNumber)) {
                is AppResult.Success -> _state.update { it.copy(loadingEpisodes = false, episodes = result.value) }
                is AppResult.Failure -> _state.update { it.copy(loadingEpisodes = false, error = result.error.toUiError()) }
            }
        }
    }

    fun toggleFavorite() {
        val item = _state.value.selected?.item ?: return
        viewModelScope.launch {
            val next = !_state.value.favorite
            repository.setFavorite(item, next)
            _state.update { it.copy(favorite = next) }
        }
    }
}

data class CatalogUiState(
    val type: MediaType = MediaType.MOVIE,
    val items: List<CatalogItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = false,
    val fromCache: Boolean = false,
    val selected: CatalogDetails? = null,
    val loadingDetails: Boolean = false,
    val detailsLoaded: Boolean = false,
    val favorite: Boolean = false,
    val error: CatalogError? = null,
    val selectedSeason: Int? = null,
    val episodes: List<CatalogEpisode> = emptyList(),
    val loadingEpisodes: Boolean = false,
    val selectedGenre: CatalogGenre? = null,
    val shelves: List<CatalogShelf> = emptyList(),
)

data class CatalogGenre(val id: Int, val label: String)

internal fun genresFor(type: MediaType): List<CatalogGenre> = when (type) {
    MediaType.MOVIE -> listOf(
        CatalogGenre(28, "Боевики"), CatalogGenre(35, "Комедии"), CatalogGenre(18, "Драмы"),
        CatalogGenre(80, "Криминал"), CatalogGenre(14, "Фэнтези"), CatalogGenre(878, "Фантастика"),
        CatalogGenre(27, "Ужасы"), CatalogGenre(10751, "Семейные"), CatalogGenre(16, "Анимация"),
        CatalogGenre(99, "Документальные"),
    )
    MediaType.SERIES -> listOf(
        CatalogGenre(10759, "Приключения"), CatalogGenre(35, "Комедии"), CatalogGenre(18, "Драмы"),
        CatalogGenre(80, "Криминал"), CatalogGenre(10765, "Фантастика"), CatalogGenre(9648, "Детективы"),
        CatalogGenre(10751, "Семейные"), CatalogGenre(10762, "Детские"), CatalogGenre(16, "Анимация"),
        CatalogGenre(99, "Документальные"),
    )
}

enum class CatalogError { CONFIGURATION, NETWORK, UNKNOWN }

private fun AppError.toUiError() = when (this) {
    is AppError.Configuration -> CatalogError.CONFIGURATION
    is AppError.Network -> CatalogError.NETWORK
    else -> CatalogError.UNKNOWN
}
