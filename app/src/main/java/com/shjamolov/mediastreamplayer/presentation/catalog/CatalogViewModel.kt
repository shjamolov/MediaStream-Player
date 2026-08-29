package com.shjamolov.mediastreamplayer.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.domain.repository.CatalogRepository
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
        _state.update { it.copy(type = type, loading = true, error = null, selected = null) }
        viewModelScope.launch {
            when (val result = repository.popular(type)) {
                is AppResult.Success -> _state.update {
                    it.copy(items = result.value.items, loading = false, fromCache = result.value.fromCache)
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
                    it.copy(items = result.value.items, loading = false, fromCache = false)
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
        _state.update { it.copy(loadingDetails = true, selected = CatalogDetails(item), favorite = false, error = null) }
        viewModelScope.launch {
            when (val result = repository.details(item.id, item.type)) {
                is AppResult.Success -> _state.update { it.copy(selected = result.value, loadingDetails = false) }
                is AppResult.Failure -> _state.update { it.copy(loadingDetails = false, error = result.error.toUiError()) }
            }
        }
    }

    fun closeDetails() {
        favoriteJob?.cancel()
        _state.update { it.copy(selected = null, favorite = false, error = null) }
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
    val favorite: Boolean = false,
    val error: CatalogError? = null,
)

enum class CatalogError { CONFIGURATION, NETWORK, UNKNOWN }

private fun AppError.toUiError() = when (this) {
    is AppError.Configuration -> CatalogError.CONFIGURATION
    is AppError.Network -> CatalogError.NETWORK
    else -> CatalogError.UNKNOWN
}
