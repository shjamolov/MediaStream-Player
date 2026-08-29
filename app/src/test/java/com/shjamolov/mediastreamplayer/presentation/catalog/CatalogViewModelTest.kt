package com.shjamolov.mediastreamplayer.presentation.catalog

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.domain.model.TmdbId
import com.shjamolov.mediastreamplayer.domain.repository.CatalogPage
import com.shjamolov.mediastreamplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val movie = CatalogItem(TmdbId(1), MediaType.MOVIE, "Movie")

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun load_exposesPopularMovies() = runTest(dispatcher) {
        val viewModel = CatalogViewModel(FakeCatalogRepository(movie))
        advanceUntilIdle()
        assertEquals(listOf(movie), viewModel.state.value.items)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun open_loadsDetails() = runTest(dispatcher) {
        val viewModel = CatalogViewModel(FakeCatalogRepository(movie))
        advanceUntilIdle()
        viewModel.open(movie)
        advanceUntilIdle()
        assertEquals(movie, viewModel.state.value.selected?.item)
    }
}

private class FakeCatalogRepository(private val item: CatalogItem) : CatalogRepository {
    private val favorite = MutableStateFlow(false)
    override suspend fun popular(type: MediaType) = AppResult.Success(CatalogPage(listOf(item), false))
    override suspend fun search(query: String) = AppResult.Success(CatalogPage(listOf(item), false))
    override suspend fun details(id: TmdbId, type: MediaType) = AppResult.Success(CatalogDetails(item))
    override fun observeFavorites(): Flow<List<CatalogItem>> = MutableStateFlow(emptyList())
    override fun observeIsFavorite(id: TmdbId, type: MediaType): Flow<Boolean> = favorite
    override suspend fun setFavorite(item: CatalogItem, favorite: Boolean) { this.favorite.value = favorite }
}
