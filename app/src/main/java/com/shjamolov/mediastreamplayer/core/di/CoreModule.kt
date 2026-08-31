package com.shjamolov.mediastreamplayer.core.di

import com.shjamolov.mediastreamplayer.core.coroutines.DefaultDispatcherProvider
import com.shjamolov.mediastreamplayer.core.coroutines.DispatcherProvider
import com.shjamolov.mediastreamplayer.core.security.ParentalControlStore
import com.shjamolov.mediastreamplayer.core.security.PinHasher
import com.shjamolov.mediastreamplayer.core.settings.AppSettingsStore
import com.shjamolov.mediastreamplayer.core.settings.TorrServerSettingsStore
import com.shjamolov.mediastreamplayer.core.torrserver.LocalTorrServerManager
import com.shjamolov.mediastreamplayer.core.security.AndroidCredentialCipher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { PinHasher() }
    single { ParentalControlStore(androidContext()) }
    single { AppSettingsStore(androidContext()) }
    single { AndroidCredentialCipher() }
    single { TorrServerSettingsStore(androidContext(), get()) }
    single { LocalTorrServerManager(androidContext(), get()) }
}
