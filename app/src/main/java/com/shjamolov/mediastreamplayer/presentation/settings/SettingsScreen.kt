package com.shjamolov.mediastreamplayer.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.core.settings.CatalogLanguage
import com.shjamolov.mediastreamplayer.presentation.security.ParentalControlScreen
import com.shjamolov.mediastreamplayer.presentation.security.ParentalControlViewModel
import com.shjamolov.mediastreamplayer.presentation.torrserver.TorrServerSettingsScreen
import com.shjamolov.mediastreamplayer.presentation.torrserver.TorrServerViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    parentalViewModel: ParentalControlViewModel,
    torrServerViewModel: TorrServerViewModel,
) {
    var page by remember { mutableStateOf(SettingsPage.GENERAL) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 48.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsPage.entries.forEach { item ->
                Button(onClick = { page = item }) {
                    Text(if (page == item) "✓ ${stringResource(item.title)}" else stringResource(item.title))
                }
            }
        }
        when (page) {
            SettingsPage.GENERAL -> GeneralSettings(viewModel)
            SettingsPage.PARENTAL -> ParentalControlScreen(parentalViewModel)
            SettingsPage.TORRSERVER -> TorrServerSettingsScreen(torrServerViewModel)
            SettingsPage.DIAGNOSTICS -> Diagnostics(viewModel)
            SettingsPage.ABOUT -> About()
        }
    }
}

@Composable
private fun GeneralSettings(viewModel: SettingsViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(64.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.catalog_language), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.catalog_language_note), color = Color(0xFF9CB3C5))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CatalogLanguage.entries.forEach { item ->
                Button(onClick = { viewModel.setLanguage(item) }) {
                    Text(if (language == item) "✓ ${item.label()}" else item.label())
                }
            }
        }
    }
}

@Composable
private fun Diagnostics(viewModel: SettingsViewModel) {
    val state by viewModel.diagnostics.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(64.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.diagnostics), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Button(onClick = viewModel::runDiagnostics, enabled = state != DiagnosticsState.Running) {
            Text(stringResource(if (state == DiagnosticsState.Running) R.string.diagnostics_running else R.string.run_diagnostics))
        }
        if (state is DiagnosticsState.Complete) {
            val result = (state as DiagnosticsState.Complete).result
            StatusLine(stringResource(R.string.tmdb_token), result.tmdbTokenConfigured)
            StatusLine("TMDB API", result.tmdbReachable)
            StatusLine("iptv-org API", result.iptvOrgReachable)
        }
    }
}

@Composable private fun StatusLine(name: String, ok: Boolean) {
    Text("${if (ok) "✓" else "✕"} $name", color = if (ok) Color(0xFF76E39A) else Color(0xFFFFA5A5))
}

@Composable
private fun About() {
    Column(
        Modifier.fillMaxSize().padding(64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MediaStream Player", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Version 0.1.0", Modifier.padding(top = 10.dp))
        Spacer(Modifier.padding(10.dp))
        Text(stringResource(R.string.tmdb_attribution), color = Color(0xFF9CB3C5))
        Text(stringResource(R.string.iptv_attribution), color = Color(0xFF9CB3C5), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable private fun CatalogLanguage.label() = when (this) {
    CatalogLanguage.RUSSIAN -> stringResource(R.string.language_russian)
    CatalogLanguage.ENGLISH -> stringResource(R.string.language_english)
    CatalogLanguage.UZBEK -> stringResource(R.string.language_uzbek)
}

private enum class SettingsPage(val title: Int) {
    GENERAL(R.string.settings_general),
    PARENTAL(R.string.parental_control),
    TORRSERVER(R.string.torrserver),
    DIAGNOSTICS(R.string.diagnostics),
    ABOUT(R.string.about),
}
