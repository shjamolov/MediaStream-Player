package com.shjamolov.mediastreamplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogScreen
import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val tvCatalogViewModel: TvCatalogViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvCatalogScreen(viewModel = tvCatalogViewModel)
        }
    }
}
