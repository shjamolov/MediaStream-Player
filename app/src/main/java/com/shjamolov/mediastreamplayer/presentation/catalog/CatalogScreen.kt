package com.shjamolov.mediastreamplayer.presentation.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.CatalogDetails
import com.shjamolov.mediastreamplayer.domain.model.CatalogItem
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.presentation.torrent.TorrentPlaybackViewModel
import com.shjamolov.mediastreamplayer.presentation.torrent.TorrentSourceScreen
import com.shjamolov.mediastreamplayer.presentation.components.AdaptiveButton
import com.shjamolov.mediastreamplayer.presentation.theme.AppAccent
import com.shjamolov.mediastreamplayer.presentation.theme.AppBackground
import com.shjamolov.mediastreamplayer.presentation.theme.AppGold
import com.shjamolov.mediastreamplayer.presentation.theme.AppSurface
import com.shjamolov.mediastreamplayer.presentation.theme.AppTextSecondary

private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    torrentViewModel: TorrentPlaybackViewModel,
    searchMode: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val torrentState by torrentViewModel.state.collectAsStateWithLifecycle()
    if (torrentState.visible) {
        TorrentSourceScreen(torrentViewModel)
        return
    }
    state.selected?.let {
        BackHandler(onBack = viewModel::closeDetails)
        DetailsScreen(
            details = it,
            loading = state.loadingDetails,
            detailsLoaded = state.detailsLoaded,
            detailsError = state.error,
            favorite = state.favorite,
            episodes = state.episodes,
            loadingEpisodes = state.loadingEpisodes,
            onFavorite = viewModel::toggleFavorite,
            onSeason = viewModel::loadSeason,
            onRecommendation = viewModel::open,
            onWatch = {
                torrentViewModel.open(
                    title = it.item.title,
                    poster = it.item.posterPath?.let { path -> IMAGE_BASE + path },
                    year = it.item.releaseDate,
                    imdbId = it.imdbId,
                )
            },
        )
        return
    }
    Column(Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState()).padding(vertical = 28.dp)) {
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
            if (!searchMode) GenreRow(state.type, state.selectedGenre, viewModel::selectGenre)
            if (!searchMode) FeaturedMedia(state.items.first(), viewModel::open)
            if (searchMode) {
                MediaRow("Результаты", state.items, viewModel::open)
            } else {
                state.shelves.forEach { shelf ->
                    MediaRow(
                        title = shelf.title,
                        media = shelf.items,
                        onClick = viewModel::open,
                        numbered = shelf.id == "top_rated",
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreRow(type: MediaType, selected: CatalogGenre?, onSelected: (CatalogGenre?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { GenreButton("Все", selected == null) { onSelected(null) } }
        items(genresFor(type), key = { it.id }) { genre ->
            GenreButton(genre.label, selected?.id == genre.id) { onSelected(genre) }
        }
    }
}

@Composable
private fun GenreButton(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.onFocusChanged { focused = it.hasFocus }
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else if (selected) Color(0xFFFF6B00) else Color(0xFF355064), shape)
            .clip(shape).background(if (selected) Color(0xFFFF6B00) else AppSurface)
            .clickable(onClick = onClick).focusable().padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MediaRow(
    title: String,
    media: List<CatalogItem>,
    onClick: (CatalogItem) -> Unit,
    numbered: Boolean = false,
) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 48.dp, top = 18.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(media.size, key = { index -> "row-$title-${media[index].type}-${media[index].id.value}" }) { index ->
            Box {
                MediaCard(media[index], onClick)
                if (numbered) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.align(Alignment.BottomStart).background(Color(0xE6FF6B00), RoundedCornerShape(topEnd = 12.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedMedia(item: CatalogItem, onClick: (CatalogItem) -> Unit) {
    Box(Modifier.fillMaxWidth().height(270.dp)) {
        AsyncImage(
            model = item.backdropPath?.let { IMAGE_BASE + it },
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(AppBackground, AppBackground.copy(alpha = 0.78f), Color.Transparent)),
            ),
        )
        Column(Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = 570.dp).padding(start = 20.dp, end = 16.dp, top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.Center) {
            Text("ВЫБОР РЕДАКЦИИ", color = Color(0xFFFF6B00), fontWeight = FontWeight.Bold)
            Text(item.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, maxLines = 2)
            Text(
                listOfNotNull(item.releaseDate?.take(4), item.voteAverage?.let { "★ %.1f".format(it) }).joinToString("  •  "),
                color = AppGold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(item.overview.orEmpty(), maxLines = 3, overflow = TextOverflow.Ellipsis, color = AppTextSecondary, modifier = Modifier.padding(vertical = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdaptiveButton(onClick = { onClick(item) }) { Text("▶ Смотреть") }
                Text("Подробнее о фильме", color = AppTextSecondary, modifier = Modifier.padding(top = 12.dp))
            }
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
            .background(AppSurface, RoundedCornerShape(14.dp)).border(1.dp, AppAccent.copy(alpha = .45f), RoundedCornerShape(14.dp)).padding(16.dp),
        decorationBox = { inner -> if (value.isEmpty()) Text(stringResource(R.string.search_hint), color = Color.Gray); inner() },
    )
}

@Composable
private fun MediaCard(item: CatalogItem, onClick: (CatalogItem) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(190.dp).onFocusChanged { focused = it.isFocused }
            .border(if (focused) 3.dp else 1.dp, if (focused) AppAccent else Color(0xFF294354), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)).background(AppSurface).clickable { onClick(item) }.focusable().padding(8.dp),
    ) {
        AsyncImage(
            model = item.posterPath?.let { IMAGE_BASE + it }, contentDescription = item.title,
            modifier = Modifier.fillMaxWidth().height(270.dp).background(Color(0xFF10242F)), contentScale = ContentScale.Crop,
        )
        Text(item.title, Modifier.padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text("★ ${item.voteAverage?.let { "%.1f".format(it) } ?: "—"}", color = AppGold)
    }
}

@Composable
private fun DetailsScreen(
    details: CatalogDetails,
    loading: Boolean,
    detailsLoaded: Boolean,
    detailsError: CatalogError?,
    favorite: Boolean,
    episodes: List<com.shjamolov.mediastreamplayer.domain.model.CatalogEpisode>,
    loadingEpisodes: Boolean,
    onFavorite: () -> Unit,
    onSeason: (Int) -> Unit,
    onRecommendation: (CatalogItem) -> Unit,
    onWatch: () -> Unit,
) {
    val item = details.item
    var trailerMessageVisible by remember(item.id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        AsyncImage(item.backdropPath?.let { IMAGE_BASE + it }, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(AppBackground, AppBackground.copy(.94f), AppBackground.copy(.60f)))))
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        val poster: @Composable () -> Unit = {
            AsyncImage(
                item.posterPath?.let { IMAGE_BASE + it },
                item.title,
                Modifier.width(if (compact) 190.dp else 250.dp).height(if (compact) 285.dp else 375.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        if (compact) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                poster()
                DetailsInfo(details, loading, detailsLoaded, detailsError, favorite, episodes, loadingEpisodes, onFavorite, onSeason, onRecommendation, onWatch, trailerMessageVisible) {
                    trailerMessageVisible = true
                }
            }
        } else {
            Row(Modifier.fillMaxSize().padding(48.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                poster()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DetailsInfo(details, loading, detailsLoaded, detailsError, favorite, episodes, loadingEpisodes, onFavorite, onSeason, onRecommendation, onWatch, trailerMessageVisible) {
                        trailerMessageVisible = true
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun DetailsInfo(
    details: CatalogDetails,
    loading: Boolean,
    detailsLoaded: Boolean,
    detailsError: CatalogError?,
    favorite: Boolean,
    episodes: List<com.shjamolov.mediastreamplayer.domain.model.CatalogEpisode>,
    loadingEpisodes: Boolean,
    onFavorite: () -> Unit,
    onSeason: (Int) -> Unit,
    onRecommendation: (CatalogItem) -> Unit,
    onWatch: () -> Unit,
    trailerMessageVisible: Boolean,
    onTrailerUnavailable: () -> Unit,
) {
    val item = details.item
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxWidth()) {
            Text(item.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(listOfNotNull(
                item.releaseDate?.take(4),
                item.voteAverage?.let { "★ %.1f".format(it) },
                details.runtimeMinutes?.let { "$it мин" },
                details.certification?.let { "$it+" },
            ).joinToString(" • "), color = Color(0xFFFFC857))
            if (details.genres.isNotEmpty()) Text(details.genres.joinToString(" • "), Modifier.padding(top = 10.dp))
            Text(item.overview.orEmpty(), Modifier.padding(vertical = 20.dp), maxLines = 7, overflow = TextOverflow.Ellipsis)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { AdaptiveButton(onClick = onWatch) { Text(stringResource(R.string.watch)) } }
                item { AdaptiveButton(onClick = {
                    val opened = details.trailer?.let { trailer ->
                        runCatching { uriHandler.openUri("https://www.youtube.com/watch?v=${trailer.key}") }.isSuccess
                    } ?: false
                    if (!opened) onTrailerUnavailable()
                }) {
                    Text(stringResource(R.string.watch_trailer))
                } }
                item { AdaptiveButton(onClick = onFavorite, selected = favorite) { Text(stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite)) } }
            }
            if (trailerMessageVisible) {
                Text(stringResource(R.string.trailer_unavailable), Modifier.padding(top = 12.dp), color = Color(0xFFFFC857))
            }
            Text(stringResource(R.string.tmdb_metadata_only), Modifier.padding(top = 16.dp), color = Color(0xFF9CB3C5))
            if (loading) Text(stringResource(R.string.catalog_loading))
            if (detailsLoaded) Text(stringResource(R.string.details_loaded), Modifier.padding(top = 10.dp), color = Color(0xFF76E39A))
            if (!loading && !detailsLoaded && detailsError != null) {
                Text(stringResource(R.string.details_load_failed), Modifier.padding(top = 10.dp), color = Color(0xFFFFA5A5))
            }
            if (details.seasons.isNotEmpty()) {
                Text(stringResource(R.string.seasons), Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(details.seasons, key = { it.id }) { season ->
                        AdaptiveButton(onClick = { onSeason(season.number) }) { Text("${season.name} • ${season.episodeCount}") }
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
