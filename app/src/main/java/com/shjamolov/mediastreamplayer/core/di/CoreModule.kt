package com.shjamolov.mediastreamplayer.core.di

import com.shjamolov.mediastreamplayer.core.coroutines.DefaultDispatcherProvider
import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import org.koin.dsl.module

val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

