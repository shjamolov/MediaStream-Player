package com.shjamolov.mediastreamplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.presentation.catalog.CatalogScreen
import com.shjamolov.mediastreamplayer.presentation.catalog.CatalogViewModel
import com.shjamolov.mediastreamplayer.presentation.player.TorrentPlayerScreen
import com.shjamolov.mediastreamplayer.presentation.player.TvPlayerScreen
import com.shjamolov.mediastreamplayer.presentation.security.ParentalControlViewModel
import com.shjamolov.mediastreamplayer.presentation.settings.SettingsScreen
import com.shjamolov.mediastreamplayer.presentation.settings.SettingsViewModel
import com.shjamolov.mediastreamplayer.presentation.theme.AppAccent
import com.shjamolov.mediastreamplayer.presentation.theme.AppBackground
import com.shjamolov.mediastreamplayer.presentation.theme.AppSurface
import com.shjamolov.mediastreamplayer.presentation.theme.AppTextSecondary
import com.shjamolov.mediastreamplayer.presentation.theme.MediaStreamTheme
import com.shjamolov.mediastreamplayer.presentation.torrent.TorrentPlaybackViewModel
import com.shjamolov.mediastreamplayer.presentation.torrserver.TorrServerViewModel
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogScreen
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val tvCatalogViewModel: TvCatalogViewModel by viewModel()
    private val catalogViewModel: CatalogViewModel by viewModel()
    private val parentalControlViewModel: ParentalControlViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val torrServerViewModel: TorrServerViewModel by viewModel()
    private val torrentPlaybackViewModel: TorrentPlaybackViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaStreamTheme {
                val selectedChannel by tvCatalogViewModel.selectedChannel.collectAsStateWithLifecycle()
                val guideState by tvCatalogViewModel.guideState.collectAsStateWithLifecycle()
                val parentalState by parentalControlViewModel.state.collectAsStateWithLifecycle()
                val torrentPlayback by torrentPlaybackViewModel.playback.collectAsStateWithLifecycle()
                var section by remember { mutableStateOf(AppSection.TV) }

                LaunchedEffect(parentalState.unlocked) {
                    tvCatalogViewModel.setAdultContentUnlocked(parentalState.unlocked)
                }

                when {
                    torrentPlayback != null -> TorrentPlayerScreen(checkNotNull(torrentPlayback), torrentPlaybackViewModel::closePlayer)
                    selectedChannel != null -> TvPlayerScreen(checkNotNull(selectedChannel), guideState, tvCatalogViewModel::closePlayer)
                    else -> AppShell(section, onSectionSelected = { selected ->
                        section = selected
                        when (selected) {
                            AppSection.MOVIES -> catalogViewModel.load(MediaType.MOVIE)
                            AppSection.SERIES -> catalogViewModel.load(MediaType.SERIES)
                            else -> Unit
                        }
                    }) {
                        when (section) {
                            AppSection.TV -> TvCatalogScreen(tvCatalogViewModel, tvCatalogViewModel::openChannel)
                            AppSection.MOVIES, AppSection.SERIES -> CatalogScreen(catalogViewModel, torrentPlaybackViewModel)
                            AppSection.SEARCH -> CatalogScreen(catalogViewModel, torrentPlaybackViewModel, searchMode = true)
                            AppSection.SETTINGS -> SettingsScreen(settingsViewModel, parentalControlViewModel, torrServerViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShell(section: AppSection, onSectionSelected: (AppSection) -> Unit, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Row {
            AppNavigation(section, onSectionSelected)
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    }
}

@Composable
private fun AppNavigation(section: AppSection, onSectionSelected: (AppSection) -> Unit) {
    Column(
        modifier = Modifier.width(118.dp).fillMaxHeight().background(AppSurface).padding(vertical = 22.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(AppAccent), contentAlignment = Alignment.Center) {
            Text("MS", color = Color(0xFF00131B), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppSection.entries.forEach { item -> NavigationItem(item, section == item) { onSectionSelected(item) } }
        }
        Spacer(Modifier.weight(1f))
        Text("v0.1", color = AppTextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NavigationItem(item: AppSection, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier.width(94.dp).onFocusChanged { focused = it.hasFocus }
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
            .clip(shape).background(if (selected) AppAccent else Color.Transparent)
            .clickable(onClick = onClick).focusable().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(item.symbol, color = if (selected) Color(0xFF00131B) else Color.White, fontWeight = FontWeight.Bold)
        Text(item.label, color = if (selected) Color(0xFF00131B) else AppTextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

private enum class AppSection(val symbol: String, val label: String) {
    TV("TV", "Эфир"),
    MOVIES("▶", "Фильмы"),
    SERIES("▦", "Сериалы"),
    SEARCH("⌕", "Поиск"),
    SETTINGS("⚙", "Настройки"),
}
