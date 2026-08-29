package com.shjamolov.mediastreamplayer.presentation.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TvCatalog
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.domain.repository.TvCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TvCatalogViewModel(
    private val repository: TvCatalogRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TvCatalogUiState>(TvCatalogUiState.Loading)
    val state: StateFlow<TvCatalogUiState> = mutableState.asStateFlow()

    private var catalog: TvCatalog? = null
    private var selectedFilter: TvCatalogFilter = TvCatalogFilter.All

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun selectFilter(filter: TvCatalogFilter) {
        selectedFilter = filter
        catalog?.let(::showCatalog)
    }

    private fun load() {
        mutableState.value = TvCatalogUiState.Loading
        viewModelScope.launch(dispatchers.main) {
            when (val result = repository.getCatalog()) {
                is AppResult.Success -> {
                    catalog = result.value
                    selectedFilter = result.value.defaultFilter()
                    showCatalog(result.value)
                }

                is AppResult.Failure -> {
                    mutableState.value = TvCatalogUiState.Error(result.error.toUiError())
                }
            }
        }
    }

    private fun showCatalog(catalog: TvCatalog) {
        mutableState.value = TvCatalogUiState.Content(
            catalog = catalog,
            selectedFilter = selectedFilter,
            visibleChannels = catalog.channels.filterBy(selectedFilter),
        )
    }
}

sealed interface TvCatalogUiState {
    data object Loading : TvCatalogUiState

    data class Content(
        val catalog: TvCatalog,
        val selectedFilter: TvCatalogFilter,
        val visibleChannels: List<TvChannelStreams>,
    ) : TvCatalogUiState

    data class Error(val type: TvCatalogError) : TvCatalogUiState
}

sealed interface TvCatalogFilter {
    data object All : TvCatalogFilter
    data class Country(val code: String) : TvCatalogFilter
    data class Category(val id: String) : TvCatalogFilter
}

enum class TvCatalogError {
    NETWORK,
    STORAGE,
    CONFIGURATION,
    UNKNOWN,
}

internal fun List<TvChannelStreams>.filterBy(filter: TvCatalogFilter): List<TvChannelStreams> =
    when (filter) {
        TvCatalogFilter.All -> this
        is TvCatalogFilter.Country -> filter { it.channel.countryCode == filter.code }
        is TvCatalogFilter.Category -> filter { filter.id in it.channel.categoryIds }
    }

private fun TvCatalog.defaultFilter(): TvCatalogFilter =
    if (channels.any { it.channel.countryCode == DEFAULT_COUNTRY }) {
        TvCatalogFilter.Country(DEFAULT_COUNTRY)
    } else {
        TvCatalogFilter.All
    }

private fun AppError.toUiError(): TvCatalogError = when (this) {
    is AppError.Network -> TvCatalogError.NETWORK
    is AppError.Storage -> TvCatalogError.STORAGE
    is AppError.Configuration -> TvCatalogError.CONFIGURATION
    is AppError.Unexpected -> TvCatalogError.UNKNOWN
}

private const val DEFAULT_COUNTRY = "UZ"
