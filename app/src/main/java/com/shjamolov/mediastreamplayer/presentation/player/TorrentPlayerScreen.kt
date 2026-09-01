package com.shjamolov.mediastreamplayer.presentation.player

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.domain.model.TorrentPlaybackSource
import com.shjamolov.mediastreamplayer.presentation.components.AdaptiveButton
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun TorrentPlayerScreen(
    source: TorrentPlaybackSource,
    onChooseRelease: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(source.url) { VlcTorrentController(context, source) }
    var audioTracks by remember(source.url) { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var selectedTrack by remember(source.url) { mutableStateOf<Int?>(null) }
    var message by remember(source.url) { mutableStateOf("Анализируем аудиодорожки…") }

    BackHandler(onBack = onBack)
    DisposableEffect(controller) {
        controller.onTracksChanged = { tracks, selected ->
            audioTracks = tracks
            selectedTrack = selected
            message = audioStatusMessage(tracks)
        }
        onDispose(controller::release)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { controller.createView(it) },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.align(Alignment.TopStart).fillMaxWidth().background(Color(0xB0000000)).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(source.title, style = MaterialTheme.typography.titleLarge)
            Text(message, color = if (message.contains("не поддерживает", true)) Color(0xFFFF9B9B) else Color(0xFFB7C9D6))
            if (audioTracks.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(audioTracks, key = { it.id }) { track ->
                        AdaptiveButton(
                            onClick = { controller.selectAudioTrack(track.id) },
                            selected = selectedTrack == track.id,
                        ) { Text(track.label) }
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).background(Color(0xB0000000)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdaptiveButton(onClick = onChooseRelease) { Text("Другой релиз") }
            AdaptiveButton(onClick = onBack) { Text("Закрыть") }
        }
    }
}

private class VlcTorrentController(context: Context, private val source: TorrentPlaybackSource) {
    private val libVlc = LibVLC(context.applicationContext, arrayListOf(
        "--network-caching=3000",
        "--audio-time-stretch",
        "--avcodec-hw=any",
    ))
    private val player = MediaPlayer(libVlc)
    private var automaticAudioSelected = false
    var onTracksChanged: (List<AudioTrackOption>, Int?) -> Unit = { _, _ -> }

    init {
        player.setEventListener { event ->
            if (event.type == MediaPlayer.Event.Playing || event.type == MediaPlayer.Event.ESAdded || event.type == MediaPlayer.Event.ESSelected) {
                publishAudioTracks()
            }
        }
    }

    fun createView(context: Context): VLCVideoLayout = VLCVideoLayout(context).also { layout ->
        player.attachViews(layout, null, false, false)
        val media = Media(libVlc, Uri.parse(source.url)).apply {
            source.requestHeaders["User-Agent"]?.let { addOption(":http-user-agent=$it") }
            source.requestHeaders["Referer"]?.let { addOption(":http-referrer=$it") }
            addOption(":network-caching=3000")
        }
        player.media = media
        media.release()
        player.play()
    }

    fun selectAudioTrack(id: Int) {
        player.setAudioTrack(id)
        publishAudioTracks()
    }

    private fun publishAudioTracks() {
        val tracks = player.audioTracks?.map { AudioTrackOption(it.id, it.name.orEmpty()) }.orEmpty()
        if (tracks.isNotEmpty()) {
            val preferred = tracks.firstOrNull { it.label.contains("AAC", true) }
                ?: tracks.firstOrNull { it.label.contains("рус", true) || it.label.contains("rus", true) }
            if (!automaticAudioSelected && preferred != null) {
                player.setAudioTrack(preferred.id)
                automaticAudioSelected = true
            }
            onTracksChanged(tracks, player.audioTrack.takeIf { it >= 0 })
        }
    }

    fun release() {
        player.stop()
        player.detachViews()
        player.release()
        libVlc.release()
    }
}

private data class AudioTrackOption(val id: Int, val label: String)

private fun audioStatusMessage(tracks: List<AudioTrackOption>): String {
    if (tracks.isEmpty()) return "Аудиодорожка пока не обнаружена"
    val names = tracks.joinToString { it.label }
    return when {
        names.contains("DTS", true) -> "Устройство может не поддерживать DTS — включён встроенный программный декодер"
        names.contains("AC3", true) || names.contains("A/52", true) -> "AC3 обнаружен — включён встроенный программный декодер"
        else -> "Доступно аудиодорожек: ${tracks.size}"
    }
}
