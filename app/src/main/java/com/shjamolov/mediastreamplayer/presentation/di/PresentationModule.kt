package com.shjamolov.mediastreamplayer.presentation.di

import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import com.shjamolov.mediastreamplayer.presentation.catalog.CatalogViewModel
import com.shjamolov.mediastreamplayer.presentation.security.ParentalControlViewModel
import com.shjamolov.mediastreamplayer.presentation.settings.SettingsViewModel
import com.shjamolov.mediastreamplayer.presentation.torrserver.TorrServerViewModel
import com.shjamolov.mediastreamplayer.presentation.torrent.TorrentPlaybackViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TvCatalogViewModel(get(), get(), get()) }
    viewModel { CatalogViewModel(get()) }
    viewModel { ParentalControlViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { TorrServerViewModel(get(), get(), get()) }
    viewModel { TorrentPlaybackViewModel(get(), get(), get()) }
}
