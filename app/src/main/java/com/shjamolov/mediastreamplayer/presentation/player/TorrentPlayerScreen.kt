package com.shjamolov.mediastreamplayer.presentation.player

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.domain.model.TorrentPlaybackSource

@OptIn(UnstableApi::class)
@Composable
fun TorrentPlayerScreen(source: TorrentPlaybackSource, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(source.url) { createTorrentPlayer(context, source) }
    BackHandler(onBack = onBack)
    DisposableEffect(player) { onDispose(player::release) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = true; keepScreenOn = true } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            source.title,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
                .background(Color(0x99000000)).padding(horizontal = 40.dp, vertical = 18.dp),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@OptIn(UnstableApi::class)
private fun createTorrentPlayer(context: Context, source: TorrentPlaybackSource): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(source.requestHeaders)
        .setAllowCrossProtocolRedirects(true)
    val mediaSource = DefaultMediaSourceFactory(DefaultDataSource.Factory(context.applicationContext, httpFactory))
        .createMediaSource(MediaItem.fromUri(source.url))
    return ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
        volume = 1f
        setMediaSource(mediaSource)
        prepare()
        playWhenReady = true
    }
}
