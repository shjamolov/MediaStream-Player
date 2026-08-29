package com.shjamolov.mediastreamplayer.presentation.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.domain.model.TvStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class TvPlayerController(
    context: Context,
    val channel: TvChannelStreams,
) : Player.Listener {
    private val appContext = context.applicationContext
    private val fallbackQueue = StreamFallbackQueue(channel.streams)
    private val mutableState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Preparing)

    val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()
    val player: ExoPlayer = ExoPlayer.Builder(appContext).build().also {
        it.addListener(this)
    }

    init {
        play(fallbackQueue.current, isFallback = false)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        mutableState.value = when (playbackState) {
            Player.STATE_BUFFERING -> currentStatus(isFallback = fallbackQueue.currentIndex > 0)
            Player.STATE_READY -> PlaybackUiState.Playing(
                streamNumber = fallbackQueue.currentIndex + 1,
                streamCount = fallbackQueue.size,
                quality = fallbackQueue.current.quality,
            )
            Player.STATE_ENDED -> PlaybackUiState.Ended
            else -> mutableState.value
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val fallback = fallbackQueue.advance()
        if (fallback == null) {
            mutableState.value = PlaybackUiState.Failed(
                attemptedStreams = fallbackQueue.size,
                cause = error,
            )
        } else {
            play(fallback, isFallback = true)
        }
    }

    fun retryFromBestStream() {
        play(fallbackQueue.reset(), isFallback = false)
    }

    fun release() {
        player.removeListener(this)
        player.release()
    }

    private fun play(stream: TvStream, isFallback: Boolean) {
        mutableState.value = currentStatus(isFallback)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(stream.requestHeaders)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)
        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(stream.toMediaItem())

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    private fun currentStatus(isFallback: Boolean) = PlaybackUiState.Buffering(
        streamNumber = fallbackQueue.currentIndex + 1,
        streamCount = fallbackQueue.size,
        quality = fallbackQueue.current.quality,
        isFallback = isFallback,
    )
}

sealed interface PlaybackUiState {
    data object Preparing : PlaybackUiState

    data class Buffering(
        val streamNumber: Int,
        val streamCount: Int,
        val quality: String?,
        val isFallback: Boolean,
    ) : PlaybackUiState

    data class Playing(
        val streamNumber: Int,
        val streamCount: Int,
        val quality: String?,
    ) : PlaybackUiState

    data object Ended : PlaybackUiState

    data class Failed(
        val attemptedStreams: Int,
        val cause: Throwable,
    ) : PlaybackUiState
}

internal fun TvStream.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(url)
        .setMimeType(inferMimeType(url))
        .build()
}

internal fun inferMimeType(url: String): String? = when {
    url.substringBefore('?').endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
    url.substringBefore('?').endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
    else -> null
}
