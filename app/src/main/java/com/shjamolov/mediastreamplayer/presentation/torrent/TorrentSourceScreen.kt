package com.shjamolov.mediastreamplayer.presentation.torrent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.TorrentVideoFile

@Composable
fun TorrentSourceScreen(viewModel: TorrentPlaybackViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = state.content
    BackHandler(onBack = viewModel::close)
    BoxWithConstraints(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF08151D)).padding(horizontal = if (maxWidth < 600.dp) 20.dp else 64.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.torrent_source_title), style = MaterialTheme.typography.headlineLarge)
        Text(state.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFC857))
        if (content == null) {
            Text(stringResource(R.string.torznab_results), style = MaterialTheme.typography.titleMedium)
            BasicTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth().background(Color(0xFF17384B), RoundedCornerShape(10.dp)).padding(14.dp),
            )
            Button(onClick = viewModel::search, enabled = !state.searching && !state.loading) {
                Text(stringResource(if (state.searching) R.string.torznab_searching else R.string.torznab_search))
            }
            when {
                state.searching -> Text(stringResource(R.string.torznab_searching), color = Color(0xFFFFC857))
                state.searchError -> Text(stringResource(R.string.torznab_not_configured), color = Color(0xFFFFA5A5))
                state.searchResults.isEmpty() -> Text(stringResource(R.string.torznab_empty), color = Color(0xFF9CB3C5))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.searchResults, key = { "${it.source}-${it.magnetOrLink}" }) { result ->
                        Button(
                            onClick = { viewModel.selectResult(result) },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val quality = result.quality?.let { "${it}p • " }.orEmpty()
                            Text("${result.title}\n$quality${result.size} • ↑ ${result.seeders} • ${result.source}")
                        }
                    }
                }
            }
            Button(onClick = viewModel::close, enabled = !state.loading) { Text(stringResource(R.string.cancel)) }
            if (state.loading) Text(stringResource(R.string.torrent_metadata_wait), color = Color(0xFFFFC857))
            state.error?.let {
                Text(
                    stringResource(if (it == TorrentSourceError.INVALID_LINK) R.string.torrent_invalid_link else R.string.torrent_load_failed),
                    color = Color(0xFFFFA5A5),
                )
            }
        } else {
            Text(stringResource(R.string.torrent_choose_file), style = MaterialTheme.typography.titleLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(content.files, key = TorrentVideoFile::id) { file ->
                    Button(onClick = { viewModel.play(file) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${file.path.substringAfterLast('/').substringAfterLast('\\')} • ${formatSize(file.sizeBytes)}")
                    }
                }
            }
        }
    }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
    else -> "${bytes / 1024} KB"
}
