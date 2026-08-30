package com.shjamolov.mediastreamplayer.presentation.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TvCatalog
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.domain.repository.TvCatalogRepository
import com.shjamolov.mediastreamplayer.domain.repository.TvGuideRepository
import com.shjamolov.mediastreamplayer.domain.repository.AdultContentAccess
import com.shjamolov.mediastreamplayer.domain.model.TvGuideEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TvCatalogViewModel(
    private val repository: TvCatalogRepository,
    private val guideRepository: TvGuideRepository,
    private val dispatchers: DispatcherProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TvCatalogUiState>(TvCatalogUiState.Loading)
    val state: StateFlow<TvCatalogUiState> = mutableState.asStateFlow()
    private val mutableSelectedChannel = MutableStateFlow<TvChannelStreams?>(null)
    val selectedChannel: StateFlow<TvChannelStreams?> = mutableSelectedChannel.asStateFlow()
    private val mutableGuideState = MutableStateFlow<TvGuideUiState>(TvGuideUiState.Idle)
    val guideState: StateFlow<TvGuideUiState> = mutableGuideState.asStateFlow()
    private var guideJob: Job? = null

    private var catalog: TvCatalog? = null
    private var selectedFilter: TvCatalogFilter = TvCatalogFilter.All

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun setAdultContentUnlocked(unlocked: Boolean) {
        load(if (unlocked) AdultContentAccess.UNLOCKED else AdultContentAccess.BLOCKED)
    }

    fun selectFilter(filter: TvCatalogFilter) {
        selectedFilter = filter
        catalog?.let(::showCatalog)
    }

    fun openChannel(channel: TvChannelStreams) {
        mutableSelectedChannel.value = channel
        mutableGuideState.value = TvGuideUiState.Loading
        guideJob?.cancel()
        guideJob = viewModelScope.launch(dispatchers.main) {
            when (
                val result = guideRepository.getSchedule(
                    channelId = channel.channel.id,
                    feedId = channel.streams.first().feedId,
                )
            ) {
                is AppResult.Success -> {
                    val (current, next) = result.value.nowAndNext(currentTimeMillis())
                    mutableGuideState.value = if (current == null && next == null) {
                        TvGuideUiState.Unavailable
                    } else {
                        TvGuideUiState.Content(current, next)
                    }
                }
                is AppResult.Failure -> mutableGuideState.value = TvGuideUiState.Unavailable
            }
        }
    }

    fun closePlayer() {
        guideJob?.cancel()
        mutableSelectedChannel.value = null
        mutableGuideState.value = TvGuideUiState.Idle
    }

    private fun load(access: AdultContentAccess = AdultContentAccess.BLOCKED) {
        mutableState.value = TvCatalogUiState.Loading
        viewModelScope.launch(dispatchers.main) {
            when (val result = repository.getCatalog(access)) {
                is AppResult.Success -> {
                    catalog = result.value
                    selectedFilter = if (access == AdultContentAccess.UNLOCKED && result.value.channels.any { it.channel.isNsfw }) {
                        TvCatalogFilter.Adult
                    } else result.value.defaultFilter()
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
    data object Adult : TvCatalogFilter
}

enum class TvCatalogError {
    NETWORK,
    STORAGE,
    CONFIGURATION,
    UNKNOWN,
}

sealed interface TvGuideUiState {
    data object Idle : TvGuideUiState
    data object Loading : TvGuideUiState
    data class Content(
        val current: TvGuideEntry?,
        val next: TvGuideEntry?,
    ) : TvGuideUiState
    data object Unavailable : TvGuideUiState
}

internal fun List<TvChannelStreams>.filterBy(filter: TvCatalogFilter): List<TvChannelStreams> =
    when (filter) {
        TvCatalogFilter.All -> this
        is TvCatalogFilter.Country -> filter { it.channel.countryCode == filter.code }
        is TvCatalogFilter.Category -> filter { filter.id in it.channel.categoryIds }
        TvCatalogFilter.Adult -> filter { it.channel.isNsfw }
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

internal fun List<TvGuideEntry>.nowAndNext(
    epochMillis: Long,
): Pair<TvGuideEntry?, TvGuideEntry?> {
    val ordered = sortedBy(TvGuideEntry::startsAtEpochMillis)
    val current = ordered.firstOrNull { it.isAiringAt(epochMillis) }
    val next = ordered.firstOrNull {
        it.startsAtEpochMillis >= (current?.endsAtEpochMillis ?: epochMillis)
    }
    return current to next
}
