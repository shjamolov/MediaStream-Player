package com.shjamolov.mediastreamplayer.presentation.torrserver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode
import com.shjamolov.mediastreamplayer.core.torrserver.LocalTorrServerState
import com.shjamolov.mediastreamplayer.presentation.components.AdaptiveButton

@Composable
fun TorrServerSettingsScreen(viewModel: TorrServerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val localState by viewModel.localServerState.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxWidth < 600.dp
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (compact) 20.dp else 64.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("TorrServer", style = MaterialTheme.typography.headlineMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 16.dp)) {
            items(listOf(TorrServerMode.LOCAL_MANAGED, TorrServerMode.LOCAL_EXTERNAL, TorrServerMode.REMOTE)) { mode ->
                AdaptiveButton(onClick = { viewModel.setMode(mode) }, selected = state.mode == mode) {
                    val label = stringResource(when (mode) {
                        TorrServerMode.LOCAL_MANAGED -> R.string.torrserver_managed
                        TorrServerMode.LOCAL_EXTERNAL -> R.string.torrserver_local
                        TorrServerMode.REMOTE -> R.string.torrserver_remote
                    })
                    Text(if (state.mode == mode) "✓ $label" else label)
                }
            }
        }
        if (state.mode == TorrServerMode.LOCAL_MANAGED) {
            val (serverText, serverOk) = when (val status = localState) {
                LocalTorrServerState.Stopped -> stringResource(R.string.local_server_stopped) to false
                is LocalTorrServerState.Downloading -> stringResource(R.string.local_server_downloading, status.percent) to true
                LocalTorrServerState.Starting -> stringResource(R.string.local_server_starting) to true
                LocalTorrServerState.PreparingSearch -> stringResource(R.string.local_search_preparing) to true
                is LocalTorrServerState.Running -> stringResource(R.string.local_server_running, status.version) to true
                is LocalTorrServerState.Failed -> stringResource(R.string.local_server_failed, status.message) to false
            }
            Text(serverText, color = if (serverOk) Color(0xFF76E39A) else Color(0xFFFFA5A5))
        } else {
            SettingField(state.url, viewModel::setUrl, stringResource(R.string.torrserver_url), compact = compact)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdaptiveButton(onClick = viewModel::save, enabled = !state.testing) {
                Text(stringResource(R.string.save))
            }
            AdaptiveButton(onClick = viewModel::testAndSave, enabled = !state.testing) {
                Text(stringResource(if (state.testing) R.string.torrserver_testing else R.string.torrserver_test_save))
            }
        }
        if (state.testing) {
            Text(stringResource(R.string.torrserver_checking), color = Color(0xFFFFC857))
        }
        state.result?.let { result ->
            val (text, ok) = when (result) {
                ConnectionResult.Saved -> stringResource(R.string.torrserver_saved) to true
                is ConnectionResult.Connected -> stringResource(R.string.torrserver_connected, result.version) to true
                ConnectionResult.Failed -> stringResource(R.string.torrserver_failed) to false
                ConnectionResult.InvalidUrl -> stringResource(R.string.torrserver_invalid_url) to false
                ConnectionResult.InvalidCredentials -> stringResource(R.string.torrserver_invalid_credentials) to false
            }
            Text(text, color = if (ok) Color(0xFF76E39A) else Color(0xFFFFA5A5))
        }
        if (state.mode != TorrServerMode.LOCAL_MANAGED) {
            Text(stringResource(R.string.torrserver_emulator_hint), color = Color(0xFF9CB3C5))
            SettingField(state.username, viewModel::setUsername, stringResource(R.string.torrserver_username), compact = compact)
            SettingField(state.password, viewModel::setPassword, stringResource(R.string.torrserver_password), password = true, compact = compact)
            Text(stringResource(R.string.torrserver_password_security), color = Color(0xFF9CB3C5))
        }
    }
    }
}

@Composable
private fun SettingField(value: String, onChange: (String) -> Unit, hint: String, password: Boolean = false, compact: Boolean = false) {
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(color = Color.White, fontSize = 19.sp),
        modifier = Modifier.fillMaxWidth(if (compact) 1f else 0.72f).background(Color(0xFF17384B), RoundedCornerShape(9.dp)).padding(14.dp),
        decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = Color.Gray); inner() },
    )
}
