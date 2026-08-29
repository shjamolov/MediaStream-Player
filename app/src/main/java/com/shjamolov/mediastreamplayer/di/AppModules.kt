package com.shjamolov.mediastreamplayer.di

import com.shjamolov.mediastreamplayer.core.di.coreModule
import com.shjamolov.mediastreamplayer.data.di.dataModule
import com.shjamolov.mediastreamplayer.data.di.iptvOrgModule
import com.shjamolov.mediastreamplayer.presentation.di.presentationModule

val appModules = listOf(
    coreModule,
    dataModule,
    iptvOrgModule,
    presentationModule,
)
