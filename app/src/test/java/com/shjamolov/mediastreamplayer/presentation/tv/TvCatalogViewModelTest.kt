package com.shjamolov.mediastreamplayer.presentation.tv

import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import com.shjamolov.mediastreamplayer.domain.model.TvCatalog
import com.shjamolov.mediastreamplayer.domain.model.TvCategory
import com.shjamolov.mediastreamplayer.domain.model.TvChannel
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.domain.model.TvCountry
import com.shjamolov.mediastreamplayer.domain.model.TvStream
import com.shjamolov.mediastreamplayer.domain.repository.AdultContentAccess
import com.shjamolov.mediastreamplayer.domain.repository.TvCatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvCatalogViewModelTest {
    @Test
    fun successfulLoad_selectsUzbekistanByDefault() = runTest {
        val viewModel = createViewModel(AppResult.Success(catalog()), testScheduler)

        advanceUntilIdle()

        val state = viewModel.state.value as TvCatalogUiState.Content
        assertEquals(TvCatalogFilter.Country("UZ"), state.selectedFilter)
        assertEquals(listOf("uz-channel"), state.visibleChannels.map { it.channel.id.value })
    }

    @Test
    fun selectingCategory_filtersExistingCatalogWithoutReloading() = runTest {
        val viewModel = createViewModel(AppResult.Success(catalog()), testScheduler)
        advanceUntilIdle()

        viewModel.selectFilter(TvCatalogFilter.Category("news"))

        val state = viewModel.state.value as TvCatalogUiState.Content
        assertEquals(listOf("uz-channel"), state.visibleChannels.map { it.channel.id.value })
    }

    @Test
    fun networkFailure_exposesRetryableNetworkState() = runTest {
        val viewModel = createViewModel(
            AppResult.Failure(AppError.Network()),
            testScheduler,
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is TvCatalogUiState.Error)
        assertEquals(TvCatalogError.NETWORK, (state as TvCatalogUiState.Error).type)
    }

    @Test
    fun openAndCloseChannel_controlsPlayerNavigationState() = runTest {
        val catalog = catalog()
        val viewModel = createViewModel(AppResult.Success(catalog), testScheduler)
        advanceUntilIdle()

        viewModel.openChannel(catalog.channels.first())
        assertEquals(catalog.channels.first(), viewModel.selectedChannel.value)

        viewModel.closePlayer()
        assertEquals(null, viewModel.selectedChannel.value)
    }

    private fun createViewModel(
        result: AppResult<TvCatalog>,
        scheduler: TestCoroutineScheduler,
    ) = TvCatalogViewModel(
        repository = object : TvCatalogRepository {
            override suspend fun getCatalog(adultContentAccess: AdultContentAccess) = result
        },
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(scheduler)),
    )

    private fun catalog() = TvCatalog(
        channels = listOf(
            channel("uz-channel", "UZ", setOf("news")),
            channel("us-channel", "US", setOf("sports")),
        ),
        categories = listOf(
            TvCategory("news", "News", "News"),
            TvCategory("sports", "Sports", "Sports"),
        ),
        countries = listOf(
            TvCountry("UZ", "Uzbekistan", setOf("uzb"), "🇺🇿"),
            TvCountry("US", "United States", setOf("eng"), "🇺🇸"),
        ),
        languages = emptyList(),
    )

    private fun channel(id: String, country: String, categories: Set<String>): TvChannelStreams {
        val channelId = ChannelId(id)
        return TvChannelStreams(
            channel = TvChannel(
                id = channelId,
                name = id,
                countryCode = country,
                categoryIds = categories,
            ),
            streams = listOf(TvStream(channelId, "https://example.com/$id.m3u8")),
        )
    }
}

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}
