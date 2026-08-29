package com.shjamolov.mediastreamplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shjamolov.mediastreamplayer.presentation.player.TvPlayerScreen
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogScreen
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val tvCatalogViewModel: TvCatalogViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val selectedChannel by tvCatalogViewModel.selectedChannel.collectAsStateWithLifecycle()
            val guideState by tvCatalogViewModel.guideState.collectAsStateWithLifecycle()
            val channel = selectedChannel
            if (channel == null) {
                TvCatalogScreen(
                    viewModel = tvCatalogViewModel,
                    onChannelSelected = tvCatalogViewModel::openChannel,
                )
            } else {
                TvPlayerScreen(
                    channel = channel,
                    guideState = guideState,
                    onBack = tvCatalogViewModel::closePlayer,
                )
            }
        }
    }
}
