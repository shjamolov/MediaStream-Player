package com.shjamolov.mediastreamplayer.di

import com.shjamolov.mediastreamplayer.core.di.coreModule
import com.shjamolov.mediastreamplayer.data.di.dataModule

val appModules = listOf(
    coreModule,
    dataModule,
)

