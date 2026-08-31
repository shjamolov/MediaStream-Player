package com.shjamolov.mediastreamplayer.presentation.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.TvCatalog
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.presentation.theme.AppAccent
import com.shjamolov.mediastreamplayer.presentation.theme.AppBackground
import com.shjamolov.mediastreamplayer.presentation.theme.AppSurface
import com.shjamolov.mediastreamplayer.presentation.theme.AppSurfaceRaised
import com.shjamolov.mediastreamplayer.presentation.theme.AppTextSecondary

@Composable
fun TvCatalogScreen(
    viewModel: TvCatalogViewModel,
    onChannelSelected: (TvChannelStreams) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        when (val currentState = state) {
            TvCatalogUiState.Loading -> LoadingState()
            is TvCatalogUiState.Error -> ErrorState(currentState.type, viewModel::retry)
            is TvCatalogUiState.Content -> CatalogContent(
                state = currentState,
                onFilterSelected = viewModel::selectFilter,
                onChannelSelected = onChannelSelected,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.tv_catalog_loading))
    }
}

@Composable
private fun ErrorState(error: TvCatalogError, onRetry: () -> Unit) {
    val message = when (error) {
        TvCatalogError.NETWORK -> stringResource(R.string.tv_catalog_network_error)
        TvCatalogError.STORAGE -> stringResource(R.string.tv_catalog_storage_error)
        TvCatalogError.CONFIGURATION -> stringResource(R.string.tv_catalog_configuration_error)
        TvCatalogError.UNKNOWN -> stringResource(R.string.tv_catalog_unknown_error)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun CatalogContent(
    state: TvCatalogUiState.Content,
    onFilterSelected: (TvCatalogFilter) -> Unit,
    onChannelSelected: (TvChannelStreams) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        CatalogHeader(channelCount = state.visibleChannels.size)
        FilterRows(
            catalog = state.catalog,
            selectedFilter = state.selectedFilter,
            onFilterSelected = onFilterSelected,
        )
        if (state.visibleChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.tv_catalog_empty))
            }
        } else {
            ChannelGrid(state.visibleChannels, onChannelSelected)
        }
    }
}

@Composable
private fun CatalogHeader(channelCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 42.dp, top = 28.dp, end = 42.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.live_tv),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = pluralStringResource(R.plurals.channel_count, channelCount, channelCount),
            color = AppTextSecondary,
        )
    }
}

@Composable
private fun FilterRows(
    catalog: TvCatalog,
    selectedFilter: TvCatalogFilter,
    onFilterSelected: (TvCatalogFilter) -> Unit,
) {
    val availableCountries = remember(catalog) {
        val codes = catalog.channels.mapNotNull { it.channel.countryCode }.toSet()
        catalog.countries
            .filter { it.code in codes }
            .sortedBy { SUPPORTED_COUNTRY_ORDER.indexOf(it.code) }
    }
    val availableCategories = remember(catalog) {
        val ids = catalog.channels.flatMap { it.channel.categoryIds }.toSet()
        catalog.categories.filter { it.id in ids }.sortedBy { it.name }
    }

    Text(
        text = stringResource(R.string.countries),
        modifier = Modifier.padding(horizontal = 42.dp),
        style = MaterialTheme.typography.titleMedium,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 42.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "all") {
            FilterButton(
                label = stringResource(R.string.all_channels),
                selected = selectedFilter == TvCatalogFilter.All,
                onClick = { onFilterSelected(TvCatalogFilter.All) },
            )
        }
        items(availableCountries, key = { it.code }) { country ->
            val filter = TvCatalogFilter.Country(country.code)
            FilterButton(
                label = "${country.flag} ${country.name}",
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }

    Text(
        text = stringResource(R.string.categories),
        modifier = Modifier.padding(horizontal = 42.dp),
        style = MaterialTheme.typography.titleMedium,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 42.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(availableCategories, key = { it.id }) { category ->
            val filter = TvCatalogFilter.Category(category.id)
            FilterButton(
                label = category.name,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier.onFocusChanged { focused = it.hasFocus }
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else if (selected) AppAccent else Color(0xFF294354), shape)
            .clip(shape).background(if (selected) AppAccent else AppSurface)
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(text = label, color = if (selected) Color(0xFF00131B) else Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChannelGrid(
    channels: List<TvChannelStreams>,
    onChannelSelected: (TvChannelStreams) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 42.dp, top = 12.dp, end = 42.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(channels, key = { it.channel.id.value }) { channel ->
            ChannelCard(channel, onChannelSelected)
        }
    }
}

@Composable
private fun ChannelCard(
    item: TvChannelStreams,
    onChannelSelected: (TvChannelStreams) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) AppAccent else Color(0xFF294354),
                shape = shape,
            )
            .clip(shape)
            .background(if (focused) AppSurfaceRaised else AppSurface)
            .clickable { onChannelSelected(item) }
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            contentAlignment = Alignment.Center,
        ) {
            val logo = item.channel.logoUrl
            if (logo == null) {
                Text(
                    text = item.channel.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                AsyncImage(
                    model = logo,
                    contentDescription = item.channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = item.channel.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = streamSummary(item),
            maxLines = 1,
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun streamSummary(item: TvChannelStreams): String {
    val quality = item.streams.firstNotNullOfOrNull { it.quality } ?: "AUTO"
    return "$quality • ${item.streams.size} stream${if (item.streams.size == 1) "" else "s"}"
}

private val SUPPORTED_COUNTRY_ORDER = listOf("UZ", "RU", "KZ")
