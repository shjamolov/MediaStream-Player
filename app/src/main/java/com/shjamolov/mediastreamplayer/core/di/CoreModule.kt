package com.shjamolov.mediastreamplayer.core.di

import com.shjamolov.mediastreamplayer.core.coroutines.DefaultDispatcherProvider
import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.core.security.ParentalControlStore
import com.shjamolov.mediastreamplayer.core.security.PinHasher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { PinHasher() }
    single { ParentalControlStore(androidContext()) }
}
