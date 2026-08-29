package com.shjamolov.mediastreamplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.domain.model.MediaType
import com.shjamolov.mediastreamplayer.presentation.catalog.CatalogScreen
import com.shjamolov.mediastreamplayer.presentation.catalog.CatalogViewModel
import com.shjamolov.mediastreamplayer.presentation.player.TvPlayerScreen
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogScreen
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val tvCatalogViewModel: TvCatalogViewModel by viewModel()
    private val catalogViewModel: CatalogViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
            val selectedChannel by tvCatalogViewModel.selectedChannel.collectAsStateWithLifecycle()
            val guideState by tvCatalogViewModel.guideState.collectAsStateWithLifecycle()
            val channel = selectedChannel
            var section by remember { mutableStateOf(AppSection.TV) }
            if (channel != null) {
                TvPlayerScreen(channel, guideState, tvCatalogViewModel::closePlayer)
            } else {
                Surface(Modifier.fillMaxSize()) {
                    Column {
                        Row(Modifier.padding(horizontal = 40.dp, vertical = 12.dp)) {
                            AppSection.entries.forEach { item ->
                                Button(
                                    onClick = {
                                        section = item
                                        when (item) {
                                            AppSection.MOVIES -> catalogViewModel.load(MediaType.MOVIE)
                                            AppSection.SERIES -> catalogViewModel.load(MediaType.SERIES)
                                            else -> Unit
                                        }
                                    },
                                    modifier = Modifier.padding(end = 10.dp),
                                ) { Text(if (section == item) "✓ ${item.label}" else item.label) }
                            }
                        }
                        when (section) {
                            AppSection.TV -> TvCatalogScreen(tvCatalogViewModel, tvCatalogViewModel::openChannel)
                            AppSection.MOVIES, AppSection.SERIES -> CatalogScreen(catalogViewModel)
                            AppSection.SEARCH -> CatalogScreen(catalogViewModel, searchMode = true)
                        }
                    }
                }
            }
            }
        }
    }
}

private enum class AppSection(val label: String) {
    TV("TV"), MOVIES("Фильмы"), SERIES("Сериалы"), SEARCH("Поиск")
}
