package com.shjamolov.mediastreamplayer.presentation.torrserver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.R
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode

@Composable
fun TorrServerSettingsScreen(viewModel: TorrServerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 64.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("TorrServer", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(TorrServerMode.LOCAL_EXTERNAL, TorrServerMode.REMOTE).forEach { mode ->
                Button(onClick = { viewModel.setMode(mode) }) {
                    val label = stringResource(if (mode == TorrServerMode.LOCAL_EXTERNAL) R.string.torrserver_local else R.string.torrserver_remote)
                    Text(if (state.mode == mode) "✓ $label" else label)
                }
            }
        }
        SettingField(state.url, viewModel::setUrl, stringResource(R.string.torrserver_url))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::save, enabled = !state.testing) {
                Text(stringResource(R.string.save))
            }
            Button(onClick = viewModel::testAndSave, enabled = !state.testing) {
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
        Text(stringResource(R.string.torrserver_emulator_hint), color = Color(0xFF9CB3C5))
        SettingField(state.username, viewModel::setUsername, stringResource(R.string.torrserver_username))
        SettingField(state.password, viewModel::setPassword, stringResource(R.string.torrserver_password), password = true)
        Text(stringResource(R.string.torrserver_password_security), color = Color(0xFF9CB3C5))
    }
}

@Composable
private fun SettingField(value: String, onChange: (String) -> Unit, hint: String, password: Boolean = false) {
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(color = Color.White, fontSize = 19.sp),
        modifier = Modifier.fillMaxWidth(0.72f).background(Color(0xFF17384B), RoundedCornerShape(9.dp)).padding(14.dp),
        decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = Color.Gray); inner() },
    )
}
