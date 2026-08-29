package com.shjamolov.mediastreamplayer.domain.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.ChannelId
import com.shjamolov.mediastreamplayer.domain.model.TvGuideEntry

interface TvGuideRepository {
    suspend fun getSchedule(
        channelId: ChannelId,
        feedId: String?,
    ): AppResult<List<TvGuideEntry>>
}
