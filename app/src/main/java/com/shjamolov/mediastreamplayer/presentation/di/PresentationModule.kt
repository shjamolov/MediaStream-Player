package com.shjamolov.mediastreamplayer.presentation.di

import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import com.shjamolov.mediastreamplayer.presentation.catalog.CatalogViewModel
import com.shjamolov.mediastreamplayer.presentation.security.ParentalControlViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TvCatalogViewModel(get(), get(), get()) }
    viewModel { CatalogViewModel(get()) }
    viewModel { ParentalControlViewModel(get(), get()) }
}
