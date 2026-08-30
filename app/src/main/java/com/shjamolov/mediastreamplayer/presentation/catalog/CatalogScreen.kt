package com.shjamolov.mediastreamplayer.presentation.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.MediaType

private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

@Composable
fun CatalogScreen(viewModel: CatalogViewModel, searchMode: Boolean = false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.selected?.let {
        BackHandler(onBack = viewModel::closeDetails)
        DetailsScreen(
            details = it,
            loading = state.loadingDetails,
            favorite = state.favorite,
            episodes = state.episodes,
            loadingEpisodes = state.loadingEpisodes,
            onFavorite = viewModel::toggleFavorite,
            onSeason = viewModel::loadSeason,
            onRecommendation = viewModel::open,
        )
        return
    }
    Column(Modifier.fillMaxSize().padding(vertical = 28.dp)) {
        Row(Modifier.padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (searchMode) R.string.search else if (state.type == MediaType.MOVIE) R.string.movies else R.string.series),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            if (state.fromCache) Text(stringResource(R.string.offline_cache), Modifier.padding(start = 18.dp), color = Color(0xFFFFC857))
        }
        if (searchMode) SearchField(state.query, viewModel::search)
        state.error?.let { error ->
            val message = when (error) {
                CatalogError.CONFIGURATION -> R.string.tmdb_token_missing
                CatalogError.NETWORK -> R.string.catalog_network_error
                CatalogError.UNKNOWN -> R.string.catalog_unknown_error
            }
            Text(stringResource(message), Modifier.padding(48.dp), color = Color(0xFFFFA5A5))
        }
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.catalog_loading)) }
        } else if (state.items.isEmpty() && state.error == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.catalog_empty)) }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) { items(state.items, key = { "${it.type}-${it.id.value}" }) { MediaCard(it, viewModel::open) } }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
        modifier = Modifier.padding(horizontal = 48.dp, vertical = 18.dp).fillMaxWidth()
            .background(Color(0xFF17384B), RoundedCornerShape(10.dp)).padding(16.dp),
        decorationBox = { inner -> if (value.isEmpty()) Text(stringResource(R.string.search_hint), color = Color.Gray); inner() },
    )
}

@Composable
private fun MediaCard(item: CatalogItem, onClick: (CatalogItem) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(190.dp).onFocusChanged { focused = it.isFocused }
            .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0xFF294354), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)).clickable { onClick(item) }.focusable().padding(8.dp),
    ) {
        AsyncImage(
            model = item.posterPath?.let { IMAGE_BASE + it }, contentDescription = item.title,
            modifier = Modifier.fillMaxWidth().height(270.dp).background(Color(0xFF10242F)), contentScale = ContentScale.Crop,
        )
        Text(item.title, Modifier.padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text("★ ${item.voteAverage?.let { "%.1f".format(it) } ?: "—"}", color = Color(0xFFFFC857))
    }
}

@Composable
private fun DetailsScreen(
    details: CatalogDetails,
    loading: Boolean,
    favorite: Boolean,
    episodes: List<com.shjamolov.mediastreamplayer.domain.model.CatalogEpisode>,
    loadingEpisodes: Boolean,
    onFavorite: () -> Unit,
    onSeason: (Int) -> Unit,
    onRecommendation: (CatalogItem) -> Unit,
) {
    val item = details.item
    val uriHandler = LocalUriHandler.current
    var watchMessageVisible by remember(item.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxSize().padding(48.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        AsyncImage(item.posterPath?.let { IMAGE_BASE + it }, item.title, Modifier.width(250.dp).height(375.dp), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(item.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(listOfNotNull(
                item.releaseDate?.take(4),
                item.voteAverage?.let { "★ %.1f".format(it) },
                details.runtimeMinutes?.let { "$it мин" },
                details.certification?.let { "$it+" },
            ).joinToString(" • "), color = Color(0xFFFFC857))
            if (details.genres.isNotEmpty()) Text(details.genres.joinToString(" • "), Modifier.padding(top = 10.dp))
            Text(item.overview.orEmpty(), Modifier.padding(vertical = 20.dp), maxLines = 7, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { watchMessageVisible = true }) {
                    Text(stringResource(R.string.watch))
                }
                details.trailer?.let { trailer ->
                    Button(onClick = { uriHandler.openUri("https://www.youtube.com/watch?v=${trailer.key}") }) {
                        Text(stringResource(R.string.watch_trailer))
                    }
                }
                Button(onClick = onFavorite) { Text(stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite)) }
            }
            if (watchMessageVisible) {
                Text(stringResource(R.string.watch_source_required), Modifier.padding(top = 12.dp), color = Color(0xFFFFC857))
            }
            Text(stringResource(R.string.tmdb_metadata_only), Modifier.padding(top = 16.dp), color = Color(0xFF9CB3C5))
            if (loading) Text(stringResource(R.string.catalog_loading))
            if (details.seasons.isNotEmpty()) {
                Text(stringResource(R.string.seasons), Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(details.seasons, key = { it.id }) { season ->
                        Button(onClick = { onSeason(season.number) }) { Text("${season.name} • ${season.episodeCount}") }
                    }
                }
            }
            if (loadingEpisodes) Text(stringResource(R.string.episodes_loading), Modifier.padding(top = 12.dp))
            if (episodes.isNotEmpty()) {
                Text(stringResource(R.string.episodes), Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold)
                episodes.forEach { episode ->
                    Text("${episode.number}. ${episode.name}${episode.runtimeMinutes?.let { " • $it мин" }.orEmpty()}", Modifier.padding(vertical = 5.dp))
                }
            }
            if (details.cast.isNotEmpty()) {
                Text(stringResource(R.string.cast), Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold)
                Text(details.cast.joinToString(" • ") { member -> member.character?.let { "${member.name} — $it" } ?: member.name })
            }
            if (details.watchProviders.isNotEmpty()) {
                Text(stringResource(R.string.available_on), Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold)
                Text(details.watchProviders.joinToString(" • "))
            }
            if (details.imdbId != null) Text("IMDb: ${details.imdbId}", Modifier.padding(top = 12.dp), color = Color(0xFF9CB3C5))
            val suggestions = details.recommendations.ifEmpty { details.similar }
            if (suggestions.isNotEmpty()) {
                Text(stringResource(R.string.recommendations), Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.height(350.dp)) {
                    items(suggestions.take(12), key = { "recommendation-${it.type}-${it.id.value}" }) { recommendation ->
                        MediaCard(recommendation, onRecommendation)
                    }
                }
            }
        }
    }
}
