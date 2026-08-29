package com.shjamolov.mediastreamplayer.presentation.di

import com.shjamolov.mediastreamplayer.presentation.tv.TvCatalogViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TvCatalogViewModel(get(), get()) }
}
