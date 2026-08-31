package com.shjamolov.mediastreamplayer.presentation.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.TvChannelStreams
import com.shjamolov.mediastreamplayer.presentation.tv.TvGuideUiState
import java.util.Date
import com.shjamolov.mediastreamplayer.presentation.components.AdaptiveButton

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    channel: TvChannelStreams,
    guideState: TvGuideUiState,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(channel.channel.id) {
        TvPlayerController(context, channel)
    }
    val state by controller.state.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)
    DisposableEffect(controller) {
        onDispose(controller::release)
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { playerContext ->
                    PlayerView(playerContext).apply {
                        player = controller.player
                        useController = true
                        keepScreenOn = true
                    }
                },
                update = { it.player = controller.player },
                modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { onPreviousChannel(); true }
                        Key.DirectionDown -> { onNextChannel(); true }
                        else -> false
                    }
                },
            )

            PlayerHeader(
                channelName = channel.channel.name,
                state = state,
                guideState = guideState,
                modifier = Modifier.align(Alignment.TopStart),
            )

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).background(Color(0x99000000)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AdaptiveButton(onClick = onPreviousChannel) { Text("↑ Предыдущий") }
                AdaptiveButton(onClick = onNextChannel) { Text("↓ Следующий") }
            }

            val failedState = state as? PlaybackUiState.Failed
            if (failedState != null) {
                PlayerError(
                    attemptedStreams = failedState.attemptedStreams,
                    onRetry = controller::retryFromBestStream,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    channelName: String,
    state: PlaybackUiState,
    guideState: TvGuideUiState,
    modifier: Modifier = Modifier,
) {
    val status = when (state) {
        PlaybackUiState.Preparing -> stringResource(R.string.player_preparing)
        is PlaybackUiState.Buffering -> if (state.isFallback) {
            stringResource(
                R.string.player_trying_fallback,
                state.streamNumber,
                state.streamCount,
            )
        } else {
            stringResource(R.string.player_buffering)
        }
        is PlaybackUiState.Playing -> listOfNotNull(
            state.quality,
            stringResource(R.string.player_stream_position, state.streamNumber, state.streamCount),
            if (state.hasAudio) stringResource(R.string.player_audio_active) else stringResource(R.string.player_audio_missing),
        ).joinToString(" • ")
        PlaybackUiState.Ended -> stringResource(R.string.player_ended)
        is PlaybackUiState.Failed -> stringResource(R.string.player_unavailable)
    }
    val context = LocalContext.current
    val timeFormat = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xB3000000))
            .padding(horizontal = 40.dp, vertical = 22.dp),
    ) {
        Text(text = channelName, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = status, color = Color(0xFFB7C9D6))
        when (guideState) {
            TvGuideUiState.Idle -> Unit
            TvGuideUiState.Loading -> Text(
                text = stringResource(R.string.epg_loading),
                color = Color(0xFFB7C9D6),
            )
            TvGuideUiState.Unavailable -> Text(
                text = stringResource(R.string.epg_unavailable),
                color = Color(0xFFB7C9D6),
            )
            is TvGuideUiState.Content -> {
                guideState.current?.let { current ->
                    Text(
                        text = stringResource(R.string.epg_now, current.title),
                        color = Color.White,
                    )
                }
                guideState.next?.let { next ->
                    Text(
                        text = stringResource(
                            R.string.epg_next,
                            timeFormat.format(Date(next.startsAtEpochMillis)),
                            next.title,
                        ),
                        color = Color(0xFFB7C9D6),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerError(
    attemptedStreams: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xE6142028))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.player_all_streams_failed,
                attemptedStreams,
                attemptedStreams,
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdaptiveButton(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
            AdaptiveButton(onClick = onBack) {
                Text(text = stringResource(R.string.back_to_catalog))
            }
        }
    }
}
